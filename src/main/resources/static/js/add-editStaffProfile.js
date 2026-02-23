document.addEventListener('DOMContentLoaded', function() {
    const fullNameInput = document.getElementById('fullName');
    const avatarPreview = document.getElementById('avatarPreview');

    if (fullNameInput && avatarPreview) {
        fullNameInput.addEventListener('input', function() {
            const name = this.value.trim().toUpperCase();

            if (name.length > 0) {
                const firstChar = name.charAt(0);
                const charCode = name.charCodeAt(0) - 65;
                const hue = (charCode * 137) % 360;

                avatarPreview.textContent = firstChar;
                avatarPreview.style.backgroundColor = `hsl(${hue}, 70%, 60%)`;
                avatarPreview.style.color = 'white';
            } else {
                avatarPreview.textContent = '';
                avatarPreview.style.backgroundColor = '#e9ecef';
                avatarPreview.style.color = '#adb5bd';
            }
        });
    }
});