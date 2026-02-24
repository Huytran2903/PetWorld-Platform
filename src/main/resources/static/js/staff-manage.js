// ==========================================
// 1. LOGIC CHO TÍNH NĂNG THÊM NHÂN VIÊN
// ==========================================
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

// ==========================================
// 2. LOGIC CHO TÍNH NĂNG XÓA NHÂN VIÊN
// ==========================================
document.addEventListener('DOMContentLoaded', function () {

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