package vn.edu.fpt.petworldplatform.dto;

import lombok.Data;

@Data
public class PetCreateDTO {
    // Tên biến này đang là createPetOwnerType
    private String createPetOwnerType;

    private String name;
    private String species;
    private String breed;
    private Integer age;

    private Integer ownerId;

    private Double price;
    private String imageUrl;
    private String description;
}