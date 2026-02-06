tailwind.config = {
    theme: {
        extend: {
            fontFamily: {
                sans: ['Nunito', 'sans-serif'],
            },
            colors: {
                primary: '#FFA54F',
                primaryHover: '#FF8C1A',
                secondary: '#f97316', 
                bgLight: '#f3f4f6',
            }
        }
    }
}

function switchTab(tabId) {
    document.getElementById('tab-info').classList.add('hidden');
    document.getElementById('tab-history').classList.add('hidden');
    document.getElementById('btn-tab-info').classList.remove('active', 'text-primary', 'border-b-2');
    document.getElementById('btn-tab-history').classList.remove('active', 'text-primary', 'border-b-2');

    document.getElementById(tabId).classList.remove('hidden');
    document.getElementById('btn-' + tabId).classList.add('active');
}

function toggleCreatePetType() {
    const type = document.querySelector('input[name="createPetOwnerType"]:checked').value;
    const fieldOwner = document.getElementById('create-field-owner');
    const fieldPrice = document.getElementById('create-field-price');

    if (type === 'customer') {
        fieldOwner.classList.remove('hidden');
        fieldPrice.classList.add('hidden');
    } else {
        fieldOwner.classList.add('hidden');
        fieldPrice.classList.remove('hidden');
    }
}

function switchView(viewName) {
    const shopView = document.getElementById('view-shop');
    const customerView = document.getElementById('view-customer');
    const btnShop = document.getElementById('btn-shop');
    const btnCustomer = document.getElementById('btn-customer');

    if (viewName === 'shop') {

        shopView.classList.remove('hidden');
        customerView.classList.add('hidden');

        btnShop.classList.add('active-tab');
        btnShop.classList.remove('inactive-tab');

        btnCustomer.classList.remove('active-tab');
        btnCustomer.classList.add('inactive-tab');
    } else {
        shopView.classList.add('hidden');
        customerView.classList.remove('hidden');

        btnShop.classList.remove('active-tab');
        btnShop.classList.add('inactive-tab');

        btnCustomer.classList.add('active-tab');
        btnCustomer.classList.remove('inactive-tab');
    }
}

function previewImage(input) {
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        reader.onload = function (e) {
            // Tìm thẻ img có id="imagePreview" để thay đổi src
            var imgElement = document.getElementById('imagePreview');
            if (imgElement) {
                imgElement.src = e.target.result;
            }
        }
        reader.readAsDataURL(input.files[0]);
    }
}