package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Pets")
@Data // Lombok getter/setter
@NoArgsConstructor
@AllArgsConstructor
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PetID")
    private Integer id;

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "PetType")
    private String petType; // dog, cat

    @Column(name = "Breed")
    private String breed;

    @Column(name = "AgeMonths")
    private Integer ageMonths;

    @Column(name = "Price")
    private Double price;

    @Column(name = "ImageUrl")
    private String imageUrl;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "IsAvailable")
    private boolean available;

    // Quan hệ với Customer (Chủ sở hữu)
    @ManyToOne
    @JoinColumn(name = "OwnerCustomerID")
    private Customer owner;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}