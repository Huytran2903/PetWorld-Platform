package vn.edu.fpt.petworldplatform.dto;

import lombok.Data;

@Data
public class PetCreateDTO {
    private String name;
    private String species; // dog/cat
    private String breed;
    private Integer age;
    private String description;
    private String imageUrl;

    // Logic form
    private String ownerType; // "shop" hoặc "customer"
    private Long ownerId;     // ID khách hàng (nếu chọn customer)
    private Double price;     // Giá bán (nếu chọn shop)
}