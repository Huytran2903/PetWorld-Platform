document.addEventListener("DOMContentLoaded", function() {

    // 1. Set default time for the "Updated At" field
    const dateInput = document.getElementById("updatedAt");
    if (dateInput) {
        const now = new Date();
        const pad = (n) => String(n).padStart(2, "0");
        const local = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
        dateInput.value = local;
    }

    // 2. Optional: Pre-select "Done" if status is "In Progress"
    const statusSelect = document.getElementById("statusSelect");
    if(statusSelect && statusSelect.value === "in_progress") {
        // Automatically suggest "Done" as the next step
        statusSelect.value = "done";
    }
});