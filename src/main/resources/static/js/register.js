document.addEventListener("DOMContentLoaded", function () {

    // 1. Logic hiển thị Modal OTP
    const shouldShowModal = window.serverData && window.serverData.showOtpModal === true;

    if (shouldShowModal) {
        const modalElement = document.getElementById('otpModal');
        if (modalElement) {
            const otpModal = new bootstrap.Modal(modalElement);
            otpModal.show();
        }
    }

    const registerForm = document.getElementById('registerForm');
    const btnSignup = document.getElementById('btnSignup');

    if (registerForm) {
        registerForm.addEventListener('submit', function (event) {
            const password = document.getElementById('register-password').value;
            const confirmPassword = document.getElementById('register-confirm-password').value;
            const errorDiv = document.getElementById('js-error');

            if (password !== confirmPassword) {
                // Nếu sai pass -> Chặn form, báo lỗi
                event.preventDefault();
                if (errorDiv) {
                    errorDiv.textContent = "Confirm password does not match!";
                    errorDiv.classList.remove('d-none');
                } else {
                    alert("Confirm password does not match!");
                }
            } else {
                // NẾU MẬT KHẨU ĐÚNG: Xóa lỗi (nếu có) và KHÓA NÚT BẤM
                if (errorDiv) errorDiv.classList.add('d-none');

                if (btnSignup) {
                    btnSignup.disabled = true;
                    btnSignup.innerText = 'Processing...';
                    btnSignup.style.opacity = '0.7';
                }
            }
        });
    }
});