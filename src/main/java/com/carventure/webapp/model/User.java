package com.carventure.webapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
public class User {

    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String otp;
    private int otpRetryCount;
    private LocalDateTime otpExpiryDate;
    private LocalDateTime otpCoolOffEndTime;

    public User( String phone, String otp, int otpRetryCount, LocalDateTime otpExpiryDate) {
        this.phone = phone;
        this.otp = otp;
        this.otpRetryCount = otpRetryCount;
        this.otpExpiryDate = otpExpiryDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public int getOtpRetryCount() {
        return otpRetryCount;
    }

    public void setOtpRetryCount(int otpRetryCount) {
        this.otpRetryCount = otpRetryCount;
    }

    public LocalDateTime getOtpExpiryDate() {
        return otpExpiryDate;
    }

    public void setOtpExpiryDate(LocalDateTime otpExpiryDate) {
        this.otpExpiryDate = otpExpiryDate;
    }

    public LocalDateTime getOtpCoolOffEndTime() {
        return otpCoolOffEndTime;
    }

    public void setOtpCoolOffEndTime(LocalDateTime otpCoolOffEndTime) {
        this.otpCoolOffEndTime = otpCoolOffEndTime;
    }
}
