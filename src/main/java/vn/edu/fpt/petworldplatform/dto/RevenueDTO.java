package vn.edu.fpt.petworldplatform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RevenueDTO {
    private String orderCode;
    private String customerName;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;

    public RevenueDTO() {
    }

    public RevenueDTO(String orderCode, String customerName, LocalDateTime orderDate, 
                      BigDecimal totalAmount, String status, String paymentMethod) {
        this.orderCode = orderCode;
        this.customerName = customerName;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentMethod = paymentMethod;
    }

    // Getters and Setters
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}