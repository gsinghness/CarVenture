package com.carventure.webapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "otps") // This specifies the MongoDB collection name
public class OTP {
    @Id
    private String id; // Optional: If you want to have an auto-generated ID
    private String phoneNumber;
    private String otpCode;
    private long timestamp;

    public OTP(String phoneNumber, String otpCode) {
        this.phoneNumber = phoneNumber;
        this.otpCode = otpCode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
