package com.example.linkup.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllUserResponse {

    private String firstName;
    private String lastName;
    private String email;
    private String bio;
    private String profile;
}
