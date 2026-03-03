document.addEventListener("DOMContentLoaded", function () {
    const urlParams = new URLSearchParams(window.location.search);
    const status = urlParams.get('verification_status');
    const justRegistered = urlParams.has('success_verify_sent');

    const shouldOpenOtp = window.shouldOpenOtp || false;

    const modalElement = document.getElementById('verificationModal');
    const otpModalElement = document.getElementById('otpModal');
    const iconDiv = document.getElementById('verifyIcon');
    const titleEl = document.getElementById('verifyTitle');
    const msgEl = document.getElementById('verifyMessage');

    let shouldShowVerifyModal = false;

    if (status) {
        shouldShowVerifyModal = true;
        if (status === 'success') {
            iconDiv.innerHTML = '<iconify-icon icon="mdi:check-circle" style="color: #28a745;"></iconify-icon>';
            titleEl.textContent = 'Account Verified!';
            msgEl.textContent = 'Your account has been activated successfully. You can now sign in.';
        } else if (status === 'expired') {
            iconDiv.innerHTML = '<iconify-icon icon="mdi:clock-alert" style="color: #ffc107;"></iconify-icon>';
            titleEl.textContent = 'Link Expired!';
            msgEl.textContent = 'This verification link has expired. Please register again.';
        } else if (status === 'invalid') {
            iconDiv.innerHTML = '<iconify-icon icon="mdi:close-circle" style="color: #dc3545;"></iconify-icon>';
            titleEl.textContent = 'Verification Failed!';
            msgEl.textContent = 'The verification token is invalid or the link is broken.';
        }
    } else if (justRegistered) {
        shouldShowVerifyModal = true;
        iconDiv.innerHTML = '<iconify-icon icon="mdi:email-fast" style="color: #17a2b8;"></iconify-icon>';
        titleEl.textContent = 'Registration Successful!';
        msgEl.textContent = 'We have sent a verification email. Please check your inbox to activate your account.';
    }

    if (shouldShowVerifyModal && modalElement) {
        const verifyModal = new bootstrap.Modal(modalElement);
        // Xóa tham số URL sau khi hiện modal để tránh hiện lại khi F5
        window.history.replaceState(null, null, window.location.pathname);
        verifyModal.show();
    }

    if (shouldOpenOtp && otpModalElement) {
        const otpModal = new bootstrap.Modal(otpModalElement);
        otpModal.show();
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