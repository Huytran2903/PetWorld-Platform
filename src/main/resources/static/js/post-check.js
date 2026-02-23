document.addEventListener("DOMContentLoaded", function() {
    // Optional: Auto-scroll to top if returning from a save
    if (location.search.includes("success")) {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
});