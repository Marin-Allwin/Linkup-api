package com.example.linkup.handler;

import com.example.linkup.collection.Person;
import com.example.linkup.collection.Posts;
import com.example.linkup.model.dto.SavedItems;
import com.example.linkup.model.request.*;
import com.example.linkup.model.response.AllUserResponse;
import com.example.linkup.model.response.PostResponse;
import com.example.linkup.model.response.RegisterResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PersonService {
    ResponseEntity<String> declineRequest(String user, String who);

    ResponseEntity<RegisterResponse> newUserSignUp(RegisterDto person);

    ResponseEntity<RegisterResponse> loginUser(LoginDto loginRequest);

    void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException;

    ResponseEntity<String> findUserAccount(String email);

    ResponseEntity<String> VerifyUserAccount(VerifyAccountDto request);

    ResponseEntity<String> changeNewPassword(ChangePasswordDto request);

    ResponseEntity<Person> getPerson(String personId);

    ResponseEntity<String> updateProfilePic(MultipartFile image, String email);

    ResponseEntity<String> uploadCoverPic(MultipartFile image, String email);

    ResponseEntity<Person> updateDetails(PersonRequest request);

    ResponseEntity<String> logout(String email);

    List<AllUserResponse> getAllUser(String email);

    ResponseEntity<Posts> addNewPost(NewPostRequest request) throws IOException;

    ResponseEntity<String> sentFriendRequest(String from, String toPerson);

    ResponseEntity<List<AllUserResponse>> getAllReceivedRequet(String email);

    ResponseEntity<String> deleteSendRequest(String from, String toPerson);

    ResponseEntity<String> acceptFriendRequest(String user, String from);

    ResponseEntity<List<AllUserResponse>> getMyFriends(String email);

    PostResponse websocketPostResponse(ResponseEntity<Posts> request) throws IOException;

    SavedItems getSavedpostList(String personID, String postId);

    List<AllUserResponse> getFewFriends(String personId);

    List<AllUserResponse> getSuggession(String personId);
}
