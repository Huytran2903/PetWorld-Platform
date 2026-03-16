package vn.edu.fpt.petworldplatform.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OrderItemRepo orderItemRepo;

    @Autowired
    private CartItemRepository cartItemRepo;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private PaymentRepository paymentRepo;
    @Autowired
    private CartRepo cartRepo;

    @Transactional
    public Order createOrder(Integer customerId, String name, String phone, String addr, String note, String method) {
        // BƯỚC 1: Lấy Giỏ hàng (Cart) của khách hàng
        Carts cart = cartRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại!"));

        // Lấy danh sách món hàng từ đối tượng Cart
        List<CartItem> itemsInCart = cart.getItems();
        if (itemsInCart == null || itemsInCart.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống!");
        }

        // BƯỚC 2: Khởi tạo Orders (Lưu thông tin giao hàng)
        Order order = new Order();
        order.setCustomerID(customerId); // Gán ID khách hàng
        order.setOrderCode("PET-" + System.currentTimeMillis()); // Tạo mã duy nhất
        order.setShipName(name);
        order.setShipPhone(phone);
        order.setShipAddress(addr);
        order.setNote(note);
        order.setStatus("pending"); // Dùng chữ thường khớp với CHECK constraint
        order.setCreatedAt(LocalDateTime.now());

        // Tính toán tiền tệ (Subtotal)
        BigDecimal subtotal = calculateSubtotal(itemsInCart);
        order.setSubtotal(subtotal);

        // ---- ĐÃ SỬA: GÁN CỨNG PHÍ SHIP 25K VÀ BỎ THUẾ ----
        BigDecimal shippingFee = new BigDecimal("25000");
        order.setShippingFee(shippingFee); // Lưu thẳng 25k vào Database

        // Tổng cộng = Tiền hàng + Phí ship 25k (Không tính thuế nữa)
        order.setTotalAmount(subtotal.add(shippingFee));
        // -------------------------------------------------------------

        // Lưu để có OrderID làm khóa ngoại cho OrderItems
        order = orderRepo.save(order);

        List<OrderItems> listOrderItems = new ArrayList<>();
        // BƯỚC 3: Chuyển từng CartItem sang OrderItem (Snapshot dữ liệu)
        for (CartItem ci : itemsInCart) {
            OrderItems oi = new OrderItems();
            oi.setOrder(order); // FK_OrderItems_Orders

            if (ci.getProduct() != null) {
                oi.setProductID(ci.getProduct().getProductId());
                oi.setItemName(ci.getProduct().getName());
                oi.setUnitPrice(ci.getProduct().getSalePrice());

                // Cập nhật tồn kho
                Product p = ci.getProduct();
                if (p.getStock() < ci.getQuantity()) {
                    throw new RuntimeException("Sản phẩm " + p.getName() + " không đủ hàng!");
                }
                p.setStock(p.getStock() - ci.getQuantity());
                productRepo.save(p);
            } else if (ci.getPet() != null) {
                oi.setPetID(ci.getPet().getPetID());
                oi.setItemName(ci.getPet().getName());
                oi.setUnitPrice(ci.getPet().getSalePrice());

                Pets pet = ci.getPet();
                Customer owner = customerRepo.findById(customerId).get();
                pet.setOwner(owner);
                pet.setIsAvailable(false);
            }

            oi.setQuantity(ci.getQuantity());
            // LineTotal tự tính trong SQL (Computed Column) nên không cần set ở đây
            orderItemRepo.save(oi);

            // Thêm đối tượng oi vào danh sách
            listOrderItems.add(oi);
        }

        // Cập nhật lại danh sách OrderItems cho order
        order.setOrderItems(listOrderItems);

        // BƯỚC 4: Tạo bản ghi thanh toán
        Payment payment = new Payment();
        payment.setOrder(order); // Gán Order để nối khóa ngoại FK_Payments_Orders
        payment.setPaymentType("order"); // Bắt buộc để thỏa mãn CK_Payments_OneTarget

        // Lúc này order.getTotalAmount() đã lấy chuẩn xác Tổng tiền + 25k ship
        payment.setAmount(order.getTotalAmount());

        // Phân loại trạng thái ban đầu dựa trên phương thức
        if ("MOMO".equalsIgnoreCase(method)) {
            payment.setMethod("momo");
            payment.setStatus("pending"); // Chờ khách quét mã
        } else {
            payment.setMethod("cod");
            payment.setStatus("pending"); // Chờ giao hàng thu tiền
        }

        paymentRepo.save(payment);
        cartItemRepo.deleteAll(cart.getItems());

        return order;
    }

    // Hàm hỗ trợ tính tổng tiền
    private BigDecimal calculateSubtotal(List<CartItem> items) {
        return items.stream()
                .map(item -> {
                    BigDecimal price = (item.getProduct() != null)
                            ? item.getProduct().getSalePrice()
                            : item.getPet().getSalePrice();
                    return price.multiply(new BigDecimal(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    //List Order
    public List<Order> getAllOrder() {
        return orderRepo.findAll();
    }

    public Order findByOrderCode(String orderCode) {
        return orderRepo.findByOrderCode(orderCode);
    }

    // 2. Phương thức cập nhật (lưu đè) đơn hàng vào Database
    public Order updateOrder(Order order) {
        return orderRepo.save(order);
    }


    @Transactional
    public void updateOrderStatusByAdmin(Integer orderID, String newStatus) {
        // Bước 1: Lấy dữ liệu hiện tại từ DB lên để so sánh
        Order order = orderRepo.findById(orderID)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderID));

        String currentStatus = order.getStatus().toLowerCase();
        String targetStatus = newStatus.toLowerCase().trim();

        // Bước 2: Luật chặn thay đổi trạng thái "Cuối"
        if ("done".equals(currentStatus) || "canceled".equals(currentStatus)) {
            throw new RuntimeException("Đơn hàng đã kết thúc (Done/Canceled), không thể sửa!");
        }

        // Bước 3: Luật chặn Paid -> Pending
        if ("paid".equals(currentStatus) && "pending".equals(targetStatus)) {
            throw new RuntimeException("Đơn đã thanh toán (Paid), không thể quay về Chờ xử lý!");
        }

        // BƯỚC 4: Luật chặn Paid -> Canceled (Mới thêm theo yêu cầu của bạn)
        if ("paid".equals(currentStatus) && "canceled".equals(targetStatus)) {
            throw new RuntimeException("Đơn đã thanh toán MoMo, không được phép Hủy. Hãy dùng Refund (Hoàn tiền)!");
        }

        // Bước 5: Nếu vượt qua hết các "cửa ải" trên thì mới cho phép đổi
        order.setStatus(targetStatus);

        // Bước 6: Lưu xuống DB
        orderRepo.saveAndFlush(order);
    }

    @Transactional
    public void cancelOrderById(Integer orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        // RÀNG BUỘC: Chỉ cho phép hủy nếu đơn hàng ở trạng thái pending
        if (!"pending".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Chỉ đơn hàng ở trạng thái 'Chờ xử lý' mới có thể hủy.");
        }

        // 1. Chuyển trạng thái đơn hàng sang canceled
        order.setStatus("canceled");
        orderRepo.save(order);

        // 2. Trả lại thú cưng (Pets) về trạng thái AVAILABLE nếu có trong đơn hàng
//        if (order.getOrderItems() != null) {
//            for (OrderItems item : order.getOrderItems()) {
//                if (item.getPet() != null) {
//                    Pets pet = item.getPet();
//                    pet.setStatus("AVAILABLE");
//                    petRepo.save(pet);
//                }
//            }
//        }
    }
}
