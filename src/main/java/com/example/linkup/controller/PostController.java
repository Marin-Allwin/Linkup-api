package com.example.linkup.controller;

import com.example.linkup.collection.Person;
import com.example.linkup.collection.Posts;
import com.example.linkup.config.JacksonConfig;
import com.example.linkup.handler.PersonService;
import com.example.linkup.handler.PostService;
import com.example.linkup.model.dto.PostComments;
import com.example.linkup.model.dto.SavedItems;
import com.example.linkup.model.request.NewComment;
import com.example.linkup.model.response.PostResponse;
import com.example.linkup.repository.PersonRepository;
import com.example.linkup.repository.PostRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/post")
@CrossOrigin("http://localhost:3000")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PersonService personService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;
    private final PersonRepository personRepository;

    @GetMapping("/get-all-post")
    public ResponseEntity<List<PostResponse>> getAllPost(){
        try{
            List<PostResponse> posts = postService.getAllPosts();
            return ResponseEntity.ok(posts);
        }
        catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
    }

    @GetMapping("/get-my-post")
    public ResponseEntity<List<Posts>> getMyPost(@RequestParam String email) {
        try {
            List<Posts> posts = postService.getAllMyPosts(email);
            return ResponseEntity.ok(posts);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
    }

    @PostMapping("/update-like/{userId}")
    public ResponseEntity<String> LikeOperation(@PathVariable("userId") String userId,@RequestParam String postId ) throws JsonProcessingException {
        ResponseEntity<String> response = postService.updateLike(userId, postId);

        if(response.getStatusCode().is2xxSuccessful()){
            ObjectId newId = new ObjectId(postId);
            Optional<Posts> posts = postRepository.findById(newId);

            String notificationMessage = objectMapper.writeValueAsString(posts.get());
            messagingTemplate.convertAndSend("/public/posts/likes", notificationMessage);
        }
        return response;
    }

    @PostMapping("/undo-like/{userId}")
    public ResponseEntity<String> undoLike(@PathVariable("userId")String userId,@RequestParam String postId) throws JsonProcessingException {
        ResponseEntity<String> response =  postService.undoLike(userId,postId);
        System.out.println("response received dislike");
        if(response.getStatusCode().is2xxSuccessful()){

            ObjectId newId = new ObjectId(postId);
            Optional<Posts> posts = postRepository.findById(newId);

            String notificationMessage = objectMapper.writeValueAsString(posts.get());
            messagingTemplate.convertAndSend("/public/posts/likes", notificationMessage);
            System.out.println("executed dislike");
        }
        return response;
    }

    @PostMapping("/post-comment")
    public ResponseEntity<String> postComment(@RequestBody NewComment request) {
        ResponseEntity<String> response = postService.postNewComment(request);

        if(response.getStatusCode().is2xxSuccessful()) {
            try {
                List<PostComments> commentsList = postService.getAllComments(request);
                String jsonResponse = objectMapper.writeValueAsString(commentsList);
                messagingTemplate.convertAndSend("/public/post-comments", jsonResponse);
            } catch (JsonProcessingException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error processing comments list");
            }
        }
        return response;
    }

    @GetMapping("/get-all-comments")
    public List<PostComments> getAllComments(@RequestParam String postId) {
        List<PostComments> commentsList= postService.getAllCommentsOfPost(postId);
        return commentsList;
    }

    @PostMapping("/save-post/{personId}")
    public ResponseEntity<String> savePost(@PathVariable("personId") String personId, @RequestParam("postId") String postId, Principal principal) throws JsonProcessingException {
       ResponseEntity<String> response = postService.savePost(personId,postId);

       Optional<Person> optionalPerson = personRepository.findById(new ObjectId(personId));
       String email = principal.getName();

       if(response.getStatusCode().is2xxSuccessful()) {
           SavedItems savedNew  =personService.getSavedpostList(personId,postId);
           String jsonResponse = objectMapper.writeValueAsString(savedNew);
           messagingTemplate.convertAndSendToUser(email, "/queue/savedpost", jsonResponse);
           System.out.println("inside");
       }else{
           System.out.println("outside");
           return (ResponseEntity<String>) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
       }
       return response;
    }

    @PostMapping("/unsave-post/{personId}")
    public ResponseEntity<String> unsavePost(@PathVariable("personId") String personId,@RequestParam("postId") String postId,Principal principal) throws JsonProcessingException {
        ResponseEntity<String> response = postService.unsavePost(personId, postId);

        if(response.getStatusCode().is2xxSuccessful()){
            Optional<Person> optionalPerson = personRepository.findById(new ObjectId(personId));
            String email = principal.getName();
            List<SavedItems> list = optionalPerson.get().getSaved();

            String newList = objectMapper.writeValueAsString(list);
            messagingTemplate.convertAndSendToUser(email,"/queue/unsavedpost", newList );
            System.out.println(newList);
        }
        return response;
    }
}
