package com.example.linkup.controller;

import com.example.linkup.collection.Person;
import com.example.linkup.handler.PersonService;
import com.example.linkup.model.request.UpdateDetailsRequest;
import com.example.linkup.model.response.AllUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user")
@CrossOrigin("http://localhost:3000")
@RequiredArgsConstructor
public class UserController {

    private final PersonService personService;

    @GetMapping("/get-person/**")
    public ResponseEntity<Person> getPerson(@RequestParam String email) {
        return ResponseEntity.ok(personService.getPerson(email).getBody());
    }

    @PostMapping("/update-profile")
    public ResponseEntity<String> updatePrifile(@RequestPart("image") MultipartFile image, @RequestParam("email") String email){
        return ResponseEntity.ok(personService.updateProfilePic(image,email)).getBody();
    }

    @PostMapping("/upload-coverPic")
    public ResponseEntity<String> updateCoverPic(@RequestParam("image") MultipartFile image, @RequestParam("email") String email){
        return ResponseEntity.ok(personService.uploadCoverPic(image, email)).getBody();
    }

    @PostMapping("/update-datails")
    public ResponseEntity<Person> updateDetails(@RequestBody Person request) {
        return ResponseEntity.ok(personService.updateDetails(request)).getBody();
    }

    @PostMapping("/logout/**")
    public ResponseEntity<String> logout(@RequestParam String email) {
        return personService.logout(email);
    }

    @GetMapping("/get-all-user")
    public List<AllUserResponse> getAllUser(@RequestParam String email){
        return personService.getAllUser(email);
    }

}
