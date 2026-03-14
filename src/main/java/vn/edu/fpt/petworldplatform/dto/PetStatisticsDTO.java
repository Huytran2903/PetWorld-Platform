package vn.edu.fpt.petworldplatform.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PetStatisticsDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Overall statistics
    private Long totalPets;
    private Long totalServicePets;
    private Long totalSalePets;
    private Long completedServicePets;
    private Long soldPets;
    
    // By species breakdown
    private List<PetSpeciesStats> dogStats;
    private List<PetSpeciesStats> catStats;
    private List<PetSpeciesStats> otherStats;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetSpeciesStats {
        private String category; // "SERVICE", "SALE", "COMPLETED_SERVICE", "SOLD"
        private Long count;
        private Double percentage;
    }
}
