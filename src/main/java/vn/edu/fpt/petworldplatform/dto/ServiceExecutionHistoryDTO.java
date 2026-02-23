package vn.edu.fpt.petworldplatform.dto;

import java.time.LocalDateTime;

/**
 * DTO for Service Execution History
 * Maps data from ServiceExecutionHistoryRepository queries
 */
public class ServiceExecutionHistoryDTO {

    private String appointmentCode;
    private String customerName;
    private String petName;
    private String serviceName;  // Changed to handle multiple services (comma-separated from STRING_AGG)
    private LocalDateTime appointmentDate;
    private String status;
    private String assignedStaff;

    // No-arg constructor (required by some frameworks)
    public ServiceExecutionHistoryDTO() {
    }

    // Full constructor
    public ServiceExecutionHistoryDTO(String appointmentCode, 
                                      String customerName, 
                                      String petName,
                                      String serviceName, 
                                      LocalDateTime appointmentDate,
                                      String status, 
                                      String assignedStaff) {
        this.appointmentCode = appointmentCode;
        this.customerName = customerName;
        this.petName = petName;
        this.serviceName = serviceName;
        this.appointmentDate = appointmentDate;
        this.status = status;
        this.assignedStaff = assignedStaff;
    }

    // Getters and Setters
    public String getAppointmentCode() {
        return appointmentCode;
    }

    public void setAppointmentCode(String appointmentCode) {
        this.appointmentCode = appointmentCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public LocalDateTime getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDateTime appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(String assignedStaff) {
        this.assignedStaff = assignedStaff;
    }

    @Override
    public String toString() {
        return "ServiceExecutionHistoryDTO{" +
                "appointmentCode='" + appointmentCode + '\'' +
                ", customerName='" + customerName + '\'' +
                ", petName='" + petName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", appointmentDate=" + appointmentDate +
                ", status='" + status + '\'' +
                ", assignedStaff='" + assignedStaff + '\'' +
                '}';
    }
}