package com.example.linkup.repository;

import com.example.linkup.collection.Person;
import com.example.linkup.model.response.AllUserResponse;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends MongoRepository<Person, ObjectId> {
    Optional<Person> findByEmail(String email);

    @Query(value = "{}", fields = "{firstName: 1, lastName: 1, email: 1,profile:1, bio:1}")
    List<Person> getAllUserDetail();
}
