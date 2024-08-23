package com.example.linkup.model.request;

import lombok.Data;

@Data
public class VerifyAccountDto {

    private String email;
    private Long otp;
}
