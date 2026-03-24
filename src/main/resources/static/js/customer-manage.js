document.addEventListener('DOMContentLoaded', function () {

    // 1. Xử lý Delete Modal
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