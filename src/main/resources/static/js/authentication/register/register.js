document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.getElementById('registerForm');
    const firstNameInput = document.getElementById('firstName');
    const lastNameInput = document.getElementById('lastName');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const agreeTermsCheckbox = document.getElementById('agreeTerms');
    const togglePassword = document.getElementById('togglePassword');
    
    // Password visibility toggle
    togglePassword.addEventListener('click', function(e) {
        e.preventDefault();
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        confirmPasswordInput.setAttribute('type', type);
        
        const icon = this.querySelector('svg');
        if (type === 'password') {
            icon.innerHTML = '<path stroke="none" d="M0 0h24v24H0z" fill="none"/><circle cx="12" cy="12" r="2" /><path d="M22 12c-2.667 4.667 -6 7 -10 7s-7.333 -2.333 -10 -7c2.667 -4.667 6 -7 10 -7s7.333 2.333 10 7" />';
        } else {
            icon.innerHTML = '<path stroke="none" d="M0 0h24v24H0z" fill="none"/><line x1="3" y1="3" x2="21" y2="21" /><path d="M10.584 10.587a2 2 0 0 0 2.829 2.828" /><path d="M9.363 5.365a9.466 9.466 0 0 1 2.637 -.365c4 0 7.333 2.333 10 7c-.778 1.361 -1.612 2.524 -2.503 3.488m-2.14 1.861c-1.631 1.1 -3.415 1.651 -5.357 1.651c-4 0 -7.333 -2.333 -10 -7c1.369 -2.395 2.913 -4.175 4.632 -5.341" />';
        }
    });
    
    // Real-time password match validation
    confirmPasswordInput.addEventListener('input', function() {
        if (this.value && this.value !== passwordInput.value) {
            this.classList.add('is-invalid');
        } else {
            this.classList.remove('is-invalid');
        }
    });
    
    // Form submission
    registerForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        // Reset validation states
        registerForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
        
        // Validate form
        let isValid = true;
        
        if (!firstNameInput.value.trim()) {
            firstNameInput.classList.add('is-invalid');
            isValid = false;
        }
        
        if (!lastNameInput.value.trim()) {
            lastNameInput.classList.add('is-invalid');
            isValid = false;
        }
        
        if (!emailInput.value.trim() || !emailInput.checkValidity()) {
            emailInput.classList.add('is-invalid');
            isValid = false;
        }
        
        if (passwordInput.value.length < 8) {
            passwordInput.classList.add('is-invalid');
            isValid = false;
        }
        
        if (passwordInput.value !== confirmPasswordInput.value) {
            confirmPasswordInput.classList.add('is-invalid');
            isValid = false;
        }
        
        if (!agreeTermsCheckbox.checked) {
            agreeTermsCheckbox.classList.add('is-invalid');
            isValid = false;
        }
        
        if (!isValid) {
            return;
        }
        
        // Disable submit button
        const submitButton = registerForm.querySelector('button[type="submit"]');
        const originalText = submitButton.innerHTML;
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Creating account...';
        
        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    firstName: firstNameInput.value.trim(),
                    lastName: lastNameInput.value.trim(),
                    email: emailInput.value.trim(),
                    password: passwordInput.value
                })
            });
            
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Registration failed');
            }
            
            const data = await response.json();
            
            notifications.show('Account created successfully! Redirecting to login...', 'success');
            
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
            
        } catch (error) {
            console.error('Registration error:', error);
            notifications.show(error.message || 'Failed to create account. Please try again.', 'error');
            
            submitButton.disabled = false;
            submitButton.innerHTML = originalText;
        }
    });
    
    // Enter key navigation
    const inputs = [firstNameInput, lastNameInput, emailInput, passwordInput, confirmPasswordInput];
    inputs.forEach((input, index) => {
        input.addEventListener('keypress', function(e) {
            if (e.key === 'Enter' && index < inputs.length - 1) {
                e.preventDefault();
                inputs[index + 1].focus();
            }
        });
    });
});