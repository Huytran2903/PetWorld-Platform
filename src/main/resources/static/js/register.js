document.addEventListener("DOMContentLoaded", function () {
    function setupPasswordToggle(inputId, iconId) {
        const input = document.getElementById(inputId);
        const icon = document.getElementById(iconId);

        if (input && icon) {
            icon.addEventListener('click', function () {
                if (input.type === 'password') {
                    input.type = 'text';
                    icon.classList.remove('bi-eye-slash');
                    icon.classList.add('bi-eye');
                    icon.style.color = '#FFB25B';
                } else {
                    input.type = 'password';
                    icon.classList.remove('bi-eye');
                    icon.classList.add('bi-eye-slash');
                    icon.style.color = '#6c757d';
                }
            });
        }
    }

    setupPasswordToggle('register-password', 'togglePassword');
    setupPasswordToggle('register-confirm-password', 'toggleConfirmPassword');



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


    const shouldShowModal = window.serverData && window.serverData.showOtpModal === true;
    const modalElement = document.getElementById('otpModal');

    if (shouldShowModal && modalElement) {
        const otpModal = new bootstrap.Modal(modalElement);
        otpModal.show();
    }


    let timeLeft = 60;
    const resendBtn = document.getElementById("resendOtpBtn");
    const timerText = document.getElementById("timerText");

    if (resendBtn && timerText && modalElement) {
        const countdown = setInterval(function () {
            timeLeft--;
            timerText.textContent = `(${timeLeft}s)`;

            if (timeLeft <= 0) {
                clearInterval(countdown);
                timerText.textContent = "";

                resendBtn.style.color = "#FFB25B";
                resendBtn.style.pointerEvents = "auto";
                resendBtn.classList.remove("disabled-link");
            }
        }, 1000);

        resendBtn.addEventListener('click', function () {
            if (timeLeft <= 0) {
                const resendForm = document.getElementById("resendForm");
                if (resendForm) {
                    resendForm.submit();
                }
            }
        });
    }

});