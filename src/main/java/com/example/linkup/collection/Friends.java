package com.example.linkup.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Friends {

    private String firstName;
    private String LastName;
    private String email;
    private String bio;
}
