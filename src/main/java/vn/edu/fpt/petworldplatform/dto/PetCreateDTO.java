package vn.edu.fpt.petworldplatform.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PetCreateDTO {

    private String createPetOwnerType;

    private String name;
    private String species;
    private String breed;
    private Integer age;

    private Integer ownerId;

    private Double price;
    private String imageUrl;
    private String description;

    private Double weightKg;
    private String color;
    private String gender;
    private String note;

    private MultipartFile imageFile;

    public void setOwnerId(Integer customerId) {

    }
}