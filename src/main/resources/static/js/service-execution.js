document.addEventListener("DOMContentLoaded", function() {
    const spa = document.getElementById("panel-spa");
    const vac = document.getElementById("panel-vac");
    const board = document.getElementById("panel-board");
    const pill = document.getElementById("typePill");
    const chips = document.querySelectorAll(".chip");

    function setType(t){
        if(spa) spa.classList.add("hidden");
        if(vac) vac.classList.add("hidden");
        if(board) board.classList.add("hidden");

        chips.forEach(c => c.classList.remove("active"));

        if (t === "vaccination"){
            if(vac) vac.classList.remove("hidden");
            if(pill) pill.textContent = "VACCINATION";
            const c = document.querySelector(`.chip[data-type="vaccination"]`);
            if(c) c.classList.add("active");
        } else if (t === "boarding"){
            if(board) board.classList.remove("hidden");
            if(pill) pill.textContent = "BOARDING";
            const c = document.querySelector(`.chip[data-type="boarding"]`);
            if(c) c.classList.add("active");
        } else {
            if(spa) spa.classList.remove("hidden");
            if(pill) pill.textContent = "SPA";
            const c = document.querySelector(`.chip[data-type="hygiene"]`);
            if(c) c.classList.add("active");
        }
    }

    chips.forEach(chip => {
        chip.addEventListener("click", () => {
            const t = chip.getAttribute("data-type");
            setType(t);
        });
    });

    // Default to first available or hygiene
    const initialType = document.querySelector(".chip")?.getAttribute("data-type") || "hygiene";
    setType(initialType);
});