document.addEventListener("DOMContentLoaded", function () {

    const shouldShowModal = window.serverData && window.serverData.showOtpModal === true;


    if (shouldShowModal) {
        const modalElement = document.getElementById('otpModal');
        if (modalElement) {
            const otpModal = new bootstrap.Modal(modalElement);
            otpModal.show();
        }
    }

    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', function (event) {
            const password = document.getElementById('register-password').value;
            const confirmPassword = document.getElementById('register-confirm-password').value;
            const errorDiv = document.getElementById('js-error');

            if (password !== confirmPassword) {
                event.preventDefault();
                if (errorDiv) {
                    errorDiv.textContent = "Confirm password does not match!";
                    errorDiv.classList.remove('d-none');
                } else {
                    alert("Confirm password does not match!");
                }
            } else {
                if (errorDiv) errorDiv.classList.add('d-none');
            }
        });
    }
});