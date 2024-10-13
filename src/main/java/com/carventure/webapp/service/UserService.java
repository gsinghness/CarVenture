package com.carventure.webapp.service;

import com.carventure.webapp.exception.CooldownActiveException;
import com.carventure.webapp.exception.InvalidOtpException;
import com.carventure.webapp.exception.OtpExpiredException;
import com.carventure.webapp.exception.UserNotFoundException;
import com.carventure.webapp.model.User;
import com.carventure.webapp.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    private final UserRepository userRepository;
    private static final Random RANDOM = new Random();
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("${golden.otp.value}")
    private String goldenOtpValue;

    @Value("${otp.retry.count}")
    private int otpRetryCount;

    @Value("${otp.cooloff.time}")
    private int otpCooloffTime;

    @Value("${otp.expiry.time}")
    private int otpExpiryTime;

    private final JavaMailSender mailSender;

    private Environment environment;

    private static final String OTP_SENT = "OTP sent successfully";

    @Autowired
    public UserService(UserRepository userRepository, JavaMailSender javaMailSender, Environment environment) {
        this.userRepository = userRepository;
        this.mailSender = javaMailSender;
        this.environment = environment;
    }

    public String sendPhoneOtp(String phoneNumber) {
        //get user by mobile number
        Result result = getOtp(phoneNumber);
        User user;
        if (result.userOptional.isPresent()) {
            user = result.userOptional.get();
            if (user.getOtpRetryCount() >= otpRetryCount) { //if user attempted more than 5
                if (LocalDateTime.now().isBefore(user.getOtpCoolOffEndTime())) {
                    throw new CooldownActiveException("User is still in the cooldown period. Please try again later");
                } else {
                    user.setOtp(result.hashedOtp);
                    user.setOtpExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryTime));
                    user.setOtpRetryCount(1);
                    //sendMobileMessage();
                    userRepository.save(user);
                    return OTP_SENT;
                }
            } else { // has not attempted more than 5 times
                user.setOtpRetryCount(user.getOtpRetryCount() + 1);
                if (user.getOtpRetryCount() == 5) {
                    user.setOtpCoolOffEndTime(LocalDateTime.now().plusMinutes(otpCooloffTime));
                }
                user.setOtp(result.hashedOtp);
                user.setOtpExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryTime));
                //sendMobileMessage();
                userRepository.save(user);
                return OTP_SENT;
            }

        } else { //if new user
            user = new User(phoneNumber, result.hashedOtp, 1, LocalDateTime.now().plusMinutes(otpExpiryTime));
            //sendMobileMessage();
            userRepository.save(user);
            return OTP_SENT;
        }
    }

    private Result getOtp(String phoneNumber) {
        Optional<User> userOptional = userRepository.findByPhone(phoneNumber);
        String otp;
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "";
        if ("localhost".equals(activeProfile)) {
            otp = goldenOtpValue;
        } else {
            otp = generateOtp();
        }
        // Hash the OTP
        String hashedOtp = hashOtp(otp);
        return new Result(userOptional, otp, hashedOtp);
    }

    private static class Result {
        public final Optional<User> userOptional;
        public final String otp;
        public final String hashedOtp;

        public Result(Optional<User> userOptional, String otp, String hashedOtp) {
            this.userOptional = userOptional;
            this.otp = otp;
            this.hashedOtp = hashedOtp;
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

    public boolean verifyPhoneOtp(String phoneNumber, String otpInput) {
       return verifyOtp(phoneNumber, otpInput, "mobile");
    }

    private boolean verifyOtp(String phoneNumber, String otpInput, String entityInVerification) {
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
                    if (entityInVerification.equals("mobile")) {
                        user.setCurrentState(User.CurrentState.MOBILE_VERIFIED.toString());
                    } else if (entityInVerification.equals("email")) {
                        user.setCurrentState(User.CurrentState.EMAIL_VERIFIED.toString());
                    }
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

    // Method to generate JWT token
    public String generateToken(String phoneNumber) {
        return Jwts.builder()
                .setSubject(phoneNumber)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // Token valid for 1 day
                .signWith(SignatureAlgorithm.HS512, "b2F8c7eR3t6Qp0zL9vX5gY4hK1jN8sM7wP0oU4fH2kR9aD6cQ3tY8zV4wP5eF7gU".getBytes()) // Replace with your secret key
                .compact();
    }

    public String extractPhoneNumberFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey("b2F8c7eR3t6Qp0zL9vX5gY4hK1jN8sM7wP0oU4fH2kR9aD6cQ3tY8zV4wP5eF7gU".getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
        logger.info("Current user is: {}", claims.getSubject());
        return claims.getSubject();
    }

    public String sendEmailOtp(String email, String userPhoneNumber) {

        checkUser(userPhoneNumber);

        Result result = getOtp(userPhoneNumber);
        User user;
        if (result.userOptional.isPresent()) {
            user = result.userOptional.get();
            if (user.getOtpRetryCount() >= otpRetryCount) { // If user attempted more than 5
                if (LocalDateTime.now().isBefore(user.getOtpCoolOffEndTime())) {
                    throw new CooldownActiveException("User is still in the cooldown period. Please try again later");
                } else {
                    user.setOtp(result.hashedOtp);
                    user.setOtpExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryTime));
                    user.setOtpRetryCount(1);
                    sendEmail(email, result.otp); // Send OTP email
                    userRepository.save(user);
                    return OTP_SENT;
                }
            } else { // Has not attempted more than 5 times
                user.setOtpRetryCount(user.getOtpRetryCount() + 1);
                if (user.getOtpRetryCount() == 5) {
                    user.setOtpCoolOffEndTime(LocalDateTime.now().plusMinutes(otpCooloffTime));
                }
                user.setOtp(result.hashedOtp);
                user.setOtpExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryTime));
                user.setEmail(email);
                sendEmail(email, result.otp); // Send OTP email
                userRepository.save(user);
                return OTP_SENT;
            }
        } else {
            throw new UserNotFoundException("Internal server error, user not found. Try starting a new mobile number verification process for adding new user");
        }
    }

    private static void checkUser(String userPhoneNumber) {
        if (StringUtils.isBlank(userPhoneNumber)) {
            throw new UserNotFoundException("Not able to find any valid user details from token");
        }
    }
    public boolean userExists (String userPhoneNumber) {
        Optional<User> userOptional = userRepository.findByPhone(userPhoneNumber);
        return userOptional.isPresent();
    }

    private void sendEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("gurpreet.singh.ness@gmail.com");
        message.setTo(to);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP code is: " + otp);
        mailSender.send(message);
    }

    public boolean verifyEmailOTP(String email, String otpInput, String userPhoneNumber) {

        checkUser(userPhoneNumber);
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new UserNotFoundException("Invalid email address sent");
        }
        return verifyOtp(userPhoneNumber, otpInput, "email");
    }


    public boolean setPreferredName(String userPhoneNumber, String preferredName) {
        Optional<User> userOptional = userRepository.findByPhone(userPhoneNumber);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPreferredName(preferredName);
            user.setCurrentState(User.CurrentState.PREFERRED_NAME_FILLED.toString());
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
