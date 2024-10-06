package com.carventure.webapp.service;

import com.carventure.webapp.exception.CooldownActiveException;
import com.carventure.webapp.exception.InvalidOtpException;
import com.carventure.webapp.exception.OtpExpiredException;
import com.carventure.webapp.exception.UserNotFoundException;
import com.carventure.webapp.model.User;
import com.carventure.webapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    private final UserRepository userRepository;
    private static final Random RANDOM = new Random();

    @Value("${golden.otp.value}")
    private String goldenOtpValue;

    @Value("${otp.retry.count}")
    private int otpRetryCount;

    @Value("${otp.cooloff.time}")
    private int otpCooloffTime;

    @Value("${otp.expiry.time}")
    private int otpExpiryTime;

    private Environment environment;

    private static final String  OTP_SENT = "OTP sent successfully";

    @Autowired
    public UserService(UserRepository userRepository, Environment environment) {
        this.userRepository = userRepository;
        this.environment = environment;
    }

    public String sendOtp(String phoneNumber) {
        //get user by mobile number
        Optional<User> userOptional = userRepository.findByPhone(phoneNumber);
        User user;
        String otp;
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "";
        if ("localhost".equals(activeProfile)) {
            otp = goldenOtpValue;
        } else {
            otp = generateOtp();
        }
        // Hash the OTP
        String hashedOtp = hashOtp(otp);
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getOtpRetryCount() >= otpRetryCount) { //if user attempted more than 5
                if (LocalDateTime.now().isBefore(user.getOtpCoolOffEndTime())) {
                    throw new CooldownActiveException("User is still in the cooldown period. Please try again later");
                } else {
                    user.setOtpRetryCount(1);
                    userRepository.save(user);
                    return OTP_SENT;
                }
            } else { // has not attempted more than 5 times
                user.setOtpRetryCount(user.getOtpRetryCount() + 1);
                if (user.getOtpRetryCount() == 5) {
                    user.setOtpCoolOffEndTime(LocalDateTime.now().plusMinutes(otpCooloffTime));
                }
                userRepository.save(user);
                return OTP_SENT;
            }

        } else { //if new user
            user = new User(phoneNumber, hashedOtp, 1, LocalDateTime.now().plusSeconds(otpExpiryTime));
            userRepository.save(user);
            return OTP_SENT;
        }
    }

    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(10000));
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString(); // Return hashed OTP as hex string
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing OTP", e);
        }
    }

    public boolean verifyOtp(String phoneNumber, String otpInput) {
        Optional<User> userOptional = userRepository.findByPhone(phoneNumber);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (LocalDateTime.now().isBefore(user.getOtpExpiryDate())) {
                // Hash the OTP
                String hashedOtp = hashOtp(otpInput);
                if (hashedOtp.equals(user.getOtp())) {
                    user.setOtp(null);
                    user.setOtpRetryCount(1);
                    user.setOtpExpiryDate(null);
                    user.setOtpCoolOffEndTime(null);
                    userRepository.save(user);
                    return true;
                } else {
                    throw new InvalidOtpException("OTP does not match");
                }
            } else {
                throw new OtpExpiredException("OTP expired, please retry with a new one");
            }
        }
        throw new UserNotFoundException("Internal server error, user not found. Try getting a new OTP");
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
