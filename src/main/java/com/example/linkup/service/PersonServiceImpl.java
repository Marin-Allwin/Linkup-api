package com.example.linkup.service;

import com.example.linkup.collection.Person;
import com.example.linkup.exception.CustomException;
import com.example.linkup.handler.PersonService;
import com.example.linkup.model.dto.College;
import com.example.linkup.model.dto.School;
import com.example.linkup.model.dto.Work;
import com.example.linkup.model.request.*;
import com.example.linkup.model.response.AllUserResponse;
import com.example.linkup.model.response.RegisterResponse;
import com.example.linkup.repository.PersonRepository;
import com.example.linkup.util.JwtService;
import com.example.linkup.util.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;



    @Override
    public ResponseEntity<RegisterResponse> newUserSignUp(RegisterDto request) {

        try {
            Optional<Person> person1 = personRepository.findByEmail(request.getEmail());
            if (person1.isPresent()) {
                throw new CustomException("Email already exist try to login", HttpStatus.BAD_REQUEST);
            }
            else {
                Person person = Person.builder()
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .dob(request.getDob())
                        .gender(request.getGender())
                        .build();

                personRepository.save(person);

                String accessToken = jwtService.generateAccessToken(person);
                String refreshToken = jwtService.generateRefreshToken(person);

                storeAccessToken(request.getEmail(), refreshToken);

                RegisterResponse authResponse = new RegisterResponse(person, accessToken, refreshToken);

                return ResponseEntity.ok(authResponse);
            }

        } catch (Exception e){
            throw new CustomException(e.getMessage(),HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<RegisterResponse> loginUser(LoginDto loginRequest) {

        try{
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            var user = personRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow();

            var accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            storeAccessToken(loginRequest.getEmail(),refreshToken);

            RegisterResponse authResponse = new RegisterResponse(user, accessToken,refreshToken);
            return ResponseEntity.ok(authResponse);
        }catch (BadCredentialsException e) {
            throw new CustomException("Bad Credentials", HttpStatus.UNAUTHORIZED);
        }catch (Exception e) {
            e.printStackTrace();
            throw new CustomException("Bad Credentials",HttpStatus.NOT_FOUND);
        }

    }

    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUserName(refreshToken);
        if(userEmail != null) {
            var user = this.personRepository.findByEmail(userEmail).orElseThrow();

            if(jwtService.isTokenValid(refreshToken, user)){
                var accessToken = jwtService.generateAccessToken(user);

                RegisterResponse authResponse = new RegisterResponse(user, accessToken,refreshToken);
                new ObjectMapper().writeValue(response.getOutputStream(),authResponse);
            }
        }
    }

    @Override
    public ResponseEntity<String> findUserAccount(String email) {
//        System.out.println("Searching for email: " + email);

        Optional<Person> person = personRepository.findByEmail(email);

        if (person.isPresent()) {

            String otp = otpService.generateOtp();
            otpService.saveOtp(email, otp);

            System.out.println(otp+ " for " +email);

            return ResponseEntity.ok("Account exists");
        } else {
            throw new CustomException("No user found for this account",HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<String> VerifyUserAccount(VerifyAccountDto request) {
        String storedOtp = otpService.getOtp(request.getEmail());

        if (storedOtp != null && storedOtp.equals(request.getOtp().toString())) {
            return ResponseEntity.ok("Account verified successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP.");
        }
    }

    @Override
    public ResponseEntity<String> changeNewPassword(ChangePasswordDto request) {
        Optional<Person> person = personRepository.findByEmail(request.getEmail());

        Person person1 = person.get();

        person1.setPassword(passwordEncoder.encode(request.getNewPasword()));

        personRepository.save(person1);

        return ResponseEntity.ok("Password changed successfully");
    }

    @Override
    public ResponseEntity<Person> getPerson(String email) {
        try{
            Optional<Person> personOptional = personRepository.findByEmail(email);

            if(personOptional.isPresent()){

                Person person = personOptional.get();

                if(person.getProfile() != null){
                    String base64Profile = Base64.getEncoder().encodeToString(person.getProfile());
                    person.setProfileImg(base64Profile);
                }

                if(person.getCover() != null) {
                    String base64Cover = Base64.getEncoder().encodeToString(person.getCover());
                    person.setCoverImg(base64Cover);
                }
                return ResponseEntity.ok().body(person);
            }
            else {
                throw new RuntimeException();
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            throw new RuntimeException();
        }
    }

    @Override
    public ResponseEntity<String> updateProfilePic(MultipartFile image, String email) {
        try {

            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file type");
            }

            Optional<Person> person = personRepository.findByEmail(email);

            if(person.isPresent()){
                Person person1 = person.get();

                person1.setProfile(image.getBytes());
                personRepository.save(person1);

                return ResponseEntity.ok("profile Updated Successfully");
            }else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person not found");
            }
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public ResponseEntity<String> uploadCoverPic(MultipartFile image, String email) {
        try {
            Optional<Person> person = personRepository.findByEmail(email);

            if(person.isPresent()){
                Person person1 = person.get();

                person1.setCover(image.getBytes());
                personRepository.save(person1);

                return ResponseEntity.ok("Coverpic Updated Successfully");
            }else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Person not found");
            }
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public ResponseEntity<Person> updateDetails(Person request) {

        Optional<Person> optionalPerson = personRepository.findByEmail(request.getEmail());
        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        String status = request.getRelationShipStatus();
        List<School> schools = request.getSchools();
        List<College> colleges = request.getColleges();
        List<Work> works = request.getWorks();
        String gender = request.getGender();
        String phone = request.getPhoneNumber();
        String bio = request.getBio();

//        LocalDate dob = LocalDate.parse(request.getDob(), DateTimeFormatter.ISO_DATE);

        Person person = optionalPerson.get();

        if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
            person.setFirstName(firstName);
            person.setLastName(lastName);
        }
        if (status != null && !status.isEmpty() ) {
            person.setRelationShipStatus(status);
        }
        if (bio != null && !bio.isEmpty() ) {
            person.setBio(bio);
        }
        if (schools != null && !schools.isEmpty() ) {
            person.setSchools(schools);
        }
        if (colleges != null && !colleges.isEmpty() ) {
            person.setColleges(colleges);
        }
        if (works != null && !works.isEmpty() ) {
            person.setWorks(works);
        }
        if (gender != null && !gender.isEmpty() ) {
            person.setGender(gender);
        }
        if (phone != null && !phone.isEmpty()) {
            person.setPhoneNumber(phone);
        }

        if (request.getDob() != null) {
            person.setDob(request.getDob());
        }


        if (person.getProfile() != null && person.getProfile().length > 0) {
            String base64Profile = Base64.getEncoder().encodeToString(person.getProfile());
            person.setProfileImg(base64Profile);
        } else {
            person.setProfileImg(null);
        }

        if (person.getCover() != null && person.getCover().length > 0) {
            String base64Profile = Base64.getEncoder().encodeToString(person.getCover());
            person.setCoverImg(base64Profile);
        } else {
            person.setCoverImg(null);
        }

        personRepository.save(person);
        return ResponseEntity.ok(person);
    }

    @Override
    public ResponseEntity<String> logout(String email) {
        try{
            removeAccessToken(email);
            return ResponseEntity.ok("Logout Successfully");

        }catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Logout failed due to an error");
        }
    }

    @Override
    public List<AllUserResponse> getAllUser(String email) {
        return personRepository.getAllUserDetail()
                .stream()
                .filter(p -> !p.getEmail().equals(email))
                .map(p -> AllUserResponse.builder()
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .email(p.getEmail())
                        .bio(p.getBio())
                        .profile(p.getProfile() != null ? Base64.getEncoder().encodeToString(p.getProfile()) : null)  // Convert byte[] to Base64 String
                        .build())
                .collect(Collectors.toList());
    }

    private void removeAccessToken(String email) {
        redisTemplate.delete(email);
    }

    public void storeAccessToken(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(userId, refreshToken, 5, TimeUnit.DAYS);
    }

}
