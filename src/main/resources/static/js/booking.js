document.addEventListener("DOMContentLoaded", function() {
    const form = document.getElementById("bookForm");
    const sumServices = document.getElementById("sumServices");
    const sumDuration = document.getElementById("sumDuration");
    const sumTotal = document.getElementById("sumTotal");

    // Hàm định dạng tiền tệ VN
    function formatVND(n) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n);
    }

    // Hàm tính toán
    function calculateTotal() {
        // Lấy tất cả input dịch vụ đang được checked
        const inputs = form.querySelectorAll("input[name='mainServices']");
        let selectedInputs = [];
        inputs.forEach(inp => {
            if (inp.checked) selectedInputs.push(inp);
        });

        // Xử lý số lượng (nếu là boarding)
        const qtyInput = document.getElementById("boardingQty");
        let qty = 1;
        if (qtyInput) {
            qty = parseInt(qtyInput.value) || 1;
        }

        let total = 0;
        let totalDuration = 0;
        let names = [];

        selectedInputs.forEach(inp => {
            const price = parseFloat(inp.getAttribute("data-price")) || 0;
            const duration = parseFloat(inp.getAttribute("data-duration")) || 0;
            const name = inp.getAttribute("data-name");

            // Nếu là boarding, giá nhân theo số lượng ngày/giờ
            // Nếu không (spa/vaccine), giá là cố định 1 lần
            // (Logic này tùy thuộc rule kinh doanh của bạn, ở đây giả sử boarding nhân theo qty)
            if(document.getElementById("boardingQtyBlock")) {
                total += price * qty;
                totalDuration += duration * qty;
            } else {
                total += price;
                totalDuration += duration;
            }

            names.push(name);
        });

        // Cập nhật UI
        sumServices.textContent = names.length > 0 ? names.join(", ") : "—";
        sumTotal.textContent = names.length > 0 ? formatVND(total) : "—";

        // Format thời gian (phút -> giờ)
        if (totalDuration > 0) {
            const hours = Math.floor(totalDuration / 60);
            const minutes = totalDuration % 60;
            let timeStr = "";
            if(hours > 0) timeStr += `${hours} giờ `;
            if(minutes > 0) timeStr += `${minutes} phút`;
            sumDuration.textContent = timeStr;
        } else {
            sumDuration.textContent = "—";
        }
    }

    // Lắng nghe sự kiện thay đổi
    form.addEventListener("change", (e) => {
        if (e.target.matches("input[name='mainServices'], #boardingQty")) {
            calculateTotal();
        }
    });

    // Tính toán lần đầu khi trang load
    calculateTotal();

    // Xử lý trước khi submit (nếu cần gộp note)
    form.addEventListener("submit", (e) => {
        const boardingHint = document.getElementById("boardingHint");
        const note = document.getElementById("note");

        if(boardingHint && boardingHint.value.trim() !== "") {
            note.value = `[Đưa đón: ${boardingHint.value}] \n` + note.value;
        }
    });
});