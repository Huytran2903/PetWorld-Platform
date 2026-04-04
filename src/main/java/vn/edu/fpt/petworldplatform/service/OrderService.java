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
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepo;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepo;
    @Autowired
    private CartRepository cartRepository;

    @Transactional
    public Order createOrder(Integer customerId, String name, String phone, String addr, String note, String method) {

        Carts cart = cartRepository.findByCustomerId(customerId)
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


        order = orderRepository.save(order);

        List<OrderItems> listOrderItems = new ArrayList<>();

        for (CartItem ci : itemsInCart) {
            OrderItems oi = new OrderItems();
            oi.setOrder(order);

            if (ci.getProduct() != null) {
                Product p = ci.getProduct();
                oi.setProduct(p);
                oi.setItemName(p.getName());
                oi.setUnitPrice(p.getSalePrice());

                if (p.getStock() < ci.getQuantity()) {
                    throw new RuntimeException("Not enough stock available for product " + p.getName() + " !");
                }
                p.setStock(p.getStock() - ci.getQuantity());
                productRepository.save(p);

            } else if (ci.getPet() != null) {

                Pets pet = ci.getPet();

                oi.setPet(pet);
                oi.setItemName(pet.getName());
                oi.setUnitPrice(pet.getSalePrice());


                Customer owner = customerRepository.findById(customerId).get();
                pet.setOwner(owner);
                pet.setIsAvailable(false);
                pet.setPurchasedAt(LocalDateTime.now());
            }

            oi.setQuantity(ci.getQuantity());

            orderItemRepository.save(oi);


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
    public Page<Order> getAllOrder(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    //Customer
    public Page<Order> getAllOrderById(Pageable pageable, Customer customer) {
        return orderRepository.findAllByCustomerOrderByOrderIDDesc(pageable, customer);
    }


    //List Order
    public Page<Order> getAllOrderFilter(LocalDate startDate, LocalDate endDate, String status, Pageable pageable) {

        boolean hasDate = (startDate != null && endDate != null);
        boolean hasStatus = (status != null && !status.trim().isEmpty());


        if (hasDate && hasStatus) {
            return orderRepository.findByCreatedAtBetweenAndStatus(
                    startDate.atStartOfDay(),
                    endDate.atTime(LocalTime.MAX),
                    status,
                    pageable);
        }


        if (hasDate) {
            return orderRepository.findByCreatedAtBetween(
                    startDate.atStartOfDay(),
                    endDate.atTime(LocalTime.MAX),
                    pageable);
        }


        if (hasStatus) {
            return orderRepository.findByStatus(status, pageable);
        }


        return orderRepository.findAll(pageable);
    }

    public Order findByOrderCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode);
    }


    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }


    @Transactional
    public void updateOrderStatusByAdmin(Integer orderID, String newStatus) {

        Order order = orderRepository.findById(orderID)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderID));

        String currentStatus = order.getStatus().toLowerCase();
        String targetStatus = newStatus.toLowerCase().trim();

        if ("done".equals(currentStatus) || "canceled".equals(currentStatus)) {
            throw new RuntimeException("The order has already been finalized (Done/Canceled) and cannot be modified.");
        }

        if ("paid".equals(currentStatus) && "pending".equals(targetStatus)) {
            throw new RuntimeException("The order is already Paid and cannot be reverted to Pending status.");
        }

        if ("paid".equals(currentStatus) && "canceled".equals(targetStatus)) {
            throw new RuntimeException("Orders paid via MoMo cannot be canceled directly. Please initiate the Refund process instead.");
        }

        if ("canceled".equals(targetStatus)) {
            for (OrderItems item : order.getOrderItems()) {

                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    int restoredStock = product.getStock() + item.getQuantity();
                    product.setStock(restoredStock);
                    productRepository.save(product);
                }

                if (item.getPet() != null) {
                    Pets pet = item.getPet();
                    pet.setOwner(null);
                    pet.setIsAvailable(true);
                    petRepository.save(pet);
                }
            }
        }

        order.setStatus(targetStatus);
        orderRepository.saveAndFlush(order);
    }

    private void deductProductStockForDoneOrder(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        for (OrderItems item : order.getOrderItems()) {
            if (item.getProduct() == null) {
                continue;
            }

            Product product = item.getProduct();

            int quantityToDeduct = item.getQuantity() != null ? item.getQuantity() : 0;
            if (quantityToDeduct <= 0) {
                continue;
            }

            if (product.getStock() < quantityToDeduct) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ tồn kho để hoàn tất đơn hàng.");
            }

            product.setStock(product.getStock() - quantityToDeduct);
            productRepository.save(product);
        }
    }


    @Transactional
    public void cancelOrderById(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        if (!"pending".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Chỉ đơn hàng ở trạng thái 'Chờ xử lý' mới có thể hủy.");
        }

        List<OrderItems> items = order.getOrderItems();

        for (OrderItems item : items) {

            if (item.getProduct() != null) {
                Product product = item.getProduct();
                int restoredQuantity = product.getStock() + item.getQuantity();
                product.setStock(restoredQuantity);

                productRepository.save(product);
            }

            if (item.getPet() != null) {
                Pets pet = item.getPet();

                pet.setOwner(null);
                pet.setIsAvailable(true);

                petRepository.save(pet);
            }
        }

        // Đổi trạng thái đơn hàng thành đã hủy
        order.setStatus("canceled");
        orderRepository.save(order);
    }
}
