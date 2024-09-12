package com.example.linkup.collection;

import com.example.linkup.model.dto.PostComments;
import com.example.linkup.model.dto.PostLikes;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Posts {

    @Id
    private ObjectId postId;
    private String Content;
    @JsonIgnore
    private byte[] postImage;
    private String image;
    private ObjectId personId;
    private Date postTime;
    private List<PostLikes> likes;
    private List<PostComments> comments;


}
