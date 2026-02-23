const detailModal = document.getElementById('detailModal');
if (detailModal) {
    detailModal.addEventListener('show.bs.modal', event => {
        const button = event.relatedTarget;

        const firstName = button.getAttribute('data-firstname');
        const lastName = button.getAttribute('data-lastname');
        const phone = button.getAttribute('data-phone');
        const date = button.getAttribute('data-date');
        const status = button.getAttribute('data-status');

        detailModal.querySelector('#modalFirstName').value = firstName;
        detailModal.querySelector('#modalLastName').value = lastName;
        detailModal.querySelector('#modalPhone').value = phone;
        detailModal.querySelector('#modalDate').value = date;

        const badge = detailModal.querySelector('#modalStatusBadge');
        badge.textContent = status;

        badge.className = 'status-badge';
        const statusClass = 'status-' + status.toLowerCase().replace(' ', '_');
        badge.classList.add(statusClass);
    });
}

const assignModal = document.getElementById('assignModal');
if (assignModal) {
    assignModal.addEventListener('show.bs.modal', event => {
        const button = event.relatedTarget;

        const firstName = button.getAttribute('data-firstname');
        const lastName = button.getAttribute('data-lastname');
        const phone = button.getAttribute('data-phone');
        const date = button.getAttribute('data-date');

        assignModal.querySelector('#assignFirstName').value = firstName;
        assignModal.querySelector('#assignLastName').value = lastName;
        assignModal.querySelector('#assignPhone').value = phone;
        assignModal.querySelector('#assignDate').value = date;

        assignModal.querySelector('#assignStaffSelect').selectedIndex = 0;
    });
}