package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.CartItem;
import vn.edu.fpt.petworldplatform.entity.Carts;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.entity.Product;
import vn.edu.fpt.petworldplatform.repository.CartItemRepository;
import vn.edu.fpt.petworldplatform.repository.CartRepo;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private CartItemRepository cartItemRepo;


    //Thêm pet và product vào giỏ hàng - OanhTP
    @Transactional
    public void addToCart(Integer customerId, Pets petId, Product productId, Integer quantity) {


        Carts cart = cartRepo.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Carts newCart = new Carts();
                    newCart.setCustomerId(customerId);
                    newCart.setCreatedAt(LocalDateTime.now());
                    return cartRepo.save(newCart);
                });

        // 2. Kiểm tra loại hàng để xử lý logic tương ứng
        if (petId != null) {
            addPetToCart(cart, petId);
        } else if (productId != null) {
            addProductToCart(cart, productId, quantity);
        }
    }

    // Logic xử lý riêng cho Pet
    public void addPetToCart(Carts cart, Pets pet) { // Đổi tên tham số từ petId thành pet
        // Kiểm tra xem Pet này đã có trong giỏ chưa (Gọi đúng tên hàm Repo theo Cách 1)
        Optional<CartItem> existingItem = cartItemRepo.findByCart_CartIdAndPet(cart.getCartId(), pet);

        if (existingItem.isEmpty()) {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setPet(pet); // Set đối tượng pet
            newItem.setProduct(null); // Đảm bảo đúng ràng buộc DB
            newItem.setQuantity(1);   // Thú cưng luôn là 1

            // Bạn có thể bỏ dòng setAddedAt này vì @PrePersist trong CartItem đã tự động làm việc này rồi!
            newItem.setAddedAt(LocalDateTime.now());

            cartItemRepo.save(newItem);
        }
    }

    // Logic xử lý riêng cho Product
    public void addProductToCart(Carts cart, Product product, Integer quantity) { // Đổi productId thành product
        // Kiểm tra xem Product này đã có trong giỏ chưa (Gọi đúng tên hàm Repo theo Cách 1)
        Optional<CartItem> existingItem = cartItemRepo.findByCart_CartIdAndProduct(cart.getCartId(), product);

        if (existingItem.isPresent()) {
            // Nếu có rồi thì "mở hộp" lấy object và cộng dồn số lượng
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepo.save(item);
        } else {
            // Nếu chưa có thì tạo mới
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product); // Set đối tượng product
            newItem.setPet(null);
            newItem.setQuantity(quantity);
            newItem.setAddedAt(LocalDateTime.now());

            cartItemRepo.save(newItem);
        }
    }

    // Hàm lấy giỏ hàng để hiển thị (nếu cần)
    public Optional<Carts> getCartByCustomer(Integer customerId) {
        return cartRepo.findByCustomerId(customerId);
    }

    /**
     * Lấy chi tiết giỏ hàng kèm theo danh sách các món hàng (CartItems)
     */
    public Carts getCartDetail(Integer customerId) {
        // Tìm giỏ hàng theo CustomerID
        return cartRepo.findByCustomerId(customerId)
                .orElseGet(() -> {
                    // Nếu chưa có giỏ hàng, trả về một đối tượng Cart mới (rỗng)
                    // để tránh lỗi NullPointerException ở View
                    Carts emptyCart = new Carts();
                    emptyCart.setCustomerId(customerId);
                    return emptyCart;
                });
    }

    }

