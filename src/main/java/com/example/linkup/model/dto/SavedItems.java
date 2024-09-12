package com.example.linkup.model.dto;

import lombok.*;
import org.bson.types.ObjectId;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SavedItems {

    private ObjectId personId;
    private ObjectId postId;
    private String firstName;
    private String lastName;
    private Date savedDate;
    private String postImage;
    private String content;
}
