package com.example.linkup.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostComments {

    private ObjectId personId;
    private String comment;
    private Date commentDate;
    private String PersonImage;
    private String firstName;
    private String lastName;
}
