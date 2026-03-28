package vn.edu.fpt.petworldplatform.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        Carts cart = cartRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại!"));


        List<CartItem> itemsInCart = cart.getItems();
        if (itemsInCart == null || itemsInCart.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống!");
        }


        Order order = new Order();
        order.setCustomerID(customerId);
        order.setOrderCode("PET-" + System.currentTimeMillis());
        order.setShipName(name);
        order.setShipPhone(phone);
        order.setShipAddress(addr);
        order.setNote(note);
        order.setStatus("pending");
        order.setCreatedAt(LocalDateTime.now());


        BigDecimal subtotal = calculateSubtotal(itemsInCart);
        order.setSubtotal(subtotal);


        BigDecimal shippingFee = new BigDecimal("25000");
        order.setShippingFee(shippingFee);


        order.setTotalAmount(subtotal.add(shippingFee));



        order = orderRepo.save(order);

        List<OrderItems> listOrderItems = new ArrayList<>();

        for (CartItem ci : itemsInCart) {
            OrderItems oi = new OrderItems();
            oi.setOrder(order);

            if (ci.getProduct() != null) {
                oi.setProductID(ci.getProduct().getProductId());
                oi.setItemName(ci.getProduct().getName());
                oi.setUnitPrice(ci.getProduct().getSalePrice());

                Product p = ci.getProduct();
                if(p.getStock() < ci.getQuantity()) {
                    throw new RuntimeException("Not enough stock available for product " + p.getName() + " !");
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
                pet.setPurchasedAt(LocalDateTime.now());
            }

            oi.setQuantity(ci.getQuantity());

            orderItemRepo.save(oi);


            listOrderItems.add(oi);
        }


        order.setOrderItems(listOrderItems);


        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentType("order");


        payment.setAmount(order.getTotalAmount());


        if ("MOMO".equalsIgnoreCase(method)) {
            payment.setMethod("momo");
            payment.setStatus("pending");
        } else {
            payment.setMethod("cod");
            payment.setStatus("pending");
        }

        paymentRepo.save(payment);
        cartItemRepo.deleteAll(cart.getItems());

        return order;
    }


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

    //Admin
    public Page<Order> getAllOrder(Pageable pageable){
       return orderRepo.findAll(pageable);
    }

    //Customer
    public Page<Order> getAllOrderById(Pageable pageable, Customer customer){
        return orderRepo.findAllByCustomerOrderByOrderIDDesc(pageable, customer);
    }


    //List Order
    public Page<Order> getAllOrderFilter(LocalDate startDate, LocalDate endDate, String status, Pageable pageable) {

        boolean hasDate = (startDate != null && endDate != null);
        boolean hasStatus = (status != null && !status.trim().isEmpty());


        if (hasDate && hasStatus) {
            return orderRepo.findByCreatedAtBetweenAndStatus(
                    startDate.atStartOfDay(),
                    endDate.atTime(LocalTime.MAX),
                    status,
                    pageable);
        }


        if (hasDate) {
            return orderRepo.findByCreatedAtBetween(
                    startDate.atStartOfDay(),
                    endDate.atTime(LocalTime.MAX),
                    pageable);
        }


        if (hasStatus) {
            return orderRepo.findByStatus(status, pageable);
        }


        return orderRepo.findAll(pageable);
    }

    public Order findByOrderCode(String orderCode) {
        return orderRepo.findByOrderCode(orderCode);
    }


    public Order updateOrder(Order order) {
        return orderRepo.save(order);
    }


    @Transactional
    public void updateOrderStatusByAdmin(Integer orderID, String newStatus) {

        Order order = orderRepo.findById(orderID)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderID));

        String currentStatus = order.getStatus().toLowerCase();
        String targetStatus = newStatus.toLowerCase().trim();


        if ("done".equals(currentStatus) || "canceled".equals(currentStatus)) {
            throw new RuntimeException("Đơn hàng đã kết thúc (Done/Canceled), không thể sửa!");
        }


        if ("paid".equals(currentStatus) && "pending".equals(targetStatus)) {
            throw new RuntimeException("Đơn đã thanh toán (Paid), không thể quay về Chờ xử lý!");
        }


        if ("paid".equals(currentStatus) && "canceled".equals(targetStatus)) {
            throw new RuntimeException("Đơn đã thanh toán MoMo, không được phép Hủy. Hãy dùng Refund (Hoàn tiền)!");
        }



        order.setStatus(targetStatus);

        orderRepo.saveAndFlush(order);
    }

    private void deductProductStockForDoneOrder(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        for (OrderItems item : order.getOrderItems()) {
            if (item.getProductID() == null) {
                continue;
            }

            Product product = productRepo.findById(item.getProductID())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + item.getProductID()));

            int quantityToDeduct = item.getQuantity() != null ? item.getQuantity() : 0;
            if (quantityToDeduct <= 0) {
                continue;
            }

            if (product.getStock() < quantityToDeduct) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " không đủ tồn kho để hoàn tất đơn hàng.");
            }

            product.setStock(product.getStock() - quantityToDeduct);
            productRepo.save(product);
        }
    }

    @Transactional
    public void cancelOrderById(Integer orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));


        if (!"pending".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Chỉ đơn hàng ở trạng thái 'Chờ xử lý' mới có thể hủy.");
        }


        order.setStatus("canceled");
        orderRepo.save(order);

    }
}
