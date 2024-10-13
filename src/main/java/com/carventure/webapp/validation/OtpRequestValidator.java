package com.carventure.webapp.validation;

import com.carventure.webapp.model.GenerateOtpRequest;
import com.carventure.webapp.model.VerifyOtpRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class OtpRequestValidator implements ConstraintValidator<ValidOtpRequest, Object> {

    @Override
    public void initialize(ValidOtpRequest constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        String mobileNumber = null;
        String email = null;

        if (obj instanceof GenerateOtpRequest) {
            GenerateOtpRequest request = (GenerateOtpRequest) obj;
            mobileNumber = request.getMobileNumber();
            email = request.getEmail();
        } else if (obj instanceof VerifyOtpRequest) {
            VerifyOtpRequest request = (VerifyOtpRequest) obj;
            mobileNumber = request.getMobileNumber();
            email = request.getEmail();
        }

        boolean isValid = (mobileNumber != null && !mobileNumber.isEmpty()) ||
                (email != null && !email.isEmpty());

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Either mobile number or email must be provided.")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
