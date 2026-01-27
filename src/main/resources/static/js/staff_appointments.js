const btnApply = document.getElementById("btnApply");
const btnReset = document.getElementById("btnReset");

function normalize(s) { return (s || "").toLowerCase().trim(); }

function applyFilters() {
    // Logic filter của bạn giữ nguyên
    const status = normalize(document.getElementById("status").value);
    const keyword = normalize(document.getElementById("keyword").value);
    // ... code tiếp ...
}

if(btnApply) btnApply.addEventListener("click", applyFilters);
if(btnReset) btnReset.addEventListener("click", () => {
    document.querySelectorAll(".item").forEach(i => i.style.display = "flex");
    const empty = document.getElementById("emptyState");
    if (empty) empty.style.display = "none";
});