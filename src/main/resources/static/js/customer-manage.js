document.addEventListener('DOMContentLoaded', function () {
    const deleteModal = document.getElementById('deleteModal');
    if (deleteModal) {
        deleteModal.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            const href = button.getAttribute('data-href');
            const confirmBtn = deleteModal.querySelector('#btnConfirmDelete');
            confirmBtn.setAttribute('href', href);
        });
    }

    const statusModal = document.getElementById('statusModal');
    if (statusModal) {
        statusModal.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            const href = button.getAttribute('data-href');
            const statusType = button.getAttribute('data-status-type');

            const confirmBtn = statusModal.querySelector('#btnConfirmStatus');
            const actionText = statusModal.querySelector('#statusActionText');

            confirmBtn.setAttribute('href', href);
            actionText.textContent = statusType;
        });
    }
});
document.addEventListener('DOMContentLoaded', function () {

    // 1. Xử lý Logic Nút Xóa (Quan trọng nhất)
    const deleteBtns = document.querySelectorAll('.delete-btn');
    deleteBtns.forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();

            const hasPets = this.getAttribute('data-has-pets') === 'true';
            const name = this.getAttribute('data-name');
            const href = this.getAttribute('data-href');

            if (hasPets) {
                // Nếu có Pet -> Hiện popup Cảnh báo (Vàng)
                document.getElementById('restCustName').textContent = name;
                const modal = new bootstrap.Modal(document.getElementById('restrictedDeleteModal'));
                modal.show();
            } else {
                // Nếu không có Pet -> Hiện popup Xác nhận xóa (Đỏ)
                document.getElementById('delCustName').textContent = name;
                document.getElementById('btnConfirmDelete').setAttribute('href', href);
                const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
                modal.show();
            }
        });
    });

    // 2. Xử lý Status Modal (Giữ nguyên logic của bạn)
    const statusModal = document.getElementById('statusModal');
    if (statusModal) {
        statusModal.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            const href = button.getAttribute('data-href');
            const type = button.getAttribute('data-status-type');

            this.querySelector('#statusActionText').textContent = type;
            this.querySelector('#btnConfirmStatus').setAttribute('href', href);
        });
    }

    // 3. Xử lý Search nhanh (Bonus)
    const searchInput = document.querySelector('.search-box-custom input');
    if (searchInput) {
        searchInput.addEventListener('keyup', function () {
            const val = this.value.toLowerCase();
            document.querySelectorAll('tbody tr').forEach(row => {
                const text = row.innerText.toLowerCase();
                row.style.display = text.includes(val) ? "" : "none";
            });
        });
    }
});