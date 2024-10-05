package com.carventure.webapp.controller;

import com.carventure.webapp.exception.CooldownActiveException;
import com.carventure.webapp.exception.InvalidOtpException;
import com.carventure.webapp.service.OTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
public class OTPController {

    private final OTPService otpService;
    private static final Logger logger = LoggerFactory.getLogger(OTPController.class);

    @Autowired
    public OTPController(OTPService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    public String generateOTP(@RequestParam String phoneNumber) {
        logger.info("Generating OTP for phone number {}", phoneNumber);
        String otp = otpService.sendOtp(phoneNumber);
        logger.info("OTP is: {}", otp);
        return otp;
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyOtp(@RequestParam String phoneNumber, @RequestParam String otpInput) {
        logger.info("Verifying OTP for phone number {}", phoneNumber);
        try {
            boolean isVerified = otpService.verifyOtp(phoneNumber, otpInput);
            if (isVerified) {
                logger.info("OTP verification successful for phone number {}", phoneNumber);
                return ResponseEntity.ok("OTP verified successfully.");
            }
        } catch (InvalidOtpException e) {
            logger.warn("OTP verification failed for phone number {}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CooldownActiveException e) {
            logger.warn("Cool down period is active for user{}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
        return null;
    }

}
