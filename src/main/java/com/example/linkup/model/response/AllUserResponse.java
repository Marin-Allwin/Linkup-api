package com.example.linkup.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllUserResponse {

    private String firstName;
    private String lastName;
    private String personId;
    private String email;
    private String bio;
    private String profile;
    private String friendshipStatus;
}
