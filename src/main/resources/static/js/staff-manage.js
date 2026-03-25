
function saveStaff() {
    const modalEl = document.getElementById('addStaffModal');
    if (modalEl) {
        let modal = bootstrap.Modal.getInstance(modalEl);
        if (!modal) {
            modal = new bootstrap.Modal(modalEl);
        }
        alert("Đã thêm nhân viên mới thành công!");
        modal.hide();
        document.getElementById('addStaffForm').reset();
    }
}


document.addEventListener("DOMContentLoaded", function () {


    const transferDeleteModal = document.getElementById('transferDeleteModal');

    if (transferDeleteModal) {
        transferDeleteModal.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            if (!button) return;

            const staffId = button.getAttribute('data-id');
            const staffName = button.getAttribute('data-name');
            const vaccines = parseInt(button.getAttribute('data-vaccines') || 0);
            const health = parseInt(button.getAttribute('data-health') || 0);
            const appointments = parseInt(button.getAttribute('data-appointments') || 0);

            document.getElementById('deleteStaffId').value = staffId;
            document.getElementById('deleteStaffName').textContent = staffName;

            document.getElementById('vaccineCount').textContent = vaccines;
            document.getElementById('healthCount').textContent = health;
            document.getElementById('appointmentCount').textContent = appointments;

            const totalTasks = vaccines + health + appointments;

            const pendingWarning = document.getElementById('pendingWorkWarning');
            const noWorkMessage = document.getElementById('noWorkMessage');
            const transferSelect = document.getElementById('transferStaffSelect');

            if (totalTasks > 0) {
                pendingWarning.style.display = 'block';
                noWorkMessage.style.display = 'none';

                Array.from(transferSelect.options).forEach(option => {
                    if (option.value === staffId) {
                        option.style.display = 'none';
                    } else {
                        option.style.display = 'block';
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