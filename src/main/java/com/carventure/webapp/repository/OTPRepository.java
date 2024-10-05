package com.carventure.webapp.repository;

import com.carventure.webapp.model.OTP;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OTPRepository extends MongoRepository<OTP, String> {

}

