// 1. CẤU HÌNH MÀU SẮC (BẮT BUỘC ĐỂ HIỆN MÀU CAM)
tailwind.config = {
    theme: {
        extend: {
            fontFamily: {
                sans: ['Nunito', 'sans-serif'],
            },
            colors: {
                primary: '#FFA54F',      // <-- Màu cam chủ đạo của bạn
                primaryHover: '#FF8C1A', // Màu hover đậm hơn
                secondary: '#f97316', 
                bgLight: '#f3f4f6',
            }
        }
    }
}

// 2. CÁC HÀM XỬ LÝ (Giữ nguyên logic gốc)

// Hàm chuyển Tab (Dùng cho Detail/History)
function switchTab(tabId) {
    document.getElementById('tab-info').classList.add('hidden');
    document.getElementById('tab-history').classList.add('hidden');
    document.getElementById('btn-tab-info').classList.remove('active', 'text-primary', 'border-b-2');
    document.getElementById('btn-tab-history').classList.remove('active', 'text-primary', 'border-b-2');

    document.getElementById(tabId).classList.remove('hidden');
    document.getElementById('btn-' + tabId).classList.add('active');
}

// Hàm đổi loại form (Khách/Shop)
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