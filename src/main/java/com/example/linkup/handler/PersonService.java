package com.example.linkup.handler;

import com.example.linkup.collection.Person;
import com.example.linkup.model.request.*;
import com.example.linkup.model.response.AllUserResponse;
import com.example.linkup.model.response.RegisterResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PersonService {
    ResponseEntity<RegisterResponse> newUserSignUp(RegisterDto person);

    ResponseEntity<RegisterResponse> loginUser(LoginDto loginRequest);

    void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException;

    ResponseEntity<String> findUserAccount(String email);

    ResponseEntity<String> VerifyUserAccount(VerifyAccountDto request);

    ResponseEntity<String> changeNewPassword(ChangePasswordDto request);

    ResponseEntity<Person> getPerson(String email);

    ResponseEntity<String> updateProfilePic(MultipartFile image, String email);

    ResponseEntity<String> uploadCoverPic(MultipartFile image, String email);

    ResponseEntity<Person> updateDetails(Person request);

    ResponseEntity<String> logout(String email);

    List<AllUserResponse> getAllUser(String email);
}
