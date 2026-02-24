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