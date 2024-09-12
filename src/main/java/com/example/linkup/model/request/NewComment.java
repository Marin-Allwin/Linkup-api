package com.example.linkup.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewComment {

    private String postId;
    private String personId;
    private String comment;
    private Date commentDate;
}
