document.addEventListener("DOMContentLoaded", function() {
    const LEAD_HOURS = 2;
    const MAX_ADVANCE_DAYS = 30;
    /** Match BookingService: OPEN_TIME / CLOSE_TIME (end must be on or before 20:00 same day unless boarding crosses midnight). */
    const CLOSE_MINUTES_OF_DAY = 20 * 60;
    const form = document.getElementById("bookForm");
    const sumServices = document.getElementById("sumServices");
    const sumDuration = document.getElementById("sumDuration");
    const sumTotal = document.getElementById("sumTotal");
    const summaryEmptyState = document.getElementById("summaryEmptyState");
    const summaryMetrics = document.getElementById("summaryMetrics");
    const bookingSummaryAside = document.getElementById("bookingSummaryAside");
    const petSelect = document.getElementById("petId");
    const appointmentDateInput = document.getElementById("appointmentDate");
    const petErrorEl = document.getElementById("booking-pet-error");
    const dateTimeErrorEl = document.getElementById("booking-datetime-error");
    const servicesErrorEl = document.getElementById("booking-services-error");

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
        if (servicesErrorEl) {
            servicesErrorEl.textContent = "";
            servicesErrorEl.classList.remove("is-visible");
        }
    }

    function hideBookingIssuesDialog() {
        const el = document.getElementById("booking-issues-modal");
        if (!el) return;
        el.classList.remove("is-open");
        document.body.classList.remove("booking-modal-open");
        document.removeEventListener("keydown", onBookingIssuesEscape);
    }

    function onBookingIssuesEscape(ev) {
        if (ev.key === "Escape") hideBookingIssuesDialog();
    }

    const BOOKING_ALERT_ICON_SVG =
        '<svg class="booking-alert-icon-svg" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">' +
        '<path d="M12 9v4M12 17h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
        '<path d="M10.3 3.6h3.4l7.8 13.5a1.6 1.6 0 0 1-1.4 2.4H4.9a1.6 1.6 0 0 1-1.4-2.4L10.3 3.6z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>' +
        "</svg>";

    /**
     * Polished alert dialog for validation / vaccine messages (English copy matches server).
     */
    function showBookingIssuesDialog(title, messages) {
        const list = (Array.isArray(messages) ? messages : [messages])
            .map((m) => String(m || "").trim())
            .filter(Boolean);
        if (!list.length) return;

        let backdrop = document.getElementById("booking-issues-modal");
        if (!backdrop) {
            backdrop = document.createElement("div");
            backdrop.id = "booking-issues-modal";
            backdrop.className = "booking-modal-backdrop booking-alert-overlay";
            backdrop.setAttribute("role", "alertdialog");
            backdrop.setAttribute("aria-modal", "true");
            backdrop.setAttribute("aria-labelledby", "booking-issues-modal-title");

            const panel = document.createElement("div");
            panel.className = "booking-alert-dialog";

            const header = document.createElement("header");
            header.className = "booking-alert-header";

            const iconWrap = document.createElement("div");
            iconWrap.className = "booking-alert-icon-ring";
            iconWrap.setAttribute("aria-hidden", "true");
            iconWrap.innerHTML = BOOKING_ALERT_ICON_SVG;

            const titleEl = document.createElement("h2");
            titleEl.id = "booking-issues-modal-title";
            titleEl.className = "booking-alert-title";

            const titleTextWrap = document.createElement("div");
            titleTextWrap.className = "booking-alert-title-wrap";
            titleTextWrap.appendChild(titleEl);

            header.appendChild(iconWrap);
            header.appendChild(titleTextWrap);

            const body = document.createElement("div");
            body.className = "booking-alert-body";
            body.id = "booking-issues-modal-body";

            const lead = document.createElement("p");
            lead.className = "booking-alert-lead";
            lead.id = "booking-issues-modal-lead";

            const ul = document.createElement("ul");
            ul.className = "booking-alert-list";
            ul.id = "booking-issues-modal-list";

            body.appendChild(lead);
            body.appendChild(ul);

            const footer = document.createElement("footer");
            footer.className = "booking-alert-footer";

            const okBtn = document.createElement("button");
            okBtn.type = "button";
            okBtn.className = "booking-alert-btn booking-alert-btn--primary";
            okBtn.textContent = "OK";

            footer.appendChild(okBtn);
            panel.appendChild(header);
            panel.appendChild(body);
            panel.appendChild(footer);
            backdrop.appendChild(panel);
            document.body.appendChild(backdrop);

            okBtn.addEventListener("click", hideBookingIssuesDialog);
            backdrop.addEventListener("click", (e) => {
                if (e.target === backdrop) hideBookingIssuesDialog();
            });
            panel.addEventListener("click", (e) => e.stopPropagation());
        }

        const titleHeading = backdrop.querySelector("#booking-issues-modal-title");
        if (titleHeading) titleHeading.textContent = title || "Cannot complete booking";

        const leadEl = backdrop.querySelector("#booking-issues-modal-lead");
        if (leadEl) {
            leadEl.textContent =
                list.length === 1
                    ? "The following issue needs your attention:"
                    : `Please fix the following ${list.length} issues:`;
        }

        const listEl = backdrop.querySelector("#booking-issues-modal-list");
        if (listEl) {
            listEl.innerHTML = "";
            list.forEach((text) => {
                const li = document.createElement("li");
                li.className = "booking-alert-list-item";
                li.textContent = text;
                listEl.appendChild(li);
            });
        }

        backdrop.classList.add("is-open");
        document.body.classList.add("booking-modal-open");
        document.removeEventListener("keydown", onBookingIssuesEscape);
        document.addEventListener("keydown", onBookingIssuesEscape);
        backdrop.querySelector(".booking-alert-btn--primary")?.focus();
    }

    /**
     * Validation order: pet → date/time (all applicable rules) → services → boarding rules → duration / closing.
     */
    function collectOrderedBookingIssues() {
        const items = [];

        if (!(petSelect?.value || "").trim()) {
            items.push({ field: "pet", message: "Please select your pet." });
        }

        const dateVal = (appointmentDateInput?.value || "").trim();
        if (!dateVal) {
            items.push({ field: "datetime", message: "Please choose an appointment date and time." });
        } else {
            const selected = new Date(appointmentDateInput.value);
            if (Number.isNaN(selected.getTime())) {
                items.push({ field: "datetime", message: "Please enter a valid date and time." });
            } else {
                const now = new Date();
                now.setSeconds(0, 0);
                selected.setSeconds(0, 0);
                if (selected < now) {
                    items.push({ field: "datetime", message: "The selected date and time cannot be in the past." });
                }
                const minDate = new Date(now.getTime() + LEAD_HOURS * 60 * 60 * 1000);
                const maxDate = new Date(now.getTime() + MAX_ADVANCE_DAYS * 24 * 60 * 60 * 1000);
                if (selected < minDate) {
                    items.push({ field: "datetime", message: "Booking must be at least 2 hours in advance." });
                }
                if (selected > maxDate) {
                    items.push({ field: "datetime", message: "Booking can only be made up to 30 days in advance." });
                }
                const minutesOfDay = selected.getHours() * 60 + selected.getMinutes();
                if (minutesOfDay < 8 * 60 || minutesOfDay >= 20 * 60) {
                    items.push({ field: "datetime", message: "Selected time is outside operating hours (08:00 – 20:00)." });
                }
            }
        }

        if (!form.querySelector("input[name='mainServices']:checked")) {
            items.push({ field: "services", message: "Please select at least one service." });
        }

        const counts = getCheckedServiceLineCounts();
        if (counts.boarding > 0 && counts.nonBoarding > 0) {
            items.push({
                field: "services",
                message:
                    "Boarding cannot be combined with other services in one appointment. Book boarding separately, or uncheck boarding / other services.",
            });
        }
        if (counts.boarding > 1) {
            items.push({ field: "services", message: "Please select only one boarding package per appointment." });
        }

        const boardingRulesInvalid = (counts.boarding > 0 && counts.nonBoarding > 0) || counts.boarding > 1;
        const dateVal2 = (appointmentDateInput?.value || "").trim();
        const start = dateVal2 ? new Date(appointmentDateInput.value) : null;
        if (!boardingRulesInvalid && start && !Number.isNaN(start.getTime())) {
            const { totalMinutes, allowMultiDayStay } = getBookingSelectionTotals();
            if (totalMinutes > 0) {
                const fit = durationFitsOperatingHours(start, totalMinutes, allowMultiDayStay);
                if (!fit.ok) {
                    items.push({ field: "datetime", message: fit.message });
                }
            }
        }

        return items;
    }

    function focusFirstBookingIssue(items) {
        if (!items || !items.length) return;
        clearBookingFieldErrors();
        const f = items[0].field;
        if (f === "pet") {
            petSelect?.classList.add("booking-input-invalid");
            petSelect?.focus();
            petSelect?.scrollIntoView({ behavior: "smooth", block: "center" });
        } else if (f === "datetime") {
            appointmentDateInput?.classList.add("booking-input-invalid");
            appointmentDateInput?.focus();
            appointmentDateInput?.scrollIntoView({ behavior: "smooth", block: "center" });
        } else if (f === "services") {
            form.querySelector(".section")?.scrollIntoView({ behavior: "smooth", block: "center" });
        }
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
    function hideVaccineLockPopup() {
        const backdrop = document.getElementById("booking-vaccine-modal");
        if (!backdrop) return;
        backdrop.classList.remove("is-open");
        document.body.classList.remove("booking-modal-open");
        document.removeEventListener("keydown", onVaccineModalEscape);
    }

    function onVaccineModalEscape(ev) {
        if (ev.key === "Escape") hideVaccineLockPopup();
    }

    /** Custom English popup for locked vaccines (no browser alert). */
    function showVaccineLockPopup(message) {
        const text = (message || "").trim();
        if (!text) return;

        let backdrop = document.getElementById("booking-vaccine-modal");
        if (!backdrop) {
            backdrop = document.createElement("div");
            backdrop.id = "booking-vaccine-modal";
            backdrop.className = "booking-modal-backdrop";
            backdrop.setAttribute("role", "dialog");
            backdrop.setAttribute("aria-modal", "true");
            backdrop.setAttribute("aria-labelledby", "booking-vaccine-modal-title");

            const dialog = document.createElement("div");
            dialog.className = "booking-modal-dialog";

            const title = document.createElement("h3");
            title.id = "booking-vaccine-modal-title";
            title.className = "booking-modal-title";
            title.textContent = "Vaccination not available";

            const body = document.createElement("p");
            body.className = "booking-modal-body";
            body.id = "booking-vaccine-modal-body";

            const actions = document.createElement("div");
            actions.className = "booking-modal-actions";

            const okBtn = document.createElement("button");
            okBtn.type = "button";
            okBtn.className = "booking-modal-btn booking-modal-btn--primary";
            okBtn.textContent = "OK";

            actions.appendChild(okBtn);
            dialog.appendChild(title);
            dialog.appendChild(body);
            dialog.appendChild(actions);
            backdrop.appendChild(dialog);
            document.body.appendChild(backdrop);

            okBtn.addEventListener("click", hideVaccineLockPopup);
            backdrop.addEventListener("click", (e) => {
                if (e.target === backdrop) hideVaccineLockPopup();
            });
            dialog.addEventListener("click", (e) => e.stopPropagation());
        }

        const bodyEl = backdrop.querySelector("#booking-vaccine-modal-body");
        if (bodyEl) bodyEl.textContent = text;

        backdrop.classList.add("is-open");
        document.body.classList.add("booking-modal-open");
        document.removeEventListener("keydown", onVaccineModalEscape);
        document.addEventListener("keydown", onVaccineModalEscape);
        backdrop.querySelector(".booking-modal-btn--primary")?.focus();
    }

    function setEligibilityBanner(message) {
        const t = message != null ? String(message).trim() : "";
        if (!t) return;
        showBookingIssuesDialog("Vaccine eligibility", [t]);
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
        // Align to minute precision so "exactly 2 hours ahead" isn't rejected due to seconds/millis.
        now.setSeconds(0, 0);
        const minDate = new Date(now.getTime() + LEAD_HOURS * 60 * 60 * 1000);
        const maxDate = new Date(now.getTime() + MAX_ADVANCE_DAYS * 24 * 60 * 60 * 1000);
        appointmentDateInput.min = toDateTimeLocalValue(minDate);
        appointmentDateInput.max = toDateTimeLocalValue(maxDate);
    }

    function isBoardingServiceInput(inp) {
        const raw = inp.getAttribute("data-duration");
        let duration = 30;
        if (raw != null && String(raw).trim() !== "") {
            const n = parseFloat(raw);
            duration = Number.isFinite(n) ? n : 30;
        }
        const typeName = (inp.getAttribute("data-type") || "").trim().toLowerCase();
        return typeName === "boarding" || duration >= 1440;
    }

    function getCheckedServiceLineCounts() {
        let boarding = 0;
        let nonBoarding = 0;
        form.querySelectorAll("input[name='mainServices']").forEach(inp => {
            if (!inp.checked) return;
            if (isBoardingServiceInput(inp)) boarding++;
            else nonBoarding++;
        });
        return { boarding, nonBoarding };
    }

    /**
     * Mirrors BookingService line totals: duration null/empty -> 30 min.
     * allowMultiDayStay = boarding-only selection (exactly one boarding line after combo rules).
     */
    function getBookingSelectionTotals() {
        const inputs = form.querySelectorAll("input[name='mainServices']");
        const qtyInput = document.getElementById("boardingQty");
        const hasBoardingQtyBlock = !!document.getElementById("boardingQtyBlock");
        let qty = 1;
        if (qtyInput) qty = parseInt(qtyInput.value, 10) || 1;

        let total = 0;
        let totalMinutes = 0;
        const names = [];
        const { boarding, nonBoarding } = getCheckedServiceLineCounts();
        const allowMultiDayStay = boarding > 0 && nonBoarding === 0;

        inputs.forEach(inp => {
            if (!inp.checked) return;
            const price = parseFloat(inp.getAttribute("data-price")) || 0;
            const raw = inp.getAttribute("data-duration");
            let duration = 30;
            if (raw != null && String(raw).trim() !== "") {
                const n = parseFloat(raw);
                duration = Number.isFinite(n) ? n : 30;
            }

            if (hasBoardingQtyBlock) {
                total += price * qty;
                totalMinutes += duration * qty;
            } else {
                total += price;
                totalMinutes += duration;
            }
            names.push(inp.getAttribute("data-name"));
        });

        return { total, totalMinutes, allowMultiDayStay, names, boarding, nonBoarding };
    }

    /**
     * Aligns with BookingService.validateOperatingHoursRange (same-day end after 20:00; cross-midnight only for boarding-only).
     */
    function durationFitsOperatingHours(start, totalMinutes, allowMultiDayStay) {
        if (!totalMinutes || totalMinutes <= 0) {
            return { ok: true };
        }
        const end = new Date(start.getTime() + totalMinutes * 60 * 1000);
        if (start.toDateString() !== end.toDateString()) {
            if (!allowMultiDayStay) {
                return {
                    ok: false,
                    message: "These services would run past midnight. Non-boarding visits must finish the same day — shorten the package or split the booking.",
                };
            }
            return { ok: true };
        }
        const endMod = end.getHours() * 60 + end.getMinutes();
        if (endMod > CLOSE_MINUTES_OF_DAY) {
            return {
                ok: false,
                message: "Selected services would end after closing time (20:00). Pick an earlier start time or fewer services.",
            };
        }
        return { ok: true };
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
        hideVaccineLockPopup();

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
                    "Could not load vaccine eligibility (network issue or your session expired). Vaccine options are temporarily locked — please reload the page or sign in again."
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
                "Could not load vaccine eligibility. Vaccine options are temporarily locked — please reload the page."
            );
            calculateTotal();
        }
    }

    // Hàm tính toán
    function calculateTotal() {
        const { total, totalMinutes: totalDuration, names } = getBookingSelectionTotals();
        const hasSelection = names.length > 0;

        if (bookingSummaryAside) {
            bookingSummaryAside.dataset.empty = hasSelection ? "false" : "true";
        }
        if (summaryEmptyState) {
            summaryEmptyState.hidden = hasSelection;
        }
        if (summaryMetrics) {
            summaryMetrics.hidden = !hasSelection;
        }

        if (sumServices) {
            sumServices.textContent = hasSelection ? names.join(", ") : "";
        }
        if (sumTotal) {
            sumTotal.textContent = hasSelection ? formatVND(total) : "";
        }

        if (sumDuration) {
            if (totalDuration > 0) {
                const hours = Math.floor(totalDuration / 60);
                const minutes = totalDuration % 60;
                let timeStr = "";
                if (hours > 0) timeStr += `${hours} ${hours === 1 ? "hour" : "hours"} `;
                if (minutes > 0) timeStr += `${minutes} ${minutes === 1 ? "minute" : "minutes"}`;
                sumDuration.textContent = timeStr.trim();
            } else {
                sumDuration.textContent = "";
            }
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
        showVaccineLockPopup(msg);
    });

    // Xử lý trước khi submit (nếu cần gộp note)
    form.addEventListener("submit", (e) => {
        const issues = collectOrderedBookingIssues();
        if (issues.length) {
            e.preventDefault();
            showBookingIssuesDialog(
                "Cannot complete booking",
                issues.map((i) => i.message)
            );
            focusFirstBookingIssue(issues);
            return;
        }

        clearBookingFieldErrors();

        const boardingHint = document.getElementById("boardingHint");
        const note = document.getElementById("note");

        if(boardingHint && boardingHint.value.trim() !== "") {
            note.value = `[Đưa đón: ${boardingHint.value}] \n` + note.value;
        }
    });
});