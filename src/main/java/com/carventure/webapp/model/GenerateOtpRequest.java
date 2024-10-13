package com.carventure.webapp.model;

import com.carventure.webapp.validation.ValidOtpRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@ValidOtpRequest // Apply the custom validation at the class level
public class GenerateOtpRequest {

    @Size(min = 10, max = 10, message = "10-digit valid mobile number is required.")
    @Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits long and contain only numbers.")
    @JsonProperty("mobile_number") // For JSON serialization
    @Field("mobile_number")
    private String mobileNumber;

    // Email is optional
    @Pattern(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Email should be valid.")
    @JsonProperty("email") // For JSON serialization
    @Field("email")
    private String email;

    // Getters and Setters
    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
