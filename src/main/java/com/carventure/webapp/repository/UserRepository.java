package com.carventure.webapp.repository;

import com.carventure.webapp.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    User findByEmail(String email);
    User findByPhone(String phone);
}
