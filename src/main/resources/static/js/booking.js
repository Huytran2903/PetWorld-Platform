document.addEventListener("DOMContentLoaded", function() {
    const form = document.getElementById("bookForm");
    const sumServiceType = document.getElementById("sumServiceType");
    const sumServices = document.getElementById("sumServices");
    const sumDuration = document.getElementById("sumDuration");
    const sumTotal = document.getElementById("sumTotal");
    const petSelect = document.getElementById("petId");
    const appointmentDateInput = document.getElementById("appointmentDate");

    // Format currency (VND, English locale for display)
    function formatVND(n) {
        return new Intl.NumberFormat('en-US', { style: 'decimal', minimumFractionDigits: 0 }).format(n) + " VND";
    }

    function isVaccineCheckbox(inp) {
        const t = (inp.getAttribute("data-type") || "").trim().toLowerCase();
        return t === "vaccine" || t === "vaccination";
    }

    function getEffectiveDateISO() {
        const v = (appointmentDateInput?.value || "").trim();
        if (!v) return null;
        // datetime-local returns "YYYY-MM-DDTHH:mm"
        const datePart = v.split("T")[0];
        return datePart || null;
    }

    async function refreshVaccineEligibility() {
        if (!form || !petSelect) return;
        const petId = (petSelect.value || "").trim();
        const inputs = form.querySelectorAll("input[name='mainServices']");

        // reset: clear vaccine disable state
        inputs.forEach(inp => {
            if (!isVaccineCheckbox(inp)) return;
            inp.disabled = false;
            inp.removeAttribute("title");
            inp.removeAttribute("data-lock-message");
            inp.closest("tr")?.classList.remove("svc-disabled");
        });

        if (!petId) return;

        const onDate = getEffectiveDateISO();
        const url = new URL("/api/vaccines/eligibility", window.location.origin);
        url.searchParams.set("petId", petId);
        if (onDate) url.searchParams.set("onDate", onDate);

        try {
            const res = await fetch(url.toString(), { headers: { "Accept": "application/json" } });
            if (!res.ok) return;
            const data = await res.json();
            const locks = Array.isArray(data?.locks) ? data.locks : [];

            // Build set of locked vaccine names (case-insensitive)
            const lockedByName = new Map();
            locks.forEach(l => {
                const name = (l?.vaccineName || "").trim();
                if (!name) return;
                lockedByName.set(name.toLowerCase(), l);
            });

            inputs.forEach(inp => {
                if (!isVaccineCheckbox(inp)) return;
                const svcName = (inp.getAttribute("data-name") || "").trim();
                if (!svcName) return;
                const lock = lockedByName.get(svcName.toLowerCase());
                if (!lock) return;

                // If it's locked, disable and uncheck
                inp.checked = false;
                inp.disabled = true;
                const nextDue = lock.nextDueDate;
                const lockMessage =
                    lock.reason === "missing_next_due"
                        ? `This pet has already received the "${svcName}" vaccine. The next due date is missing, so it cannot be booked again. Please contact staff.`
                        : `This pet has already received the "${svcName}" vaccine. Next due date: ${nextDue}.`;
                // Tooltip (hover) + message (click)
                inp.setAttribute("title", lockMessage);
                inp.setAttribute("data-lock-message", lockMessage);
                inp.closest("tr")?.classList.add("svc-disabled");
            });

            calculateTotal();
        } catch (e) {
            // ignore
        }
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
        let serviceTypes = new Set();

        selectedInputs.forEach(inp => {
            const price = parseFloat(inp.getAttribute("data-price")) || 0;
            const duration = parseFloat(inp.getAttribute("data-duration")) || 0;
            const name = inp.getAttribute("data-name");
            const svcType = inp.closest(".svc-item")?.querySelector(".svc-badge")?.textContent?.trim();
            if (svcType) serviceTypes.add(svcType);

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
        if (sumServiceType) {
            const typesArr = Array.from(serviceTypes);
            sumServiceType.textContent = typesArr.length > 0 ? typesArr.join(", ") : "—";
        }

        sumServices.textContent = names.length > 0 ? names.join(", ") : "—";
        sumTotal.textContent = names.length > 0 ? formatVND(total) : "—";

        // Format duration (minutes -> hours/minutes, English)
        if (totalDuration > 0) {
            const hours = Math.floor(totalDuration / 60);
            const minutes = totalDuration % 60;
            let timeStr = "";
            if (hours > 0) timeStr += `${hours} ${hours === 1 ? "hour" : "hours"} `;
            if (minutes > 0) timeStr += `${minutes} ${minutes === 1 ? "minute" : "minutes"}`;
            sumDuration.textContent = timeStr.trim();
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
    refreshVaccineEligibility();

    // Show message when user clicks a locked vaccine row/checkbox
    form.addEventListener("click", (e) => {
        const target = e.target;

        // If clicking directly on a disabled checkbox won't fire (browser), we also handle row click.
        const checkbox = target?.closest?.("input[name='mainServices']");
        if (checkbox && checkbox.disabled && isVaccineCheckbox(checkbox)) {
            const msg = checkbox.getAttribute("data-lock-message") || checkbox.getAttribute("title");
            if (msg) {
                e.preventDefault();
                alert(msg);
            }
            return;
        }

        const row = target?.closest?.("tr.svc-row.svc-disabled");
        if (row) {
            const cb = row.querySelector("input[name='mainServices'][data-lock-message]");
            const msg = cb?.getAttribute("data-lock-message") || cb?.getAttribute("title");
            if (msg) {
                e.preventDefault();
                alert(msg);
            }
        }
    });

    // Refresh eligibility when pet/date changes
    petSelect?.addEventListener("change", refreshVaccineEligibility);
    appointmentDateInput?.addEventListener("change", refreshVaccineEligibility);

    // Xử lý trước khi submit (nếu cần gộp note)
    form.addEventListener("submit", (e) => {
        const boardingHint = document.getElementById("boardingHint");
        const note = document.getElementById("note");

        if(boardingHint && boardingHint.value.trim() !== "") {
            note.value = `[Đưa đón: ${boardingHint.value}] \n` + note.value;
        }
    });
});