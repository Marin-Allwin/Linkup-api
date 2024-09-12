package com.example.linkup.model.request;

import com.example.linkup.collection.Address;
import com.example.linkup.collection.Friends;
import com.example.linkup.model.RequestReceive;
import com.example.linkup.model.RequestSent;
import com.example.linkup.model.dto.College;
import com.example.linkup.model.dto.SavedItems;
import com.example.linkup.model.dto.School;
import com.example.linkup.model.dto.Work;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Transient;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonRequest {

    @JsonIgnore
    private ObjectId personId;
    private String firstName;
    private String lastName;
    private String bio;
    private String email;
    private String phoneNumber;
    private List<Address> address;
    @JsonIgnore
    private String password;
    private Date dob;
    private String gender;
    private String relationShipStatus;

    @JsonIgnore
    private byte[] profile;
    @JsonIgnore
    private byte[] cover;

    @Transient
    private String profileImg;
    @Transient
    private String coverImg;

    private List<School> schools;
    private List<College> colleges;
    private List<Work> works;
    private List<Friends> friendsList;
    private List<ObjectId> postIds;
    private List<RequestSent> sent;
    private List<RequestReceive> received;
    private List<SavedItems> saved;
}
