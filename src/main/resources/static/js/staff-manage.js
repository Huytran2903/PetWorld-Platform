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

document.addEventListener('DOMContentLoaded', function () {
    const deleteBtns = document.querySelectorAll('.delete-staff-btn');
    const deleteModalElement = document.getElementById('transferDeleteModal');
    if (!deleteModalElement) return;
    const deleteModal = new bootstrap.Modal(deleteModalElement);

    deleteBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            // Lấy dữ liệu từ nút (ĐÃ THÊM APPOINTMENTS)
            const staffId = this.getAttribute('data-id');
            const name = this.getAttribute('data-name');
            const vaccines = parseInt(this.getAttribute('data-vaccines')) || 0;
            const health = parseInt(this.getAttribute('data-health')) || 0;
            const appointments = parseInt(this.getAttribute('data-appointments')) || 0; // Mới thêm

            // Gán data vào form
            document.getElementById('deleteStaffId').value = staffId;
            document.getElementById('deleteStaffName').textContent = name;

            // Ẩn nhân sự đang bị xóa khỏi Select
            const selectBox = document.getElementById('transferStaffSelect');
            Array.from(selectBox.options).forEach(opt => {
                opt.style.display = (opt.value === staffId) ? 'none' : '';
            });
            selectBox.value = '';

            // Xử lý hiển thị
            const warningDiv = document.getElementById('pendingWorkWarning');
            const noWorkDiv = document.getElementById('noWorkMessage');
            const submitBtn = document.getElementById('btnSubmitDelete');

            // KIỂM TRA THÊM APPOINTMENTS
            if (vaccines > 0 || health > 0 || appointments > 0) {
                warningDiv.style.display = 'block';
                noWorkDiv.style.display = 'none';

                document.getElementById('vaccineCount').textContent = vaccines;
                document.getElementById('healthCount').textContent = health;
                document.getElementById('appointmentCount').textContent = appointments; // Hiện số lượng

                submitBtn.disabled = true;
                selectBox.required = true;
            } else {
                warningDiv.style.display = 'none';
                noWorkDiv.style.display = 'block';

                submitBtn.disabled = false;
                selectBox.required = false;
            }

            deleteModal.show();
        });
    });

    const selectBox = document.getElementById('transferStaffSelect');
    if (selectBox) {
        selectBox.addEventListener('change', function () {
            document.getElementById('btnSubmitDelete').disabled = (this.value === "");
        });
    }
});