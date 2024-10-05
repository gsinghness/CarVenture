package com.carventure.webapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "otp_attempts")
public class OtpAttempt {
    @Id
    private String phoneNumber;
    private int retryCount;
    private LocalDateTime coolOffEndTime;

    public OtpAttempt(String phoneNumber, int retryCount, LocalDateTime coolOffEndTime) {
        this.phoneNumber = phoneNumber;
        this.retryCount = retryCount;
        this.coolOffEndTime = coolOffEndTime;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCoolOffEndTime() {
        return coolOffEndTime;
    }

    public void setCoolOffEndTime(LocalDateTime coolOffEndTime) {
        this.coolOffEndTime = coolOffEndTime;
    }
}
