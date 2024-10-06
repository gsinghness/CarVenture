package com.carventure.webapp.controller;

import com.carventure.webapp.exception.CooldownActiveException;
import com.carventure.webapp.exception.InvalidOtpException;
import com.carventure.webapp.exception.OtpExpiredException;
import com.carventure.webapp.exception.UserNotFoundException;
import com.carventure.webapp.model.ApiResponse;
import com.carventure.webapp.model.GenerateOtpRequest;
import com.carventure.webapp.model.User;
import com.carventure.webapp.model.VerifyOtpRequest;
import com.carventure.webapp.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users/")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("generateOtp")
    public ResponseEntity<ApiResponse> generateOTP(@Valid @RequestBody GenerateOtpRequest generateOtpRequest) {
        try {
            String phoneNumber = generateOtpRequest.getMobileNumber();
            logger.info("Generating OTP for phone number {}", phoneNumber);

            String message = userService.sendOtp(phoneNumber);
            logger.info(message);

            return ResponseEntity.ok(new ApiResponse(message, "success"));
        } catch (CooldownActiveException e) {
            logger.error("Cooldown active exception", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("Cooldown active. Please wait before retrying.", "failed", true));
        } catch (Exception e) {
            logger.error("An error occurred while generating OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("An error occurred while generating OTP. Please try again later.", "failed", true));
        }
    }

    @PostMapping("verifyOtp")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        String phoneNumber = verifyOtpRequest.getMobileNumber();
        String otpInput = verifyOtpRequest.getOtpInput();
        logger.info("Verifying OTP for phone number {}", phoneNumber);

        try {
            if (userService.verifyOtp(phoneNumber, otpInput)) {
                logger.info("OTP verification successful for phone number {}", phoneNumber);
                return ResponseEntity.ok(new ApiResponse("OTP verified successfully.", "success"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse("Invalid OTP", "failed", true));
            }
        } catch (InvalidOtpException e) {
            logger.error("Invalid OTP for phone number {}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Invalid OTP", "failed", true));
        } catch (OtpExpiredException e) {
            logger.error("OTP expired for phone number {}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("OTP has expired. Please request a new one.", "failed", true));
        } catch (UserNotFoundException e) {
            logger.error("User not found for phone number {}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("User not found", "failed", true));
        } catch (Exception e) {
            logger.error("An error occurred during OTP verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("An error occurred during OTP verification. Please try again later.", "failed", true));
        }
    }

    @PostMapping("register")
    public User registerUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("{email}")
    public User getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email);
    }
}
