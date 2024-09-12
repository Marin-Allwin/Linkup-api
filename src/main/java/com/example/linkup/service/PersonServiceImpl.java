package com.example.linkup.service;

import com.example.linkup.collection.Friends;
import com.example.linkup.collection.Person;
import com.example.linkup.collection.Posts;
import com.example.linkup.exception.CustomException;
import com.example.linkup.handler.PersonService;
import com.example.linkup.handler.PostService;
import com.example.linkup.model.RequestReceive;
import com.example.linkup.model.RequestSent;
import com.example.linkup.model.dto.College;
import com.example.linkup.model.dto.SavedItems;
import com.example.linkup.model.dto.School;
import com.example.linkup.model.dto.Work;
import com.example.linkup.model.request.*;
import com.example.linkup.model.response.AllUserResponse;
import com.example.linkup.model.response.PostResponse;
import com.example.linkup.model.response.RefreshTokenResponse;
import com.example.linkup.model.response.RegisterResponse;
import com.example.linkup.repository.PersonRepository;
import com.example.linkup.repository.PostRepository;
import com.example.linkup.util.JwtService;
import com.example.linkup.util.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//import static java.util.stream.Nodes.collect;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final PostService postService;
    private final PostRepository postRepository;

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

//                RegisterResponse authResponse = new RegisterResponse(user, accessToken,refreshToken);

                var refeshResponse = RefreshTokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();

                new ObjectMapper().writeValue(response.getOutputStream(),refeshResponse);
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

//            System.out.println(otp+ " for " +email);

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
            Optional<Person> personOptional = personRepository.findById(new ObjectId(email));

            if(personOptional.isPresent()){

                Person person = personOptional.get();
//                System.out.println(person);

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
//            System.out.println(e.getMessage());
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
    public ResponseEntity<Person> updateDetails(PersonRequest request) {

        System.out.println(request);

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
//        ObjectId personId = new ObjectId(String.valueOf(request.getPersonId()));

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

//    @Override
//    public List<AllUserResponse> getAllUser(String email) {
//
//        Optional<Person> optionalPerson = personRepository.findByEmail(email);
//        Person person = optionalPerson.get();
//
//        return personRepository.getAllUserDetail()
//                .stream()
//                .filter(p -> !p.getEmail().equals(email))
//                .filter(p ->)
//                .map(p -> AllUserResponse.builder()
//                        .id(p.getPersonId())
//                        .firstName(p.getFirstName())
//                        .lastName(p.getLastName())
//                        .email(p.getEmail())
//                        .bio(p.getBio())
//                        .profile(p.getProfile() != null ? Base64.getEncoder().encodeToString(p.getProfile()) : null)
//                        .build())
//                .collect(Collectors.toList());
//    }

    @Override
    public List<AllUserResponse> getAllUser(String email) {
        // Fetch the current user
        Optional<Person> optionalPerson = personRepository.findByEmail(email);
        if (optionalPerson.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        Person currentUser = optionalPerson.get();

        List<Person> allUsers = personRepository.getAllUserDetail();

        List<ObjectId> friendsList = currentUser.getFriendsList() != null ?
                currentUser.getFriendsList().stream().map(Friends::getPersonId).collect(Collectors.toList()) :
                Collections.emptyList();
        List<ObjectId> sentRequests = currentUser.getSent() != null ?
                currentUser.getSent().stream().map(RequestSent::getSentPersonId).collect(Collectors.toList()) :
                Collections.emptyList();
        List<ObjectId> receivedRequests = currentUser.getReceived() != null ?
                currentUser.getReceived().stream().map(RequestReceive::getReceivedPersonId).collect(Collectors.toList()) :
                Collections.emptyList();

        return allUsers.stream()
                .filter(user -> !user.getEmail().equals(email))
                .filter(user -> friendsList.isEmpty() || !friendsList.contains(user.getPersonId()))
//                .filter(user -> sentRequests.isEmpty() || !sentRequests.contains(user.getPersonId()))
                .filter(user -> receivedRequests.isEmpty() || !receivedRequests.contains(user.getPersonId()))
                .map(user -> AllUserResponse.builder()
                        .personId(String.valueOf(user.getPersonId()))
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .bio(user.getBio())
                        .profile(user.getProfile() != null ? Base64.getEncoder().encodeToString(user.getProfile()) : null)
                        .friendshipStatus(sentRequests.stream().anyMatch(id -> id.equals(user.getPersonId()))? "sent" : null)
                        .build())
                .collect(Collectors.toList());
    }

//    @Override
//    public ResponseEntity<Posts> addNewPost(NewPostRequest request) throws IOException {
//
//        Optional<Person> optionalPerson = personRepository.findByEmail(request.getEmail());
//        if (optionalPerson.isPresent()) {
//            ObjectId id = optionalPerson.get().getPersonId();
//            Posts posts = new Posts();
//            posts.setContent(request.getContent());
//            posts.setPersonId(id);
//            posts.setPostTime(new Date());
//
//            if(request.getPostImage() != null) {
//                posts.setPostImage(request.getPostImage().getBytes());
//            }
//            postRepository.save(posts);
//            return ResponseEntity.ok(posts);
//        } else {
//            return null;
//        }
//    }


    @Override
    public ResponseEntity<Posts> addNewPost(NewPostRequest request) throws IOException {
        Optional<Person> optionalPerson = personRepository.findByEmail(request.getEmail());

        if (optionalPerson.isPresent()) {
            ObjectId id = optionalPerson.get().getPersonId();
            Posts posts = new Posts();
            posts.setContent(request.getContent());
            posts.setPersonId(id);
            posts.setPostTime(new Date());

            if(request.getPostImage() != null) {
                posts.setPostImage(request.getPostImage().getBytes());
            }

            postRepository.save(posts);
            return ResponseEntity.ok(posts);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Return 404 if user not found
        }
    }


    @Override
    public ResponseEntity<String> sentFriendRequest(String from, String toPerson) {
        Optional<Person> byId = personRepository.findByEmail(from);
        Optional<Person> byId1 = personRepository.findByEmail(toPerson);

        try{
            if(byId.isPresent()) {
                Person person =byId.get();
                RequestSent sent = RequestSent.builder()
                        .sentPersonId(byId1.get().getPersonId())
                        .sentDate(new Date())
                        .build();

                List<RequestSent> sentRequests = person.getSent();
                if (sentRequests == null) {
                    sentRequests = new ArrayList<>();
                }
                sentRequests.add(sent);
                person.setSent(sentRequests);
                personRepository.save(person);

                Person person1 = byId1.get();
//                System.out.println(person1.getFirstName());
                RequestReceive received = RequestReceive.builder()
                        .receivedPersonId(person.getPersonId())
                        .receivedDate(new Date())
                        .build();

                List<RequestReceive> receivedRequest = person1.getReceived();
                if (receivedRequest == null) {
                    receivedRequest = new ArrayList<>();
                }
                receivedRequest.add(received);
                person1.setReceived(receivedRequest);
                personRepository.save(person1);

                return ResponseEntity.ok("Request sent successfully");
            }
            else{
                throw new RuntimeException("One or both users not found");
            }
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send friend request: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<List<AllUserResponse>> getAllReceivedRequet(String email) {

        Optional<Person> optionalPerson = personRepository.findByEmail(email);
        Optional<Person> optionalPerson1;
        try{

            if(optionalPerson.isPresent()){

                List<RequestReceive> received1 = optionalPerson.get().getReceived();
                if (received1 == null) {
                    return  null;
                }
                List<AllUserResponse> list = received1.stream()
                        .map(r -> {
                            Optional<Person> optionalPerson2 = personRepository.findById(r.getReceivedPersonId());
                            if (optionalPerson2.isPresent()) {
                                Person receivedPerson = optionalPerson2.get();
                                return AllUserResponse.builder()
                                        .personId(String.valueOf(receivedPerson.getPersonId()))
                                        .firstName(receivedPerson.getFirstName())
                                        .lastName(receivedPerson.getLastName())
                                        .bio(receivedPerson.getBio())
                                        .email(receivedPerson.getEmail())
                                        .profile(receivedPerson.getProfile() != null ? Base64.getEncoder().encodeToString(receivedPerson.getProfile()) : null)
                                        .build();
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                return ResponseEntity.ok(list);
            } else{
                throw new RuntimeException();
            }
        }catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    public ResponseEntity<String> deleteSendRequest(String from, String toPerson) {

        Optional<Person> byEmail = personRepository.findByEmail(from);
        Optional<Person> byEmail1 = personRepository.findByEmail(toPerson);

        try{
            if(byEmail.isPresent() && byEmail1.isPresent()){
                Person person = byEmail.get();
                Person person1 = byEmail1.get();

                person.setSent(person.getSent().stream()
                        .filter(p -> !p.getSentPersonId().equals(person1.getPersonId()))
                        .collect(Collectors.toList())
                );
                personRepository.save(person);

                person1.setReceived(person1.getReceived().stream()
                        .filter(p -> !p.getReceivedPersonId().equals(person.getPersonId()))
                        .collect(Collectors.toList())
                );
                personRepository.save(person1);


            }
            return ResponseEntity.ok("Sent Request Canceled");
        } catch (Exception e){
            throw new RuntimeException();
        }
    }

    @Override
    public ResponseEntity<String> acceptFriendRequest(String user, String from) {

        Optional<Person> optionalPerson = personRepository.findByEmail(user);
        Optional<Person> optionalPerson1 = personRepository.findByEmail(from);

       try{
            if (optionalPerson.isPresent() && optionalPerson1.isPresent()) {
                Person person = optionalPerson.get();
                Person person1 = optionalPerson1.get();

                Friends friendsForPerson = Friends.builder()
                        .personId(person1.getPersonId())
                        .date(new Date())
                        .build();

                List<Friends> personFriendsList = person.getFriendsList();
                if (personFriendsList == null) {
                    personFriendsList = new ArrayList<>();
                }
                personFriendsList.add(friendsForPerson);
                person.setFriendsList(personFriendsList);

                List<RequestReceive> requestReceives = person.getReceived().stream().filter(r->!person1.getPersonId().equals(r.getReceivedPersonId())).collect(Collectors.toList());
                person.setReceived(requestReceives);

                personRepository.save(person);


                Friends friendsForPerson1 = Friends.builder()
                        .personId(person.getPersonId())
                        .date(new Date())
                        .build();

                List<Friends> person1FriendsList = person1.getFriendsList();
                if (person1FriendsList == null) {
                    person1FriendsList = new ArrayList<>();
                }
                person1FriendsList.add(friendsForPerson1);
                person1.setFriendsList(person1FriendsList);

                List<RequestSent> sents = person1.getSent().stream().filter(s-> !person.getPersonId().equals(s.getSentPersonId())).collect(Collectors.toList());
                person1.setSent(sents);
                personRepository.save(person1);
            }

            return ResponseEntity.ok("Request Accepted both are friends now");
        }catch (Exception e){
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to accept friend request: " + e.getMessage());
       }
    }

    @Override
    public ResponseEntity<List<AllUserResponse>> getMyFriends(String email) {
        try {
            Optional<Person> optionalPerson = personRepository.findByEmail(email);

            if (optionalPerson.isPresent()) {
                Person person = optionalPerson.get();
                List<Friends> friendsList = person.getFriendsList();

                if (friendsList == null || friendsList.isEmpty()) {
                    return ResponseEntity.ok(Collections.emptyList());
                }

                List<AllUserResponse> list = friendsList.stream()
                        .map(r -> {
                            Optional<Person> optionalPerson2 = personRepository.findById(r.getPersonId());
                            if (optionalPerson2.isPresent()) {
                                Person friend = optionalPerson2.get();
                                return AllUserResponse.builder()
                                        .personId(String.valueOf(friend.getPersonId()))
                                        .firstName(friend.getFirstName())
                                        .lastName(friend.getLastName())
                                        .bio(friend.getBio())
                                        .email(friend.getEmail())
                                        .profile(friend.getProfile() != null ? Base64.getEncoder().encodeToString(friend.getProfile()) : null)
                                        .build();
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                return ResponseEntity.ok(list);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @Override
    public PostResponse websocketPostResponse(ResponseEntity<Posts> post) throws IOException {
        Optional<Person> optionalPerson = personRepository.findById(post.getBody().getPersonId());

        if(optionalPerson.isPresent()) {
            Person person = optionalPerson.get();

            PostResponse postResponse = new PostResponse();
            postResponse.setPersonId(person.getPersonId());
            postResponse.setFirstName(person.getFirstName());
            postResponse.setLastName(person.getLastName());
            postResponse.setContent(post.getBody().getContent());
            postResponse.setPostTime(post.getBody().getPostTime());
            postResponse.setEmail(person.getEmail());
            postResponse.setPostId(post.getBody().getPostId());


            try{
                if (post.getBody().getPostImage() != null && post.getBody().getPostImage().length > 0) {
                    postResponse.setImage(Base64.getEncoder().encodeToString(post.getBody().getPostImage()));
                } else {
                    postResponse.setImage(null);
                }
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }

            if (person.getProfile() != null && person.getProfile().length > 0) {
                postResponse.setProfile(Base64.getEncoder().encodeToString(person.getProfile()));
            }else {
                postResponse.setProfile(null);
            }

//            System.out.println(postResponse);
            return postResponse;


        }else {
            return null;
        }
    }

    @Override
    public SavedItems getSavedpostList(String personID, String postId) {
        Person person = personRepository.findById(new ObjectId(personID)).orElseThrow(() -> new RuntimeException());
        List<SavedItems> saved = person.getSaved();
        if(saved != null) {
            SavedItems collect = saved.stream()
                    .filter(s -> s.getPostId().equals(new ObjectId(postId))).findFirst().orElseThrow(()-> new CustomException("not found", HttpStatus.NOT_FOUND));
            System.out.println("WERTYUI"+collect);
            return collect;
        }else{
            throw new RuntimeException();
        }


    }

    @Override
    public List<AllUserResponse> getFewFriends(String personId) {
        try{
            Person person = personRepository.findById(new ObjectId(personId)).orElseThrow(()-> new RuntimeException());

            List<Friends> list= person.getFriendsList();
            List<AllUserResponse> allUserResponses = new ArrayList<>();

            if(list == null) {
                return allUserResponses;
            }else{
                Collections.shuffle(list);
                allUserResponses = list.stream().limit(3).map(r -> {
                    Optional<Person> optionalPerson2 = personRepository.findById(r.getPersonId());
                        Person friend = optionalPerson2.get();
                        return AllUserResponse.builder()
                                .personId(String.valueOf(friend.getPersonId()))
                                .firstName(friend.getFirstName())
                                .lastName(friend.getLastName())
                                .bio(friend.getBio())
                                .email(friend.getEmail())
                                .profile(friend.getProfile() != null ? Base64.getEncoder().encodeToString(friend.getProfile()) : null)
                                .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            }
            return allUserResponses;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AllUserResponse> getSuggession(String personId) {

        Person person = personRepository.findById(new ObjectId(personId)).orElseThrow(()-> new RuntimeException());

        List<ObjectId> friendsList = person.getFriendsList()
                .stream().map(Friends::getPersonId).collect(Collectors.toList());

        List<ObjectId> allPersonList = personRepository.findAll()
                .stream().map(Person::getPersonId)
                .filter(p-> !new ObjectId(personId).equals(p)).collect(Collectors.toList());

        List<ObjectId> suggestedIds = allPersonList
                .stream().filter(p-> !friendsList.contains(p)).collect(Collectors.toList());
        List<AllUserResponse> responses = new ArrayList<>();

        if(friendsList == null){
            return responses;
        }else{
            Collections.shuffle(suggestedIds);
            responses = suggestedIds.stream().limit(3).map(r -> {
                        Optional<Person> optionalPerson2 = personRepository.findById(r);
                        Person friend = optionalPerson2.get();
                        return AllUserResponse.builder()
                                .personId(String.valueOf(friend.getPersonId()))
                                .firstName(friend.getFirstName())
                                .lastName(friend.getLastName())
                                .bio(friend.getBio())
                                .email(friend.getEmail())
                                .profile(friend.getProfile() != null ? Base64.getEncoder().encodeToString(friend.getProfile()) : null)
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return responses;
    }

    @Override
    public ResponseEntity<String> declineRequest(String user, String who) {
        try{
            Optional<Person> optionalPerson1 = personRepository.findByEmail(user);
            Optional<Person> optionalPerson2 = personRepository.findByEmail(who);

            if(optionalPerson1.isPresent() && optionalPerson2.isPresent()){
                Person person1 = optionalPerson1.get();
                Person person2 = optionalPerson2.get();

                person1.setReceived(person1.getReceived().stream()
                        .filter(p-> !p.getReceivedPersonId().equals(person2.getPersonId()))
                        .collect(Collectors.toList()));

                personRepository.save(person1);

                person2.setSent(person2.getSent().stream()
                        .filter(p-> !p.getSentPersonId().equals(person1.getPersonId()))
                        .collect(Collectors.toList()));

                personRepository.save(person2);
                System.out.println("everything working finr");
            }
            else {
                System.out.println("some error");
                throw new RuntimeException();
            }

            return ResponseEntity.ok("Request declined successfully");

        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    private void removeAccessToken(String email) {
        redisTemplate.delete(email);
    }

    public void storeAccessToken(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(userId, refreshToken, 5, TimeUnit.DAYS);
    }

}
