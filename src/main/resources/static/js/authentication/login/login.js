document.addEventListener('DOMContentLoaded', function() {
    // If already authenticated, redirect to admin
    if (auth.isAuthenticated()) {
        window.location.href = '/admin';
        return;
    }

    const loginForm = document.getElementById('loginForm');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const rememberMeCheckbox = document.getElementById('rememberMe');
    const togglePassword = document.getElementById('togglePassword');

    notifications.showFlashMessages();
    
    togglePassword.addEventListener('click', function(e) {
        e.preventDefault();
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        
        const icon = this.querySelector('svg');
        if (type === 'password') {
            icon.innerHTML = '<path stroke="none" d="M0 0h24v24H0z" fill="none"/><circle cx="12" cy="12" r="2" /><path d="M22 12c-2.667 4.667 -6 7 -10 7s-7.333 -2.333 -10 -7c2.667 -4.667 6 -7 10 -7s7.333 2.333 10 7" />';
        } else {
            icon.innerHTML = '<path stroke="none" d="M0 0h24v24H0z" fill="none"/><line x1="3" y1="3" x2="21" y2="21" /><path d="M10.584 10.587a2 2 0 0 0 2.829 2.828" /><path d="M9.363 5.365a9.466 9.466 0 0 1 2.637 -.365c4 0 7.333 2.333 10 7c-.778 1.361 -1.612 2.524 -2.503 3.488m-2.14 1.861c-1.631 1.1 -3.415 1.651 -5.357 1.651c-4 0 -7.333 -2.333 -10 -7c1.369 -2.395 2.913 -4.175 4.632 -5.341" />';
        }
    });
    
    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        // Reset validation states
        emailInput.classList.remove('is-invalid');
        passwordInput.classList.remove('is-invalid');
        
        // Validate inputs
        let isValid = true;
        
        if (!emailInput.value.trim() || !emailInput.checkValidity()) {
            emailInput.classList.add('is-invalid');
            isValid = false;
        }
        
        if (!passwordInput.value) {
            passwordInput.classList.add('is-invalid');
            isValid = false;
        }
        
        if (!isValid) {
            return;
        }
        
        const submitButton = loginForm.querySelector('button[type="submit"]');
        const originalText = submitButton.innerHTML;
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Signing in...';
        
        try {
            const email = emailInput.value.trim();
            const password = passwordInput.value;
            const rememberMe = rememberMeCheckbox.checked;
            
            const result = await auth.login(email, password, rememberMe);
            
            notifications.show('Login successful! Redirecting...', 'success');
            
            setTimeout(() => {
                window.location.href = '/admin';
            }, 1000);
            
        } catch (error) {
            console.error('Login error:', error);
            notifications.show(error.message || 'Invalid email or password', 'error');
            
            // Add invalid state to inputs on error
            emailInput.classList.add('is-invalid');
            passwordInput.classList.add('is-invalid');
            
            submitButton.disabled = false;
            submitButton.innerHTML = originalText;
        }
    });
    
    emailInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !passwordInput.value) {
            e.preventDefault();
            passwordInput.focus();
        }
    });
});