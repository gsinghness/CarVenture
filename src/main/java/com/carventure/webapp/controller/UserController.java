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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.MapUtils;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("generatePhoneOtp")
    public ResponseEntity<ApiResponse> generatePhoneOTP(@Valid @RequestBody GenerateOtpRequest generateOtpRequest) {
        try {
            String phoneNumber = generateOtpRequest.getMobileNumber();
            logger.info("Generating OTP for phone number {}", phoneNumber);

            String message = userService.sendPhoneOtp(phoneNumber);
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

    @PostMapping("verifyPhoneOtp")
    public ResponseEntity<ApiResponse> verifyPhoneOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        String phoneNumber = verifyOtpRequest.getMobileNumber();
        String otpInput = verifyOtpRequest.getOtpInput();
        logger.info("Verifying OTP for phone number {}", phoneNumber);

        try {
            if (userService.verifyPhoneOtp(phoneNumber, otpInput)) {
                logger.info("OTP verification successful for phone number {}", phoneNumber);
                // Generate a JWT token
                String token = userService.generateToken(phoneNumber);
                ResponseCookie responseCookie = ResponseCookie.from("token", token)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(60L * 60 * 24)
                        .build();

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                        .body(new ApiResponse("OTP verified successfully.", "success"));
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

    @PostMapping("generateEmailOtp")
    public ResponseEntity<ApiResponse> generateEmailOTP(@Valid @RequestBody GenerateOtpRequest generateOtpRequest, @CookieValue("token") String token) {
        try {
            String email = generateOtpRequest.getEmail();  // Assuming GenerateOtpRequest now has an 'email' field
            logger.info("Generating OTP for email {}", email);

            String userPhoneNumber = userService.extractPhoneNumberFromToken(token);
            String message = userService.sendEmailOtp(email, userPhoneNumber);
            logger.info(message);

            return ResponseEntity.ok(new ApiResponse(message, "success"));
        } catch (CooldownActiveException e) {
            logger.error("Cooldown active exception", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("Cooldown active. Please wait before retrying.", "failed", true));
        } catch (UserNotFoundException e) {
            logger.error("User not found. {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("User not found", "failed", true));
        } catch (Exception e) {
            logger.error("An error occurred while generating OTP for email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("An error occurred while generating OTP for email. Please try again later.", "failed", true));
        }
    }

    @PostMapping("verifyEmailOtp")
    public ResponseEntity<ApiResponse> verifyEmailOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest, @CookieValue("token") String token) {
        String email = verifyOtpRequest.getEmail();
        String otpInput = verifyOtpRequest.getOtpInput();
        logger.info("Verifying OTP for Email {}", email);

        try {
            String userPhoneNumber = userService.extractPhoneNumberFromToken(token);
            if (userService.verifyEmailOTP(email, otpInput, userPhoneNumber)) {
                logger.info("OTP verification successful for email {}", email);

                return ResponseEntity.ok().body(new ApiResponse("OTP verified successfully.", "success"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse("Invalid OTP", "failed", true));
            }
        } catch (InvalidOtpException e) {
            logger.error("Invalid OTP for email {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Invalid OTP", "failed", true));
        } catch (OtpExpiredException e) {
            logger.error("OTP expired for email {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("OTP has expired. Please request a new one.", "failed", true));
        } catch (UserNotFoundException e) {
            logger.error("User not found for email {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("User not found", "failed", true));
        } catch (Exception e) {
            logger.error("An error occurred during OTP verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("An error occurred during OTP verification. Please try again later.", "failed", true));
        }
    }

    @PostMapping("preferredName")
    public ResponseEntity<ApiResponse> setPreferredName(@RequestBody Map<String, String> request, @CookieValue("token") String token) {
        String userPhoneNumber = userService.extractPhoneNumberFromToken(token);
        if (!userService.userExists(userPhoneNumber)) {
            logger.error("No user exists with phone number {}", userPhoneNumber);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("User not found with phone number " + userPhoneNumber, "failed", true));
        } else if (MapUtils.isEmpty(request)) {
            logger.error("Empty request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Request body cannot be empty", "failed"));
        } else if (!request.containsKey("preferredName")) {
            logger.error("Invalid request, send a 'preferredName' request. Current value received: {}", request.entrySet());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Invalid request, send a valid 'preferred name' request", "failed", true));
        } else if (StringUtils.isBlank(request.get("preferredName"))) {
            logger.error("Invalid request, send a value for key 'preferredName' in the request. Current value received: {}", request.entrySet());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Invalid request, send a valid 'preferred name' request", "failed", true));
        }
        String preferredName = request.get("preferredName");
        logger.info("Preferred name is {}", preferredName);
        if (userService.setPreferredName(userPhoneNumber, preferredName)) {
            logger.info("Successfully set preferred name to {}", preferredName);
            return ResponseEntity.ok().body(new ApiResponse("Preferred name set successfully", "success"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Preferred name not set", "failed", true));
        }
    }

}
