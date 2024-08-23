package com.example.linkup.controller;

import com.example.linkup.collection.Person;
import com.example.linkup.handler.PersonService;
import com.example.linkup.model.request.ChangePasswordDto;
import com.example.linkup.model.request.LoginDto;
import com.example.linkup.model.request.RegisterDto;
import com.example.linkup.model.request.VerifyAccountDto;
import com.example.linkup.model.response.RegisterResponse;
import com.example.linkup.util.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/linkup")
@CrossOrigin("http://localhost:3000")
@RequiredArgsConstructor
public class AuthenticationController {

    private final PersonService personService;
    private final JwtService jwtService;

    @PostMapping("/sign-up")
    public ResponseEntity<RegisterResponse> singup(@RequestBody RegisterDto request){
        return personService.newUserSignUp(request);
    }

    @PostMapping("/login")
    public ResponseEntity<RegisterResponse> login(@RequestBody LoginDto loginRequest) {
        return personService.loginUser(loginRequest);
    }

    @PostMapping("/refresh-token")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        personService.refreshToken(request, response);
    }

    @PostMapping("/find-account")
    public ResponseEntity<String> findAccount(@RequestParam String email) {
        return personService.findUserAccount(email);
    }

    @PostMapping("/verify-account")
    public ResponseEntity<String> verifyAccount(@RequestBody VerifyAccountDto request) {
        return personService.VerifyUserAccount(request);
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordDto request) {
        return personService.changeNewPassword(request);
    }



}
