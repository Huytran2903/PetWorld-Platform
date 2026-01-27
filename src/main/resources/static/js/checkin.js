document.addEventListener("DOMContentLoaded", function() {
    const tabs = document.querySelectorAll(".tab");
    const panels = {
        health: document.getElementById("panel-health"),
        vacc: document.getElementById("panel-vacc"),
        profile: document.getElementById("panel-profile")
    };

    if(tabs.length > 0) {
        tabs.forEach(t => {
            t.addEventListener("click", () => {
                // Deactivate all
                tabs.forEach(x => x.classList.remove("active"));
                // Activate clicked
                t.classList.add("active");

                const key = t.getAttribute("data-tab");
                // Show/Hide panels
                Object.keys(panels).forEach(k => {
                    if(panels[k]) {
                        panels[k].style.display = (k === key ? "block" : "none");
                    }
                });
            });
        });
    }
});