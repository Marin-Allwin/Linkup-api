package com.example.linkup.controller;

import com.example.linkup.collection.Person;
import com.example.linkup.collection.Posts;
import com.example.linkup.config.JacksonConfig;
import com.example.linkup.handler.PersonService;
import com.example.linkup.handler.PostService;
import com.example.linkup.model.dto.FriendRequestNotification;
import com.example.linkup.model.dto.NotificationMessage;
import com.example.linkup.model.request.NewPostRequest;
import com.example.linkup.model.request.PersonRequest;
import com.example.linkup.model.request.UpdateDetailsRequest;
import com.example.linkup.model.response.AllUserResponse;
import com.example.linkup.model.response.PostResponse;
import com.example.linkup.service.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/user")
@CrossOrigin("http://localhost:3000")
@RequiredArgsConstructor
public class UserController {

    private final PersonService personService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketService webSocketService;
    private final PostService postService;

    @GetMapping("/get-person/**")
    public ResponseEntity<Person> getPerson(@RequestParam String personId) {
        return ResponseEntity.ok(personService.getPerson(personId).getBody());
    }

    @PostMapping("/update-profile")
    public ResponseEntity<String> updatePrifile(@RequestPart("image") MultipartFile image, @RequestParam("email") String email){
        return ResponseEntity.ok(personService.updateProfilePic(image,email)).getBody();
    }

    @PostMapping("/upload-coverPic")
    public ResponseEntity<String> updateCoverPic(@RequestParam("image") MultipartFile image, @RequestParam("email") String email){
        return ResponseEntity.ok(personService.uploadCoverPic(image, email)).getBody();
    }

    @PostMapping("/update-details")
    public ResponseEntity<Person> updateDetails(@RequestBody PersonRequest request) {
        try {
            System.out.println(request);
            Person updatedPerson = personService.updateDetails(request).getBody();
            return ResponseEntity.ok(updatedPerson);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/logout/**")
    public ResponseEntity<String> logout(@RequestParam String email) {
        return personService.logout(email);
    }

    @GetMapping("/get-all-user")
    public List<AllUserResponse> getAllUser(@RequestParam String email){
        return personService.getAllUser(email);
    }

//    @PostMapping("/add-post")
//    public ResponseEntity<String> addPost(NewPostRequest request)throws IOException {
//
//        ResponseEntity<Posts> response = personService.addNewPost(request);
//
//        if (response.getStatusCode().is2xxSuccessful()) {
//            PostResponse response1 = personService.websocketPostResponse(response);
//            String notificationMessage = new ObjectMapper().writeValueAsString(response1);
//            messagingTemplate.convertAndSend("/public/posts", notificationMessage);
//        }
//
//        return ResponseEntity.ok("Post added successfully");
//    }

    @PostMapping("/add-post")
    public ResponseEntity<String> addPost(NewPostRequest request) throws IOException {
        ResponseEntity<Posts> response = personService.addNewPost(request);

        if (response.getStatusCode().is2xxSuccessful()) {
            PostResponse response1 = personService.websocketPostResponse(response);

            ObjectMapper mapper = JacksonConfig.objectMapper();

            String notificationMessage = mapper.writeValueAsString(response1);
            messagingTemplate.convertAndSend("/public/posts", notificationMessage);
            return ResponseEntity.ok("Post added successfully");
        }

        return ResponseEntity.status(response.getStatusCode()).body("Post creation failed");
    }


//    @PostMapping("/send-request/{from}")
//    public ResponseEntity<String> sendRequest(@PathVariable("from")String from, @RequestParam("to")String toPerson,Principal principal) {
//
//        ResponseEntity<String> response = personService.sentFriendRequest(from, toPerson);
//        if (response.getStatusCode().is2xxSuccessful()) {
//
//                try {
//                    NotificationMessage notification = new NotificationMessage(from, "You have a new friend request from " + from);
//                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//                    String username = authentication.getName();
//                    messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
//                }catch (Exception e){
//                    System.out.println(e.getMessage());
//                }
//        }else{
//            System.out.println("something went wrong");
//        }
//        return response;
//    }

    @PostMapping("/send-request/{from}")
    public ResponseEntity<String> sendRequest(@PathVariable("from") String from, @RequestParam("to") String toPerson, Principal principal) {
        ResponseEntity<String> response = personService.sentFriendRequest(from, toPerson);
        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                NotificationMessage notification = new NotificationMessage(from, "You have a new friend request from " + from);
                messagingTemplate.convertAndSendToUser(toPerson, "/queue/notifications", notification);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("something went wrong");
        }
        return response;
    }

    @PostMapping("/delete-send-request/{from}")
    public ResponseEntity<String> deleteSendRequest(@PathVariable("from")String from, @RequestParam("to")String toPerson) {
        return personService.deleteSendRequest(from, toPerson);
    }

    @PostMapping("/accept-request/{user}")
    public ResponseEntity<String> acceptRequest(@PathVariable("user")String user, @RequestParam("from")String from){
        ResponseEntity<String> response = personService.acceptFriendRequest(user, from);

        if(response.getStatusCode().is2xxSuccessful()){
            messagingTemplate.convertAndSendToUser(from, "/queue/acceptNotification", "notification");
        }
        return response;
    }

    @GetMapping("/request-received/**")
    public ResponseEntity<List<AllUserResponse>> getReceivedRequest(@RequestParam("email") String email) {
        return personService.getAllReceivedRequet(email);
    }

    @GetMapping("/get-my-friends")
    public ResponseEntity<List<AllUserResponse>> getMyFriends(@RequestParam String email){
        return personService.getMyFriends(email);
    }

    @PostMapping("/decline-request/{for}")
    public ResponseEntity<String> cancelRequest(@PathVariable("for") String user, @RequestParam("who") String who) {
        return personService.declineRequest(user, who);
    }

    @GetMapping("/get-few-friends/**")
    public ResponseEntity<List<AllUserResponse>> getFewFriends(@RequestParam String personId) {
        List<AllUserResponse> friends = personService.getFewFriends(personId);
        return ResponseEntity.ok(friends);  // Wrap the list in a ResponseEntity
    }

    @GetMapping("/get-suggession/{personId}")
    public ResponseEntity<List<AllUserResponse>> getSuggession(@PathVariable("personId") String personId) {
        List<AllUserResponse> suggessions = personService.getSuggession(personId);
        return ResponseEntity.ok(suggessions);
    }
}
