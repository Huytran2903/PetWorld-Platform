package vn.edu.fpt.petworldplatform.entity;



import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;



import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Categories")
public class Categories {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID")
    private Integer categoryID;

    @NotBlank(message = "Name is required!")
    @Pattern(
            regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$",
            message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\""
    )
    @Column(name = "Name", nullable = false, length = 255)
    private String name;

    @Pattern(
            regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$",
            message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\""
    )
    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> productList = new ArrayList<>();

    // Tự động gán thời gian khi lưu vào database
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true; // Mặc định là hoạt động nếu không set
        }
    }
}
