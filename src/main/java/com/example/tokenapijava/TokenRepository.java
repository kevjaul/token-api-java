package com.example.tokenapijava;

import org.springframework.data.repository.CrudRepository;

import com.example.tokenapijava.Schemas.UserTokenSchema;

public interface TokenRepository extends CrudRepository<UserTokenSchema, Long> {

}
