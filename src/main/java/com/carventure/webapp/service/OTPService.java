package com.carventure.webapp.service;

import com.carventure.webapp.model.OTP;

public interface OTPService {
    String sendOtp(String phoneNumber);
    boolean verifyOtp(String phoneNumber, String otpCode);
}
