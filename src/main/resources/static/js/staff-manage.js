document.addEventListener("DOMContentLoaded", function () {

    const transferDeleteModal = document.getElementById('transferDeleteModal');

    if (transferDeleteModal) {
        transferDeleteModal.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            if (!button) return;

            const staffId = button.getAttribute('data-id');
            const staffName = button.getAttribute('data-name');
            const vaccines = parseInt(button.getAttribute('data-vaccines') || 0);
            const appointments = parseInt(button.getAttribute('data-appointments') || 0);

            document.getElementById('deleteStaffId').value = staffId;
            document.getElementById('deleteStaffName').textContent = staffName;

            document.getElementById('vaccineCount').textContent = vaccines;
            document.getElementById('appointmentCount').textContent = appointments;

            const totalTasks = vaccines + appointments;

            const pendingWarning = document.getElementById('pendingWorkWarning');
            const noWorkMessage = document.getElementById('noWorkMessage');
            const transferSelect = document.getElementById('transferStaffSelect');

            if (totalTasks > 0) {
                pendingWarning.style.display = 'block';
                noWorkMessage.style.display = 'none';

                Array.from(transferSelect.options).forEach(option => {
                    if (option.value === staffId) {
                        option.hidden = true;
                        option.disabled = true;
                    } else {
                        option.hidden = false;
                        option.disabled = false;
                    }
                });
                transferSelect.value = "";

            } else {
                pendingWarning.style.display = 'none';
                noWorkMessage.style.display = 'block';
                transferSelect.value = "";
            }
        });
    }

    const deleteStaffModal = document.getElementById('deleteStaffModal');
    if (deleteStaffModal) {
        deleteStaffModal.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            const deleteUrl = button.getAttribute('data-href');
            const confirmBtn = document.getElementById('btnConfirmDeleteStaff');

            if (confirmBtn && deleteUrl) {
                confirmBtn.setAttribute('href', deleteUrl);
            }
        });
    }
});

function saveStaff() {
    const modalEl = document.getElementById('addStaffModal');
    if (modalEl) {
        let modal = bootstrap.Modal.getInstance(modalEl);
        if (!modal) {
            modal = new bootstrap.Modal(modalEl);
        }
        alert("Add new Staff successfully");
        modal.hide();
        document.getElementById('addStaffForm').reset();
    }
}