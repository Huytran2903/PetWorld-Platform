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

import java.math.BigDecimal;
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

            newItem.setAddedAt(LocalDateTime.now());

            cartItemRepo.save(newItem);
        }
    }

    public void removeCartItem(Integer cartItemId) {
        cartItemRepo.deleteById(cartItemId);
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

    public void updateQuantity(Integer cartItemId, String action) {
        // 1. Tìm món hàng trong giỏ
        CartItem item = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Món hàng không tồn tại trong giỏ!"));

        // 2. Xử lý tăng số lượng (Increase)
        if ("increase".equals(action)) {
            // Kiểm tra nếu là Sản phẩm (Product) mới cho phép tăng
            if (item.getProduct() != null) {
                int currentStock = item.getProduct().getStock();
                if (item.getQuantity() < currentStock) {
                    item.setQuantity(item.getQuantity() + 1);
                } else {
                    // Ném lỗi nếu vượt quá số lượng trong kho
                    throw new RuntimeException("Sorry, only " + currentStock + " items are left in stock!");
                }
            }
            // Với Pet, mặc định không cho tăng (số lượng luôn là 1)
        }

        // 3. Xử lý giảm số lượng (Decrease)
        else if ("decrease".equals(action)) {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
            }
        }

        // 4. Lưu lại thay đổi
        cartItemRepo.save(item);
    }


    public int getCountCartItems(Integer customerId) {
        if (customerId == null) return 0;
        return cartItemRepo.countByCart_CustomerId(customerId);
    }

    public BigDecimal calculateSubtotal(Carts cart) {
        // 1. Khởi tạo bằng BigDecimal.ZERO thay vì số 0
        BigDecimal subtotal = BigDecimal.ZERO;

        if (cart != null && cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                BigDecimal unitPrice = BigDecimal.ZERO;

                // 2. Lấy giá Pet (Ưu tiên Sale Price)
                if (item.getPet() != null) {
                    unitPrice = (item.getPet().getSalePrice() != null) ?
                            item.getPet().getSalePrice() : item.getPet().getPrice();
                }
                // 3. Lấy giá Product (Ưu tiên Sale Price)
                else if (item.getProduct() != null) {
                    unitPrice = (item.getProduct().getSalePrice() != null) ?
                            item.getProduct().getSalePrice() : item.getProduct().getPrice();
                }

                // 4. Nhân unitPrice với số lượng (Quantity)
                // Cần chuyển Integer sang BigDecimal bằng BigDecimal.valueOf()
                BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                // 5. Cộng dồn vào tổng (Chú ý: BigDecimal là immutable nên phải gán lại giá trị)
                subtotal = subtotal.add(itemTotal);
            }
        }
        return subtotal;
    }

    @Transactional
    public void clearCart(Integer customerId) {
        // Lấy giỏ hàng hiện tại bằng hàm có sẵn của bạn
        Carts cart = getCartDetail(customerId);

        if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
            // Xóa toàn bộ các món hàng (CartItem) nằm trong giỏ này khỏi Database
            cartItemRepo.deleteAll(cart.getItems());

            // Xóa sạch list trong bộ nhớ và cập nhật lại giỏ hàng
            cart.getItems().clear();
            cartRepo.save(cart);
        }
    }

    public void deleteCartByIdCustomer(Integer customerId) {
        cartRepo.deleteByCustomerId(customerId);
    }
}

