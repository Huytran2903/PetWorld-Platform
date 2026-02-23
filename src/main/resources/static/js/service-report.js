document.addEventListener("DOMContentLoaded", function() {

    // 1. Set default time if input is empty
    const dateInput = document.getElementById("completedAt");
    if (dateInput && !dateInput.value) {
        const now = new Date();
        const pad = (n) => String(n).padStart(2, "0");
        const local = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
        dateInput.value = local;
    }

    // 2. Image Preview Logic
    const input = document.getElementById("evidenceInput");
    const thumbs = document.getElementById("thumbs");

    if(input && thumbs) {
        input.addEventListener("change", () => {
            thumbs.innerHTML = "";
            const files = Array.from(input.files || []).slice(0, 6); // Limit preview to 6

            if (files.length === 0) {
                thumbs.innerHTML = `<div class="thumb">Preview</div><div class="thumb">Preview</div><div class="thumb">Preview</div>`;
                return;
            }

            files.forEach(file => {
                const div = document.createElement("div");
                div.className = "thumb";

                const img = document.createElement("img");
                img.alt = file.name;

                div.appendChild(img);
                thumbs.appendChild(div);

                const reader = new FileReader();
                reader.onload = (e) => img.src = e.target.result;
                reader.readAsDataURL(file);
            });
        });
    }
});