const orderModal = document.getElementById('orderDetailModal');
if (orderModal) {
    orderModal.addEventListener('show.bs.modal', event => {
        const button = event.relatedTarget;

        const id = button.getAttribute('data-id');
        const customer = button.getAttribute('data-customer');
        const product = button.getAttribute('data-product');
        const date = button.getAttribute('data-date');
        const amount = button.getAttribute('data-amount');
        const status = button.getAttribute('data-status');

        orderModal.querySelector('#modalOrderId').value = id;
        orderModal.querySelector('#modalCustomerName').value = customer;
        orderModal.querySelector('#modalProductName').value = product;
        orderModal.querySelector('#modalOrderDate').value = date;
        orderModal.querySelector('#modalAmount').value = amount;

        const badge = orderModal.querySelector('#modalStatusBadge');
        badge.textContent = status;

        badge.className = 'badge-status';
        if (status === 'Completed') badge.classList.add('status-completed');
        else if (status === 'Rejected') badge.classList.add('status-rejected');
        else badge.classList.add('status-pending');
    });
}