// =========================================================================
// 1. HÀM XỬ LÝ ẨN/HIỆN DROPDOWN STAFF KHI TICK VACCINE (Dành cho trang khác nếu có)
// =========================================================================
function toggleStaffSelect(checkBox) {
    var staffDiv = document.getElementById("staffSelectDiv");
    if (staffDiv) {
        if (checkBox.checked) {
            staffDiv.style.display = "block";
        } else {
            staffDiv.style.display = "none";
            const staffSelect = document.querySelector("select[name='vaccinationStaffID']");
            if (staffSelect) staffSelect.value = "";
        }
    }
}

// =========================================================================
// 2. HÀM XỬ LÝ PREVIEW ẢNH KHI UPLOAD
// =========================================================================
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

        if (fileName) fileName.textContent = file.name;

        const reader = new FileReader();
        reader.onload = e => {
            if (preview) preview.src = e.target.result;
            if (previewBox) previewBox.style.display = "block";
            if (placeholder) placeholder.style.display = "none";
        };
        reader.readAsDataURL(file);
    }
}

// =========================================================================
// 3. TẤT CẢ LOGIC KHỞI TẠO GIAO DIỆN (CHỈ DÙNG 1 KHỐI DOMContentLoaded)
// =========================================================================
document.addEventListener('DOMContentLoaded', function () {

    // --- A. XỬ LÝ ĐỊNH DẠNG TIỀN TỆ (VNĐ) ---
    const priceDisplay = document.getElementById('priceDisplay');
    const priceActual = document.getElementById('priceActual');

    function formatCurrency(value) {
        if (!value) return '';
        let number = value.toString().replace(/\D/g, ''); // Xóa ký tự không phải số
        if (number === '') return '';
        return new Intl.NumberFormat('vi-VN').format(number);
    }

    if (priceDisplay && priceActual) {
        // 1. Khi load trang (ví dụ Edit), format số từ DB hiển thị lên
        if (priceActual.value) {
            let initValue = priceActual.value.split('.')[0];
            priceDisplay.value = formatCurrency(initValue);
            priceActual.value = initValue;
        }

        // 2. Bắt sự kiện mỗi khi người dùng gõ phím
        priceDisplay.addEventListener('input', function (e) {
            let rawValue = this.value.replace(/\D/g, ''); // Lấy số thô
            priceActual.value = rawValue; // Gắn số thô vào ô ẩn
            this.value = formatCurrency(rawValue); // Hiển thị số đẹp lên màn hình
        });

        // 3. [QUAN TRỌNG] Chốt chặn cuối cùng: Xử lý trước khi Submit Form
        const petForm = document.querySelector('form[action*="/admin/pet/save"]');
        if (petForm) {
            petForm.addEventListener('submit', function () {
                // Đảm bảo 100% ô ẩn có dữ liệu số thô trước khi gửi về Server
                let finalValue = priceDisplay.value.replace(/\D/g, '');
                priceActual.value = finalValue;
            });
        }
    }

    // --- B. XỬ LÝ MODAL VACCINE VÀ VALIDATE NGÀY THÁNG ---
    const checkbox = document.getElementById('isVaccinatedCheck');
    const vaccineModalEl = document.getElementById('vaccineModal');
    const nextDueDateInput = document.getElementById('modalNextDueDate'); // Ô nhập ngày tiêm

    if (checkbox && vaccineModalEl) {
        let isVaccinatedPreviously = checkbox.checked; // Lưu trạng thái ban đầu
        const vaccineModal = new bootstrap.Modal(vaccineModalEl);

        // [MỚI] Khóa các ngày trong quá khứ trên DatePicker của trình duyệt
        if (nextDueDateInput) {
            let today = new Date();
            // Lấy chuỗi yyyy-mm-dd theo múi giờ local
            let year = today.getFullYear();
            let month = String(today.getMonth() + 1).padStart(2, '0');
            let day = String(today.getDate()).padStart(2, '0');
            nextDueDateInput.setAttribute('min', `${year}-${month}-${day}`);
        }

        // Bắt sự kiện khi click vào công tắc Vaccinated
        checkbox.addEventListener('change', function () {
            if (this.checked) {
                vaccineModal.show();
            } else {
                isVaccinatedPreviously = false;
            }
        });

        // Hàm xử lý khi bấm nút "Cancel" hoặc nút "X" trên Modal
        window.cancelVaccine = function () {
            checkbox.checked = isVaccinatedPreviously; // Trả lại trạng thái trước đó
            vaccineModal.hide();
        };

        // Hàm xử lý khi bấm nút "Confirm" trên Modal
        window.confirmVaccine = function () {
            // Validate 1: Bắt buộc nhập tên Vaccine
            const vName = document.getElementById('modalVaccineName').value;
            if (vName.trim() === '') {
                alert("Please enter the Vaccine Name!");
                return;
            }

            // Validate 2: Bắt lỗi nếu người dùng cố tình gõ tay ngày quá khứ
            if (nextDueDateInput && nextDueDateInput.value) {
                let selectedDate = new Date(nextDueDateInput.value);
                let todayDate = new Date();
                todayDate.setHours(0, 0, 0, 0); // Đưa về 0h để so sánh chính xác ngày

                if (selectedDate < todayDate) {
                    alert("Next due date cannot be in the past! Please select today or a future date.");
                    nextDueDateInput.value = ''; // Xóa ngày sai đi
                    nextDueDateInput.focus(); // Nháy chuột vào ô ngày để nhập lại
                    return; // Chặn lại, không cho tắt form
                }
            }

            // Nếu vượt qua hết validate -> Cập nhật trạng thái hợp lệ và tắt modal
            isVaccinatedPreviously = true;
            vaccineModal.hide();
        };
    }
});