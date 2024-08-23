package com.example.linkup.model.response;

import com.example.linkup.collection.Person;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private Person person;
    private String accessToken;
    private String refreshToken;
}
