package com.example.linkup.repository;

import com.example.linkup.collection.Posts;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends MongoRepository<Posts, ObjectId> {
    List<Posts> findAllByPersonId(ObjectId id);
}
