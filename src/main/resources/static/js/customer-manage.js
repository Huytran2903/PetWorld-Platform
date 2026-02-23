document.addEventListener("DOMContentLoaded", function () {

    const deleteModal = document.getElementById('deleteModal');
    if (deleteModal) {
        deleteModal.addEventListener('show.bs.modal', event => {
            const button = event.relatedTarget;
            const href = button.getAttribute('data-href');
            const confirmBtn = deleteModal.querySelector('#btnConfirmDelete');
            confirmBtn.href = href;
        });
    }

    const statusModal = document.getElementById('statusModal');
    if (statusModal) {
        statusModal.addEventListener('show.bs.modal', event => {
            const button = event.relatedTarget;
            const href = button.getAttribute('data-href');
            const type = button.getAttribute('data-status-type');

            const statusText = statusModal.querySelector('#statusActionText');
            statusText.textContent = type;

            const confirmBtn = statusModal.querySelector('#btnConfirmStatus');
            confirmBtn.href = href;
        });
    }
});