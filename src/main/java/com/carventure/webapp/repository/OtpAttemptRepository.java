package com.carventure.webapp.repository;

import com.carventure.webapp.model.OtpAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OtpAttemptRepository extends MongoRepository<OtpAttempt, String> {

}
