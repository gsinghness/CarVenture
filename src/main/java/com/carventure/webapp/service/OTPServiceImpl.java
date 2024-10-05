package com.carventure.webapp.service;

import com.carventure.webapp.model.OTP;
import com.carventure.webapp.repository.OTPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("prod")
@Primary
@Service
public class OTPServiceImpl implements OTPService{

    private final OTPRepository otpRepository;

    @Autowired
    public OTPServiceImpl(OTPRepository otpRepository) {
        this.otpRepository = otpRepository;
    }
    @Override
    public String sendOtp(String phoneNumber) {
        // Here we implement the logic for sending OTP using a third-party service
        return null;
    }

    @Override
    public boolean verifyOtp(String phoneNumber, String otpCode) {
        return false;
    }
}
