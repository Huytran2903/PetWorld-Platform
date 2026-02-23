package vn.edu.fpt.petworldplatform.dto;

import java.math.BigDecimal;

/**
 * DTO for Service Usage Statistics
 * Used to display service popularity/usage frequency
 */
public class ServiceUsageStatsDTO {

    private String serviceName;
    private Long usageCount;
    private BigDecimal percentage;

    // No-arg constructor
    public ServiceUsageStatsDTO() {
    }

    // Full constructor
    public ServiceUsageStatsDTO(String serviceName, Long usageCount, BigDecimal percentage) {
        this.serviceName = serviceName;
        this.usageCount = usageCount;
        this.percentage = percentage;
    }

    // Getters and Setters
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    /**
     * Get percentage as double (for easier template usage)
     */
    public Double getPercentageValue() {
        return percentage != null ? percentage.doubleValue() : 0.0;
    }

    /**
     * Get color class based on percentage
     * Used for chart visualization
     */
    public String getColorClass() {
        if (percentage == null) return "bg-gray-500";
        
        double pct = percentage.doubleValue();
        if (pct >= 30) return "bg-pink-500";
        if (pct >= 20) return "bg-purple-500";
        if (pct >= 10) return "bg-blue-500";
        return "bg-green-500";
    }

    @Override
    public String toString() {
        return "ServiceUsageStatsDTO{" +
                "serviceName='" + serviceName + '\'' +
                ", usageCount=" + usageCount +
                ", percentage=" + percentage +
                '}';
    }
}