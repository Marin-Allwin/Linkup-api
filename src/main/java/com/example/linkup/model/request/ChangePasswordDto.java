package com.example.linkup.model.request;

import lombok.Data;

@Data
public class ChangePasswordDto {

    private String email;
    private String newPasword;
}
