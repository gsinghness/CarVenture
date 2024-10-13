package com.carventure.webapp.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation definition
@Constraint(validatedBy = OtpRequestValidator.class)
@Target({ElementType.TYPE}) // Apply to class level
@Retention(RetentionPolicy.RUNTIME) // Make available at runtime
public @interface ValidOtpRequest {
    String message() default "Either mobile number or email must be provided.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
