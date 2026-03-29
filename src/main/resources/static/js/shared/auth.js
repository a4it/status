const auth = {
    TOKEN_KEY: 'status_admin_token',
    REFRESH_TOKEN_KEY: 'status_admin_refresh_token',
    USER_KEY: 'status_admin_user',

    getToken() {
        return localStorage.getItem(this.TOKEN_KEY) || sessionStorage.getItem(this.TOKEN_KEY);
    },

    getRefreshToken() {
        return localStorage.getItem(this.REFRESH_TOKEN_KEY) || sessionStorage.getItem(this.REFRESH_TOKEN_KEY);
    },

    getUser() {
        const userJson = localStorage.getItem(this.USER_KEY) || sessionStorage.getItem(this.USER_KEY);
        return userJson ? JSON.parse(userJson) : null;
    },

    setToken(token, refreshToken, user, remember = false) {
        const storage = remember ? localStorage : sessionStorage;
        storage.setItem(this.TOKEN_KEY, token);
        if (refreshToken) {
            storage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
        }
        if (user) {
            storage.setItem(this.USER_KEY, JSON.stringify(user));
        }
    },

    clearToken() {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.REFRESH_TOKEN_KEY);
        localStorage.removeItem(this.USER_KEY);
        sessionStorage.removeItem(this.TOKEN_KEY);
        sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
        sessionStorage.removeItem(this.USER_KEY);
    },

    isAuthenticated() {
        const token = this.getToken();
        if (!token) return false;

        try {
            const payload = this.parseJwt(token);
            const now = Math.floor(Date.now() / 1000);
            return payload.exp > now;
        } catch (e) {
            return false;
        }
    },

    parseJwt(token) {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    },

    async login(username, password, remember = false) {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Login failed' }));
            throw new Error(error.message || 'Invalid credentials');
        }

        const data = await response.json();

        const user = {
            id: data.userId,
            username: data.username,
            email: data.email,
            role: data.role,
            organizationId: data.organizationId,
            requiresContextSelection: data.requiresContextSelection
        };

        this.setToken(data.accessToken, data.refreshToken, user, remember);
        return data;
    },

    async refreshAccessToken() {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) {
            throw new Error('No refresh token available');
        }

        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ refreshToken })
        });

        if (!response.ok) {
            this.clearToken();
            throw new Error('Session expired');
        }

        const data = await response.json();
        const remember = localStorage.getItem(this.TOKEN_KEY) !== null;
        this.setToken(data.accessToken, data.refreshToken, null, remember);
        return data.accessToken;
    },

    logout() {
        const token = this.getToken();
        if (token) {
            fetch('/api/auth/logout', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }).catch(() => {});
        }
        this.clearToken();
        window.location.href = '/login';
    },

    requireAuth() {
        if (!this.isAuthenticated()) {
            window.location.href = '/login';
            return false;
        }
        // Redirect SUPERADMIN to context selection if they haven't selected one yet
        const token = this.getToken();
        if (token) {
            try {
                const payload = this.parseJwt(token);
                if (payload.requiresContextSelection === true && window.location.pathname !== '/admin/select-context') {
                    window.location.href = '/admin/select-context';
                    return false;
                }
            } catch (e) {}
        }
        return true;
    },

    getUserDisplayName() {
        const user = this.getUser();
        return user ? (user.fullName || user.username) : 'User';
    },

    getUserRole() {
        const user = this.getUser();
        return user ? user.role : null;
    },

    isSuperadmin() {
        return this.getUserRole() === 'SUPERADMIN';
    },

    initNavbar() {
        const user = this.getUser();
        if (!user) return;

        const displayName = user.fullName || user.username || 'Admin';

        // Update avatar initial and display name (works with IDs or by querying the structure)
        const avatarEl = document.getElementById('userAvatar') || document.querySelector('.navbar .avatar.avatar-sm');
        const nameEl = document.getElementById('userDisplayName') || document.querySelector('.navbar .d-none.d-xl-block > div');
        if (avatarEl) avatarEl.textContent = displayName.charAt(0).toUpperCase();
        if (nameEl) nameEl.textContent = displayName;

        const dropdown = document.querySelector('.dropdown-menu.dropdown-menu-end');

        if (this.isSuperadmin()) {
            // Inject "Switch Context" into the dropdown if not already there
            if (dropdown && !dropdown.querySelector('.switch-context-item')) {
                const switchItem = document.createElement('a');
                switchItem.href = '/admin/select-context';
                switchItem.className = 'dropdown-item switch-context-item';
                switchItem.innerHTML = '<i class="ti ti-switch-horizontal me-2"></i>Switch Context';
                dropdown.insertBefore(switchItem, dropdown.firstChild);

                // Add a divider
                const divider = document.createElement('div');
                divider.className = 'dropdown-divider';
                dropdown.insertBefore(divider, dropdown.firstChild.nextSibling);
            }
        }

        // Inject "Change Password" before Logout if not already there
        if (dropdown && !dropdown.querySelector('.change-password-item')) {
            const logoutItem = dropdown.querySelector('[onclick*="logout"]');
            const divider = document.createElement('div');
            divider.className = 'dropdown-divider';
            const changePasswordItem = document.createElement('a');
            changePasswordItem.href = '#';
            changePasswordItem.className = 'dropdown-item change-password-item';
            changePasswordItem.innerHTML = '<i class="ti ti-lock me-2"></i>Change Password';
            changePasswordItem.addEventListener('click', (e) => {
                e.preventDefault();
                auth.openChangePasswordModal();
            });
            if (logoutItem) {
                dropdown.insertBefore(divider, logoutItem);
                dropdown.insertBefore(changePasswordItem, divider);
            } else {
                dropdown.appendChild(divider);
                dropdown.appendChild(changePasswordItem);
            }
        }

        // Inject change password modal if not already present
        if (!document.getElementById('changePasswordModal')) {
            const modal = document.createElement('div');
            modal.innerHTML = `
                <div class="modal modal-blur fade" id="changePasswordModal" tabindex="-1" role="dialog" aria-hidden="true">
                    <div class="modal-dialog modal-sm modal-dialog-centered" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Change Password</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                <div id="changePasswordError" class="alert alert-danger d-none"></div>
                                <div class="mb-3">
                                    <label class="form-label required">Current Password</label>
                                    <input type="password" id="cpCurrentPassword" class="form-control" placeholder="Current password">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label required">New Password</label>
                                    <input type="password" id="cpNewPassword" class="form-control" placeholder="Min. 8 characters">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label required">Confirm New Password</label>
                                    <input type="password" id="cpConfirmPassword" class="form-control" placeholder="Repeat new password">
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-link link-secondary me-auto" data-bs-dismiss="modal">Cancel</button>
                                <button type="button" class="btn btn-primary" id="cpSubmitBtn" onclick="auth.submitChangePassword()">Change Password</button>
                            </div>
                        </div>
                    </div>
                </div>`;
            document.body.appendChild(modal.firstElementChild);
        }

        // Hide superadmin-only elements for non-superadmin users
        if (!this.isSuperadmin()) {
            document.querySelectorAll('.superadmin-only').forEach(el => {
                el.style.display = 'none';
            });
        }

        // Show organization context indicator for ALL users
        const navbarRight = document.querySelector('.navbar-nav.flex-row.order-md-last');
        if (navbarRight && !navbarRight.querySelector('.context-indicator')) {
            const API_obj = typeof API !== 'undefined' ? API : null;
            if (API_obj) {
                API_obj.get('/context/current').then(ctx => {
                    if (ctx && ctx.organizationName) {
                        const label = ctx.tenantName
                            ? `${ctx.tenantName} / ${ctx.organizationName}`
                            : ctx.organizationName;
                        const indicator = document.createElement('div');
                        indicator.className = 'nav-item me-3 context-indicator';
                        if (this.isSuperadmin()) {
                            indicator.innerHTML = `<a href="/admin/select-context" class="btn btn-sm btn-outline-primary"><i class="ti ti-building me-1"></i>${label}</a>`;
                        } else {
                            indicator.innerHTML = `<span class="btn btn-sm btn-outline-secondary pe-none"><i class="ti ti-building me-1"></i>${label}</span>`;
                        }
                        navbarRight.insertBefore(indicator, navbarRight.firstChild);
                    }
                }).catch(() => {});
            }
        }
    },

    openChangePasswordModal() {
        document.getElementById('cpCurrentPassword').value = '';
        document.getElementById('cpNewPassword').value = '';
        document.getElementById('cpConfirmPassword').value = '';
        document.getElementById('changePasswordError').classList.add('d-none');
        const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('changePasswordModal'));
        modal.show();
    },

    async submitChangePassword() {
        const currentPassword = document.getElementById('cpCurrentPassword').value.trim();
        const newPassword = document.getElementById('cpNewPassword').value.trim();
        const confirmPassword = document.getElementById('cpConfirmPassword').value.trim();
        const errorEl = document.getElementById('changePasswordError');
        const submitBtn = document.getElementById('cpSubmitBtn');

        const showError = (msg) => {
            errorEl.textContent = msg;
            errorEl.classList.remove('d-none');
        };

        if (!currentPassword || !newPassword || !confirmPassword) {
            showError('All fields are required.');
            return;
        }
        if (newPassword.length < 8) {
            showError('New password must be at least 8 characters.');
            return;
        }
        if (newPassword !== confirmPassword) {
            showError('New passwords do not match.');
            return;
        }

        const user = this.getUser();
        if (!user || !user.id) {
            showError('Unable to determine current user.');
            return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = 'Saving...';
        errorEl.classList.add('d-none');

        try {
            const response = await fetch(`/api/users/${user.id}/change-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getToken()}`
                },
                body: JSON.stringify({ currentPassword, newPassword })
            });

            if (!response.ok) {
                const data = await response.json().catch(() => ({}));
                showError(data.message || 'Failed to change password.');
                return;
            }

            bootstrap.Modal.getInstance(document.getElementById('changePasswordModal')).hide();
            if (typeof notifications !== 'undefined') {
                notifications.success('Password changed successfully.');
            }
        } catch (e) {
            showError('An error occurred. Please try again.');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Change Password';
        }
    },

    updateToken(newAccessToken) {
        const remember = localStorage.getItem(this.TOKEN_KEY) !== null;
        const storage = remember ? localStorage : sessionStorage;
        storage.setItem(this.TOKEN_KEY, newAccessToken);
        // Update stored user's requiresContextSelection to false after context switch
        const user = this.getUser();
        if (user) {
            user.requiresContextSelection = false;
            storage.setItem(this.USER_KEY, JSON.stringify(user));
        }
    }
};

// Auto-initialize the navbar on every admin page
document.addEventListener('DOMContentLoaded', function () {
    // Only run on admin pages (not login/register)
    if (window.location.pathname.startsWith('/admin')) {
        auth.initNavbar();
    }
});
