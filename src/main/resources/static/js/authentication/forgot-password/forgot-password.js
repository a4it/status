document.addEventListener('DOMContentLoaded', function() {
    const forgotPasswordForm = document.getElementById('forgotPasswordForm');
    const emailInput = document.getElementById('email');
    
    forgotPasswordForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        // Reset validation state
        emailInput.classList.remove('is-invalid');
        
        // Validate email
        if (!emailInput.value.trim() || !emailInput.checkValidity()) {
            emailInput.classList.add('is-invalid');
            return;
        }
        
        const submitButton = forgotPasswordForm.querySelector('button[type="submit"]');
        const originalText = submitButton.innerHTML;
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Sending reset link...';
        
        try {
            const response = await fetch('/api/auth/forgot-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    email: emailInput.value.trim()
                })
            });
            
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to send reset link');
            }
            
            notifications.show('Password reset link has been sent to your email address.', 'success');
            
            // Clear the form
            emailInput.value = '';
            
            // Redirect to login after a delay
            setTimeout(() => {
                window.location.href = '/login';
            }, 3000);
            
        } catch (error) {
            console.error('Password reset error:', error);
            notifications.show(error.message || 'Failed to send password reset link. Please try again.', 'error');
            
            submitButton.disabled = false;
            submitButton.innerHTML = originalText;
        }
    });
    
    // Focus on email input
    emailInput.focus();
});