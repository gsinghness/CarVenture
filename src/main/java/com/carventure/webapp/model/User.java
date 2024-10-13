package com.carventure.webapp.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
public class User {

    public enum CurrentState {
        //small case stages mobile_verified, email_verified, preferred_name_filled
        MOBILE_VERIFIED("mobile_verified"),
        EMAIL_VERIFIED("email_verified"),
        PREFERRED_NAME_FILLED("preferred_name_filled");

        private final String value;
        CurrentState(String value) {
            this.value = value;
        }
        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String otp;
    private int otpRetryCount;
    private LocalDateTime otpExpiryDate;
    private LocalDateTime otpCoolOffEndTime;
    private String currentState;
    private String preferredName;

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

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }
}
