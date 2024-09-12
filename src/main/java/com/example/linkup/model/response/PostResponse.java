package com.example.linkup.model.response;

import com.example.linkup.model.dto.PostComments;
import com.example.linkup.model.dto.PostLikes;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import javax.xml.stream.events.Comment;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {

    private ObjectId postId;
    private String Content;
    private String image;
    private Date postTime;

    private ObjectId personId;
//    private String personName;
    private String firstName;
    private String lastName;
    private String email;
    private String profile;
    private List<PostLikes> likes;
    private List<PostComments> comments;
}
