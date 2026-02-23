function confirmDelete(name) {
    if (confirm(`Bạn có chắc chắn muốn xóa nhân viên ${name} không?`)) {
        alert(`Đã xóa nhân viên ${name} thành công!`);

    }
}


function saveStaff() {
    const modalEl = document.getElementById('addStaffModal');
    const modal = bootstrap.Modal.getInstance(modalEl);

    alert("Đã thêm nhân viên mới thành công!");
    modal.hide();
    // Reset form
    document.getElementById('addStaffForm').reset();
}