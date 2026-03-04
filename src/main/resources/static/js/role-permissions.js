document.addEventListener("DOMContentLoaded", function () {

    const alerts = document.querySelectorAll('.alert');
    if (alerts.length > 0) {
        setTimeout(function () {
            alerts.forEach(function (alert) {
                let bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            });
        }, 5000);
    }

    const permissionForm = document.getElementById('permissionForm');
    if (permissionForm) {
        permissionForm.addEventListener('submit', function (e) {
            if (!confirm("Are you sure you want to save these permissions?")) {
                e.preventDefault();
            }
        });
    }
});