package com.carventure.webapp.service;

import com.carventure.webapp.exception.CooldownActiveException;
import com.carventure.webapp.exception.InvalidOtpException;
import com.carventure.webapp.model.OTP;
import com.carventure.webapp.model.OtpAttempt;
import com.carventure.webapp.repository.OTPRepository;
import com.carventure.webapp.repository.OtpAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Optional;

@Profile("qa")
@Service
public class OTPServiceQA implements OTPService {

    private static final Logger logger = LoggerFactory.getLogger(OTPServiceQA.class);
    private final OTPRepository otpRepository;
    private final OtpAttemptRepository otpAttemptRepository;
    @Value("${golden.otp.id}")
    private String goldenOtpId;

    @Value("${golden.otp.value}")
    private String goldenOtpValue;

    @Value("${golden.otp.phone}")
    private String goldenOtpPhone;

    @Value("${otp.retry.count}")
    private int otpRetryCount;

    @Value("${otp.cooloff.time}")
    private int otpCooloffTime;


    @Autowired
    public OTPServiceQA(OTPRepository otpRepository, OtpAttemptRepository otpAttemptRepository) {
        this.otpRepository = otpRepository;
        this.otpAttemptRepository = otpAttemptRepository;
    }

    @Override
    public String sendOtp(String phoneNumber) {
        OtpAttempt attempt = otpAttemptRepository.findById(phoneNumber).orElse(new OtpAttempt(phoneNumber, 1, LocalDateTime.now()));
        if (attempt.getRetryCount() > otpRetryCount && LocalDateTime.now().isBefore(attempt.getCoolOffEndTime())) {
            logger.warn("User has inputted wrong OTP 5 times. Cool-off period active until {}", attempt.getCoolOffEndTime());
            return "User has inputted wrong OTP 5 times, please wait for 10 minutes.";
        }
        Optional<OTP> goldenOtp = otpRepository.findById(goldenOtpId);
        if (goldenOtp.isPresent()) {
            OTP otp = goldenOtp.get();
            otp.setPhoneNumber(phoneNumber);
            if (attempt.getRetryCount() > otpRetryCount) {
                attempt.setRetryCount(1);
            } else {
                attempt.setRetryCount(attempt.getRetryCount() + 1);
            }
            if (attempt.getRetryCount() > otpRetryCount) {
                attempt.setCoolOffEndTime(LocalDateTime.now().plusMinutes(otpCooloffTime));
            }
            otpAttemptRepository.save(attempt);
            return otp.getOtpCode();
        }
        logger.debug("Golden OTP not found");
        return null;
    }

    @Override
    public boolean verifyOtp(String phoneNumber, String otpInput) {
        Optional<OTP> goldenOtp = otpRepository.findById(goldenOtpId);
        OtpAttempt attempt = otpAttemptRepository.findById(phoneNumber).orElse(new OtpAttempt(phoneNumber, 1, LocalDateTime.now()));
        if (attempt.getRetryCount() > otpRetryCount && LocalDateTime.now().isBefore(attempt.getCoolOffEndTime())) {
            logger.error("User is still in the cooldown period. Cannot verify OTP.");
            throw new CooldownActiveException("User is still in the cooldown period. Please try again later.");
        }

        if (goldenOtp.isPresent() && otpInput.equals(goldenOtp.get().getOtpCode())) {
            logger.info("OTP verification successful for phone number {}", phoneNumber);
            attempt.setRetryCount(0);
            otpAttemptRepository.save(attempt);
            return true;
        } else {
            attempt.setRetryCount(attempt.getRetryCount() + 1);
            if (attempt.getRetryCount() > otpRetryCount) {
                attempt.setCoolOffEndTime(LocalDateTime.now().plusMinutes(otpCooloffTime));
            }
            otpAttemptRepository.save(attempt);
            logger.error("Invalid OTP input for phone number {}", phoneNumber);
            throw new InvalidOtpException("Invalid OTP provided.");
        }
    }

    // Initialization method to run after the bean is constructed
    @PostConstruct
    private void initializeGoldenOtp() {
        if (!otpRepository.existsById(goldenOtpId)) {
            logger.info("Creating golden OTP");
            OTP otp = new OTP(goldenOtpPhone, goldenOtpValue);
            otp.setId(goldenOtpId);
            otpRepository.save(otp);
        }
    }
}
