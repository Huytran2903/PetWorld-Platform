document.addEventListener("DOMContentLoaded", function() {
    const LEAD_HOURS = 2;
    const MAX_ADVANCE_DAYS = 30;
    const form = document.getElementById("bookForm");
    const sumServices = document.getElementById("sumServices");
    const sumDuration = document.getElementById("sumDuration");
    const sumTotal = document.getElementById("sumTotal");
    const petSelect = document.getElementById("petId");
    const appointmentDateInput = document.getElementById("appointmentDate");
    const petErrorEl = document.getElementById("booking-pet-error");
    const dateTimeErrorEl = document.getElementById("booking-datetime-error");

    function clearBookingFieldErrors() {
        [petSelect, appointmentDateInput].forEach((el) => el?.classList.remove("booking-input-invalid"));
        if (petErrorEl) {
            petErrorEl.textContent = "";
            petErrorEl.classList.remove("is-visible");
        }
        if (dateTimeErrorEl) {
            dateTimeErrorEl.textContent = "";
            dateTimeErrorEl.classList.remove("is-visible");
        }
    }

    function showPetError(message) {
        if (petErrorEl) {
            petErrorEl.textContent = message;
            petErrorEl.classList.add("is-visible");
        }
        petSelect?.classList.add("booking-input-invalid");
        petSelect?.focus();
        petSelect?.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    function showDateTimeError(message) {
        if (dateTimeErrorEl) {
            dateTimeErrorEl.textContent = message;
            dateTimeErrorEl.classList.add("is-visible");
        }
        appointmentDateInput?.classList.add("booking-input-invalid");
        appointmentDateInput?.focus();
        appointmentDateInput?.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    /** Latest eligibility locks by service name (lowercase) — used to block race if user clicks before UI updates */
    let vaccineLocksByName = new Map();
    let eligibilityRequestSeq = 0;

    // Format currency (VND, English locale for display)
    function formatVND(n) {
        return new Intl.NumberFormat('en-US', { style: 'decimal', minimumFractionDigits: 0 }).format(n) + " VND";
    }

    function isVaccineCheckbox(inp) {
        const t = (inp.getAttribute("data-type") || "").trim().toLowerCase();
        return t === "vaccine" || t === "vaccination" || t.includes("vaccin");
    }

    /** Chỉ hiện khi API eligibility lỗi; không giữ ô trống khi không có lỗi. */
    function setEligibilityBanner(message) {
        const existing = document.getElementById("vaccine-eligibility-banner");
        if (!message || !String(message).trim()) {
            if (existing) existing.remove();
            return;
        }
        let el = existing;
        if (!el) {
            el = document.createElement("div");
            el.id = "vaccine-eligibility-banner";
            el.className = "booking-flash booking-flash--warn";
            el.setAttribute("role", "alert");
            const grid = form.querySelector(".grid");
            if (grid) grid.after(el);
            else form.prepend(el);
        }
        el.textContent = message.trim();
    }

    function getEffectiveDateISO() {
        const v = (appointmentDateInput?.value || "").trim();
        if (!v) return null;
        // datetime-local returns "YYYY-MM-DDTHH:mm"
        const datePart = v.split("T")[0];
        return datePart || null;
    }

    function toDateTimeLocalValue(date) {
        const adjusted = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
        return adjusted.toISOString().slice(0, 16);
    }

    function setBookingDateBounds() {
        if (!appointmentDateInput) return;
        const now = new Date();
        const minDate = new Date(now.getTime() + LEAD_HOURS * 60 * 60 * 1000);
        const maxDate = new Date(now.getTime() + MAX_ADVANCE_DAYS * 24 * 60 * 60 * 1000);
        appointmentDateInput.min = toDateTimeLocalValue(minDate);
        appointmentDateInput.max = toDateTimeLocalValue(maxDate);
    }

    function clearVaccineLockUi(inp) {
        const row = inp.closest("tr");
        inp.disabled = false;
        inp.removeAttribute("title");
        inp.removeAttribute("data-lock-message");
        inp.removeAttribute("aria-disabled");
        row?.classList.remove("svc-disabled");
        row?.querySelector(".svc-vaccine-lock-hint")?.remove();
    }

    function lockMessageFor(svcName, lock) {
        if (lock.reason === "missing_next_due") {
            return `This pet has already received the "${svcName}" vaccine. The next due date is missing, so it cannot be booked again. Please contact staff.`;
        }
        const nextDue = lock.nextDueDate;
        return `This pet has already received the "${svcName}" vaccine. Next due date: ${nextDue}.`;
    }

    async function refreshVaccineEligibility() {
        if (!form || !petSelect) return;
        const petId = (petSelect.value || "").trim();
        const inputs = form.querySelectorAll("input[name='mainServices']");
        const seq = ++eligibilityRequestSeq;

        vaccineLocksByName = new Map();
        setEligibilityBanner("");

        // reset: clear vaccine disable state + inline hints
        inputs.forEach(inp => {
            if (!isVaccineCheckbox(inp)) return;
            clearVaccineLockUi(inp);
        });

        if (!petId) {
            calculateTotal();
            return;
        }

        // While loading: block all vaccine checkboxes so user cannot tick before API returns
        inputs.forEach(inp => {
            if (!isVaccineCheckbox(inp)) return;
            inp.disabled = true;
        });

        const onDate = getEffectiveDateISO();
        const url = new URL("/api/vaccines/eligibility", window.location.origin);
        url.searchParams.set("petId", petId);
        if (onDate) url.searchParams.set("onDate", onDate);

        try {
            const res = await fetch(url.toString(), {
                credentials: "same-origin",
                headers: { Accept: "application/json" },
            });
            if (seq !== eligibilityRequestSeq) return;

            const ct = (res.headers.get("content-type") || "").toLowerCase();
            if (!res.ok || !ct.includes("application/json")) {
                setEligibilityBanner(
                    "Không tải được trạng thái vaccine (lỗi mạng hoặc phiên đăng nhập). Các mũi vaccine tạm thời bị khóa — vui lòng tải lại trang hoặc đăng nhập lại."
                );
                calculateTotal();
                return;
            }

            const data = await res.json();
            if (seq !== eligibilityRequestSeq) return;

            const locks = Array.isArray(data?.locks) ? data.locks : [];
            const lockedByName = new Map();
            locks.forEach(l => {
                const name = (l?.vaccineName || "").trim();
                if (!name) return;
                lockedByName.set(name.toLowerCase(), l);
            });
            vaccineLocksByName = lockedByName;

            inputs.forEach(inp => {
                if (!isVaccineCheckbox(inp)) return;
                const svcName = (inp.getAttribute("data-name") || "").trim();
                if (!svcName) {
                    inp.disabled = false;
                    return;
                }
                const lock = lockedByName.get(svcName.toLowerCase());
                if (!lock) {
                    inp.disabled = false;
                    return;
                }

                inp.checked = false;
                inp.disabled = true;
                const lockMessage = lockMessageFor(svcName, lock);
                inp.setAttribute("title", lockMessage);
                inp.setAttribute("data-lock-message", lockMessage);
                inp.setAttribute("aria-disabled", "true");

                const row = inp.closest("tr");
                row?.classList.add("svc-disabled");
            });

            calculateTotal();
        } catch (e) {
            if (seq !== eligibilityRequestSeq) return;
            setEligibilityBanner(
                "Không tải được trạng thái vaccine. Các mũi vaccine tạm thời bị khóa — vui lòng tải lại trang."
            );
            calculateTotal();
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
        if (e.target.matches("input[name='mainServices']") && isVaccineCheckbox(e.target) && e.target.checked) {
            const svcName = (e.target.getAttribute("data-name") || "").trim();
            if (svcName && vaccineLocksByName.has(svcName.toLowerCase())) {
                e.target.checked = false;
                calculateTotal();
                return;
            }
        }
        if (e.target.matches("input[name='mainServices'], #boardingQty")) {
            calculateTotal();
        }
    });

    // Tính toán lần đầu khi trang load
    setBookingDateBounds();
    calculateTotal();
    refreshVaccineEligibility();

    // Đổi pet: bỏ chọn vaccine (tránh giữ tick của pet khác), rồi tải eligibility
    petSelect?.addEventListener("change", () => {
        clearBookingFieldErrors();
        form.querySelectorAll("input[name='mainServices']").forEach(inp => {
            if (isVaccineCheckbox(inp)) inp.checked = false;
        });
        calculateTotal();
        refreshVaccineEligibility();
    });
    appointmentDateInput?.addEventListener("change", () => {
        clearBookingFieldErrors();
        refreshVaccineEligibility();
    });
    appointmentDateInput?.addEventListener("input", clearBookingFieldErrors);

    // Vaccine bị khóa: không hiện chữ trong bảng; chỉ báo khi user click vào hàng (checkbox disabled thường không nhận click).
    form.addEventListener("click", (e) => {
        const row = e.target.closest?.("tr.svc-row.svc-disabled");
        if (!row || !form.contains(row)) return;
        const cb = row.querySelector("input[name='mainServices']");
        if (!cb || !isVaccineCheckbox(cb) || !cb.disabled) return;
        const msg = cb.getAttribute("data-lock-message") || cb.getAttribute("title");
        if (!msg) return;
        e.preventDefault();
        alert(msg);
    });

    // Xử lý trước khi submit (nếu cần gộp note)
    form.addEventListener("submit", (e) => {
        clearBookingFieldErrors();

        const petIdVal = (petSelect?.value || "").trim();
        if (!petIdVal) {
            e.preventDefault();
            showPetError("Please select your pet.");
            return;
        }

        const dateVal = (appointmentDateInput?.value || "").trim();
        if (!dateVal) {
            e.preventDefault();
            showDateTimeError("Please choose an appointment date and time.");
            return;
        }

        const selected = new Date(appointmentDateInput.value);
        if (Number.isNaN(selected.getTime())) {
            e.preventDefault();
            showDateTimeError("Please enter a valid date and time.");
            return;
        }

        const now = new Date();
        const minDate = new Date(now.getTime() + LEAD_HOURS * 60 * 60 * 1000);
        const maxDate = new Date(now.getTime() + MAX_ADVANCE_DAYS * 24 * 60 * 60 * 1000);
        if (selected < minDate) {
            e.preventDefault();
            showDateTimeError("Booking must be at least 2 hours in advance.");
            return;
        }
        if (selected > maxDate) {
            e.preventDefault();
            showDateTimeError("Booking can only be made up to 30 days in advance.");
            return;
        }

        const hour = selected.getHours();
        const minute = selected.getMinutes();
        const minutesOfDay = hour * 60 + minute;
        const openMinutes = 8 * 60;
        const closeMinutes = 20 * 60;
        if (minutesOfDay < openMinutes || minutesOfDay >= closeMinutes) {
            e.preventDefault();
            showDateTimeError("Selected time is outside operating hours (08:00 – 20:00).");
            return;
        }

        const boardingHint = document.getElementById("boardingHint");
        const note = document.getElementById("note");

        if(boardingHint && boardingHint.value.trim() !== "") {
            note.value = `[Đưa đón: ${boardingHint.value}] \n` + note.value;
        }
    });
});