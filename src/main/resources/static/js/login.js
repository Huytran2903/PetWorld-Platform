document.addEventListener("DOMContentLoaded", function () {

    const urlParams = new URLSearchParams(window.location.search);
    const status = urlParams.get('verification_status');
    const justRegistered = urlParams.has('success_verify_sent');

    const modalElement = document.getElementById('verificationModal');
    const iconDiv = document.getElementById('verifyIcon');
    const titleEl = document.getElementById('verifyTitle');
    const msgEl = document.getElementById('verifyMessage');

    let shouldShowVerifyModal = false;

    if (status) {
        shouldShowVerifyModal = true;
        if (status === 'success') {
            if (iconDiv) iconDiv.innerHTML = '<iconify-icon icon="mdi:check-circle" style="color: #28a745;"></iconify-icon>';
            if (titleEl) titleEl.textContent = 'Account Verified!';
            if (msgEl) msgEl.textContent = 'Your account has been activated successfully. You can now sign in.';
        } else if (status === 'expired') {
            if (iconDiv) iconDiv.innerHTML = '<iconify-icon icon="mdi:clock-alert" style="color: #ffc107;"></iconify-icon>';
            if (titleEl) titleEl.textContent = 'Link Expired!';
            if (msgEl) msgEl.textContent = 'This verification link has expired. Please register again.';
        } else if (status === 'invalid') {
            if (iconDiv) iconDiv.innerHTML = '<iconify-icon icon="mdi:close-circle" style="color: #dc3545;"></iconify-icon>';
            if (titleEl) titleEl.textContent = 'Verification Failed!';
            if (msgEl) msgEl.textContent = 'The verification token is invalid or the link is broken.';
        }
    } else if (justRegistered) {
        shouldShowVerifyModal = true;
        if (iconDiv) iconDiv.innerHTML = '<iconify-icon icon="mdi:email-fast" style="color: #17a2b8;"></iconify-icon>';
        if (titleEl) titleEl.textContent = 'Registration Successful!';
        if (msgEl) msgEl.textContent = 'We have sent a verification email. Please check your inbox to activate your account.';
    }

    if (shouldShowVerifyModal && modalElement) {
        const verifyModal = new bootstrap.Modal(modalElement);
        verifyModal.show();
        window.history.replaceState(null, null, window.location.pathname);
    }


    const shouldOpenOtp = window.shouldOpenOtp || false;
    const otpModalElement = document.getElementById('otpModal');

    let timeLeft = 60;
    const resendBtn = document.getElementById("resendForgotOtpBtn");
    const timerText = document.getElementById("forgotTimerText");

    if (shouldOpenOtp && otpModalElement) {
        const otpModal = new bootstrap.Modal(otpModalElement);
        otpModal.show();

        if (resendBtn && timerText) {
            resendBtn.style.color = "#9e9e9e";
            resendBtn.style.pointerEvents = "none";
            resendBtn.classList.add("disabled-link");

            const countdown = setInterval(function () {
                timeLeft--;
                timerText.textContent = `(${timeLeft}s)`;

                if (timeLeft <= 0) {
                    clearInterval(countdown);
                    timerText.textContent = "";

                    resendBtn.style.color = "#ff9900";
                    resendBtn.style.pointerEvents = "auto";
                    resendBtn.classList.remove("disabled-link");
                }
            }, 1000);

            resendBtn.addEventListener('click', function() {
                if (timeLeft <= 0) {
                    const resendForm = document.getElementById("resendForgotForm");
                    if (resendForm) {
                        resendForm.submit();
                    }
                }
            });
        }
    }


    const forms = document.querySelectorAll('.needs-validation');
    Array.from(forms).forEach(form => {
        form.addEventListener('submit', event => {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });

});