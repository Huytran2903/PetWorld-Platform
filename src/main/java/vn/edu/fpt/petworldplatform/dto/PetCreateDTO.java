package vn.edu.fpt.petworldplatform.dto;

import lombok.Data;

@Data
public class PetCreateDTO {
    private String name;
    private String species; // Khớp với th:field="*{species}"
    private String breed;
    private Integer age;
    private String description;
    private String imageUrl;

    // Hai trường quan trọng gây lỗi
    private String createPetOwnerType; // Khớp với th:field="*{createPetOwnerType}" (value="shop" hoặc "customer")
    private Long ownerId;              // Khớp với th:field="*{ownerId}"

    private Double price;
}