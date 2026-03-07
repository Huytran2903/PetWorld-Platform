// 1. HÀM XỬ LÝ ẨN/HIỆN DROPDOWN STAFF KHI TICK VACCINE
function toggleStaffSelect(checkBox) {
    var staffDiv = document.getElementById("staffSelectDiv");

    // Đã dùng checkBox (được truyền trực tiếp từ HTML qua chữ 'this')
    if (checkBox.checked) {
        staffDiv.style.display = "block";
    } else {
        staffDiv.style.display = "none";
        // Reset Dropdown về giá trị rỗng ("") khi tắt công tắc
        document.querySelector("select[name='vaccinationStaffID']").value = "";
    }
}

// 2. HÀM XỬ LÝ PREVIEW ẢNH KHI UPLOAD
function previewImage(input) {
    const preview = document.getElementById("img-preview");
    const previewBox = document.getElementById("preview-box");
    const placeholder = document.getElementById("placeholder-box");
    const fileName = document.getElementById("file-name");

    if (input.files && input.files[0]) {
        const file = input.files[0];

        // Ràng buộc kích thước file tối đa 5MB
        if (file.size > 5 * 1024 * 1024) {
            alert("File ảnh quá lớn (Max 5MB)! Vui lòng chọn ảnh khác.");
            input.value = ""; // Reset input
            return;
        }

        fileName.textContent = file.name;

        const reader = new FileReader();
        reader.onload = e => {
            preview.src = e.target.result;
            previewBox.style.display = "block";
            placeholder.style.display = "none";
        };
        reader.readAsDataURL(file);
    }
}

// 3. HÀM XỬ LÝ ĐỊNH DẠNG TIỀN TỆ (VNĐ)
document.addEventListener('DOMContentLoaded', function () {
    const displayInput = document.getElementById('priceDisplay');
    const actualInput = document.getElementById('priceActual');

    function formatCurrency(value) {
        if (!value) return '';
        let number = value.toString().replace(/\D/g, '');
        if (number === '') return '';
        return new Intl.NumberFormat('vi-VN').format(number);
    }

    // Khi load trang (ví dụ Edit), format lại số hiển thị từ database
    if (actualInput.value) {
        let initValue = actualInput.value.split('.')[0].split(',')[0];
        displayInput.value = formatCurrency(initValue);
        actualInput.value = initValue;
    }

    // Khi người dùng đang nhập liệu
    displayInput.addEventListener('input', function (e) {
        let rawValue = this.value.replace(/\D/g, '');
        actualInput.value = rawValue;
        this.value = formatCurrency(rawValue);
    });
});