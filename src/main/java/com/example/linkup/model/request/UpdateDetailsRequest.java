package com.example.linkup.model.request;

import com.example.linkup.model.dto.College;
import com.example.linkup.model.dto.School;
import com.example.linkup.model.dto.Work;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDetailsRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String bio;
    private String relationShipStatus;
    private List<School> schools;
    private List<College> colleges;
    private List<Work> works;
    private String gender;
    private LocalDateTime dob;
    private String phone;

}
