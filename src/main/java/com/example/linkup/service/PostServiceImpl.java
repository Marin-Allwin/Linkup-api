package com.example.linkup.service;

import com.example.linkup.collection.Person;
import com.example.linkup.collection.Posts;
import com.example.linkup.exception.CustomException;
import com.example.linkup.handler.PostService;
import com.example.linkup.model.dto.PostComments;
import com.example.linkup.model.dto.PostLikes;
import com.example.linkup.model.dto.SavedItems;
import com.example.linkup.model.request.NewComment;
import com.example.linkup.model.response.PostResponse;
import com.example.linkup.repository.PersonRepository;
import com.example.linkup.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

//import static java.util.stream.Nodes.collect;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {


    private final PersonRepository personRepository;
    private final PostRepository postRepository;

    @Override
    public List<Posts> getAllMyPosts(String email) {
        Optional<Person> optionalPerson = personRepository.findByEmail(email);

        if (!optionalPerson.isPresent()) {
            throw new RuntimeException("Person not found for email: " + email);
        }

        ObjectId id = optionalPerson.get().getPersonId();

        try {

            List<Posts> posts = postRepository.findAllByPersonId(id).stream().map(post -> {
                if (post.getPostImage() != null) {
                    post.setImage(Base64.getEncoder().encodeToString(post.getPostImage()));
                }
                return post;
            }).collect(Collectors.toList());


            System.out.println(posts.stream().count());
            return posts;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching posts for person with id: " + id, e);
        }
    }

    @Override
    public List<PostResponse> getAllPosts() {

        try {
            List<Posts> posts = postRepository.findAll();

            List<PostResponse> collect = posts.stream().map(posts1 -> {
                PostResponse postResponse = new PostResponse();
                Person person = personRepository.findById(posts1.getPersonId()).get();
                postResponse.setEmail(person.getEmail());
                postResponse.setContent(posts1.getContent());
                postResponse.setPostTime(posts1.getPostTime());
                postResponse.setFirstName(person.getFirstName());
                postResponse.setLastName(person.getLastName());
                postResponse.setPersonId(person.getPersonId());
                postResponse.setPostId(posts1.getPostId());

                if (posts1.getPostImage() != null && posts1.getPostImage().length > 0) {
                    postResponse.setImage(Base64.getEncoder().encodeToString(posts1.getPostImage()));
                } else {
                    postResponse.setImage(null);
                }

                if (person.getProfile() != null && person.getProfile().length > 0) {
                    postResponse.setProfile(Base64.getEncoder().encodeToString(person.getProfile()));
                }else {
                    postResponse.setProfile(null);
                }

                if(posts1.getLikes() != null) {
                    postResponse.setLikes(posts1.getLikes());
                }

                if(posts1.getComments() != null) {
                    postResponse.setComments(posts1.getComments());
                }


                return postResponse;
            }).sorted(Comparator.comparing(PostResponse::getPostTime).reversed())
                    .collect(Collectors.toList());

            return collect;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Error fetching posts");
        }
    }

    @Override
    public ResponseEntity<String> updateLike(String userId, String postId) {
        try{
            ObjectId id = new ObjectId(postId);
            ObjectId id1 = new ObjectId(userId);

            Optional<Posts> optionalPosts = postRepository.findById(id);
            if (optionalPosts.isPresent()) {
                Posts posts = optionalPosts.get();
                List<PostLikes> postLikes= posts.getLikes();

                if (postLikes == null) {
                    postLikes = new ArrayList<>();
                }

                PostLikes like = PostLikes.builder()
                        .personId(id1)
                        .postId(id)
                        .likedDate(new Date())
                        .build();

                postLikes.add(like);
                posts.setLikes(postLikes);
                postRepository.save(posts);
                System.out.println(posts.getLikes());

                return ResponseEntity.ok("this is ok");
            }
            else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<String> undoLike(String userId, String postId) {
        try {
            ObjectId postObjectId  = new ObjectId(postId);
            ObjectId userObjectId  = new ObjectId(userId);

            Optional<Posts> optionalPosts = postRepository.findById(postObjectId);
            if (optionalPosts.isPresent()) {
                Posts posts = optionalPosts.get();
                List<PostLikes> postLikes = posts.getLikes();

                if (postLikes != null && !postLikes.isEmpty()) {
                    List<PostLikes> updatedLikes = postLikes.stream()
                            .filter(like -> !userObjectId.equals(like.getPersonId()))
                            .collect(Collectors.toList());

                    posts.setLikes(updatedLikes);
                        postRepository.save(posts);

                        return ResponseEntity.ok("Unliked the post successfully");
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body("No likes on this post to remove");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<String> postNewComment(NewComment request) {
        try {
            Optional<Posts> optionalPosts = postRepository.findById(new ObjectId(request.getPostId()));
            if (optionalPosts.isPresent()) {
                Posts posts = optionalPosts.get();
                List<PostComments> postComments = posts.getComments();

                if (postComments == null) {
                    postComments = new ArrayList<>();
                }
                PostComments comment = new PostComments();
                comment.setPersonId(new ObjectId(request.getPersonId()));
                comment.setComment(request.getComment());
                comment.setCommentDate(new Date());

                postComments.add(comment);
                posts.setComments(postComments);
                postRepository.save(posts);

                return ResponseEntity.ok("comment added successfully");
            }
            else{
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("post does not exist");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PostComments> getAllComments(NewComment request) {
        try {
            Optional<Posts> optionalPosts = postRepository.findById(new ObjectId(request.getPostId()));
            Optional<Person> optionalPerson = personRepository.findById(new ObjectId(request.getPersonId()));

            if (optionalPosts.isPresent() && optionalPerson.isPresent()) {
                Posts posts = optionalPosts.get();
                Person person = optionalPerson.get();
                List<PostComments> postComments = posts.getComments();

                if (postComments == null || postComments.isEmpty()) {
                    postComments = new ArrayList<>();
                }

                postComments = postComments.stream()
                        .map(c -> {
                            c.setFirstName(person.getFirstName());
                            c.setLastName(person.getLastName());
                            c.setPersonImage(Base64.getEncoder().encodeToString(person.getProfile()));
                            return c;
                        })
                        .collect(Collectors.toList()).reversed();

                return postComments;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post or Person not found");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PostComments> getAllCommentsOfPost(String postId) {

        try{
            Posts optionalPosts = postRepository.findById(new ObjectId(postId)).orElseThrow(() -> new CustomException("Post not found",HttpStatus.NOT_FOUND));
                Posts posts = optionalPosts;
                List<PostComments> postComments = posts.getComments();

                if (postComments == null || postComments.isEmpty()) {
                    postComments = new ArrayList<>();
                }

                postComments = postComments.stream()
                        .map(c -> {
                            Optional<Person> person = personRepository.findById(posts.getPersonId());
                            c.setFirstName(person.get().getFirstName());
                            c.setLastName(person.get().getLastName());
                            c.setPersonImage(Base64.getEncoder().encodeToString(person.get().getProfile()));
                            return c;
                        })
                        .collect(Collectors.toList()).reversed();

                System.out.println(postComments.stream().count());

                return postComments;
        }catch (Exception e) {
            throw new CustomException(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> savePost(String personId, String postId) {
        try {
//            Person person = personRepository.findById(new ObjectId(personId)).orElseThrow(() -> new CustomException("person not found", HttpStatus.NOT_FOUND));

            Optional<Person> optionalPerson = personRepository.findById(new ObjectId(personId));

            if(optionalPerson.isPresent()){
                SavedItems items = SavedItems.builder()
                        .postId(new ObjectId(postId))
                        .savedDate(new Date())
                        .build();

                Person person = optionalPerson.get();

                List<SavedItems> savedItems = person.getSaved();
                if (savedItems == null) {
                    savedItems = new ArrayList<>();
                }
                savedItems.add(items);
                person.setSaved(savedItems);
                personRepository.save(person);

                return ResponseEntity.ok("Saved successfully");
            }else{
                throw new CustomException("person not found", HttpStatus.NOT_FOUND);
            }
        }catch (Exception e) {
            throw new CustomException(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> unsavePost(String personId, String postId) {
        try {
            Person person = personRepository.findById(new ObjectId(personId)).orElseThrow(() -> new CustomException("person not found", HttpStatus.NOT_FOUND));
            ObjectId postObjectId = new ObjectId(postId);
            List<SavedItems> items = person.getSaved();

            if(items == null) {
                throw new CustomException("there is no saved items",HttpStatus.NOT_FOUND);
            }
//            if(!items.contains(postObjectId)){
//                System.out.println("yessssss");
//                throw new CustomException("there is no post named",HttpStatus.BAD_REQUEST);
//            }
            List<SavedItems> collect = items.stream().filter(i -> !i.getPostId().equals(postObjectId)).collect(Collectors.toList());
            person.setSaved(collect);
            personRepository.save(person);

            return ResponseEntity.ok("post unsaved successfully");
        } catch (CustomException e) {
            throw new CustomException(e.getMessage(),HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
