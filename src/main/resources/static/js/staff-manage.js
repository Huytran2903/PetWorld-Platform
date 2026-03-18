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
// 2. LOGIC CHO TÍNH NĂNG XÓA & BÀN GIAO NHÂN VIÊN
// ==========================================
document.addEventListener("DOMContentLoaded", function () {

    // ----------------------------------------------------------------
    // Logic cho Modal Xóa có Bàn giao (transferDeleteModal)
    // ----------------------------------------------------------------
    const transferDeleteModal = document.getElementById('transferDeleteModal');

    if (transferDeleteModal) {
        transferDeleteModal.addEventListener('show.bs.modal', function (event) {
            // Lấy nút thùng rác vừa được bấm
            const button = event.relatedTarget;
            if (!button) return;

            // Trích xuất data từ nút
            const staffId = button.getAttribute('data-id');
            const staffName = button.getAttribute('data-name');
            const vaccines = parseInt(button.getAttribute('data-vaccines') || 0);
            const health = parseInt(button.getAttribute('data-health') || 0);
            const appointments = parseInt(button.getAttribute('data-appointments') || 0);

            // Gán tên và ID vào form ẩn để gửi lên Controller
            document.getElementById('deleteStaffId').value = staffId;
            document.getElementById('deleteStaffName').textContent = staffName;

            // Gán số lượng vào thẻ HTML hiển thị
            document.getElementById('vaccineCount').textContent = vaccines;
            document.getElementById('healthCount').textContent = health;
            document.getElementById('appointmentCount').textContent = appointments;

            // Tính tổng công việc
            const totalTasks = vaccines + health + appointments;

            // Lấy các element để xử lý Ẩn/Hiện
            const pendingWarning = document.getElementById('pendingWorkWarning');
            const noWorkMessage = document.getElementById('noWorkMessage');
            const transferSelect = document.getElementById('transferStaffSelect');

            if (totalTasks > 0) {
                // CÓ CÔNG VIỆC -> Hiện bảng vàng, ẩn bảng xám
                pendingWarning.style.display = 'block';
                noWorkMessage.style.display = 'none';

                // UX Tối ưu: Ẩn nhân viên đang bị xóa khỏi danh sách bàn giao
                Array.from(transferSelect.options).forEach(option => {
                    if (option.value === staffId) {
                        option.style.display = 'none'; // Giấu đi
                    } else {
                        option.style.display = 'block'; // Hiện người khác
                    }
                });
                transferSelect.value = ""; // Reset lựa chọn về mặc định

            } else {
                // KHÔNG CÓ CÔNG VIỆC -> Hiện bảng xám, ẩn bảng vàng
                pendingWarning.style.display = 'none';
                noWorkMessage.style.display = 'block';
                transferSelect.value = "";
            }
        });
    }

    // ----------------------------------------------------------------
    // Logic cho Modal Xóa cũ (Giữ lại phòng khi bạn dùng ở bảng khác)
    // ----------------------------------------------------------------
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