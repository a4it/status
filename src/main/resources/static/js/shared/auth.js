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
            organizationId: data.organizationId
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
        return true;
    },

    getUserDisplayName() {
        const user = this.getUser();
        return user ? (user.fullName || user.username) : 'User';
    },

    getUserRole() {
        const user = this.getUser();
        return user ? user.role : null;
    }
};
