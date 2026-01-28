document.addEventListener("DOMContentLoaded", function() {
    const stars = Array.from(document.querySelectorAll(".star"));
    const ratingVal = document.getElementById("ratingVal");
    const ratingHint = document.getElementById("ratingHint");

    function paint(v){
        stars.forEach(s => {
            const sv = Number(s.dataset.v);
            s.classList.toggle("active", sv <= v);
        });
        ratingHint.textContent = v ? ("Selected rating: " + v + " / 5") : "You must pick a rating to submit.";
    }

    if (stars.length > 0) {
        stars.forEach(s => {
            s.addEventListener("click", () => {
                const v = Number(s.dataset.v);
                ratingVal.value = String(v);
                paint(v);
            });
        });

        // Initialize with existing value (e.g. if validation failed)
        paint(Number(ratingVal.value || 0));
    }

    const form = document.getElementById("reviewForm");
    if(form) {
        form.addEventListener("submit", (e) => {
            const r = Number(ratingVal.value || 0);
            if(!(r >= 1 && r <= 5)){
                e.preventDefault();
                alert("Please choose a rating from one to five.");
            }
        });
    }
});