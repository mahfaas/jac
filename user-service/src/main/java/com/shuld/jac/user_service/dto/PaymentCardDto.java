package com.shuld.jac.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public class PaymentCardDto {

    private Long id;

    @NotNull
    private Long userId;

    @NotBlank
    @Pattern(regexp = "\\d{13,19}", message = "Card number must be 13 to 19 digits")
    private String number;

    @NotBlank
    private String holder;

    @NotBlank
    @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Expected format MM/YY")
    private String expirationDate;

    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getHolder() { return holder; }
    public void setHolder(String holder) { this.holder = holder; }
    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}