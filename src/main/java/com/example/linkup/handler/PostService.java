package com.example.linkup.handler;

import com.example.linkup.collection.Posts;
import com.example.linkup.model.dto.PostComments;
import com.example.linkup.model.request.NewComment;
import com.example.linkup.model.response.PostResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PostService {
    List<Posts> getAllMyPosts(String email);

    List<PostResponse> getAllPosts();

    ResponseEntity<String> updateLike(String userId, String postId);

    ResponseEntity<String> undoLike(String userId, String postId);

    ResponseEntity<String> postNewComment(NewComment request);

    List<PostComments> getAllComments(NewComment request);

    List<PostComments> getAllCommentsOfPost(String postId);

    ResponseEntity<String> savePost(String personID, String postId);

    ResponseEntity<String> unsavePost(String personId, String postId);
}
