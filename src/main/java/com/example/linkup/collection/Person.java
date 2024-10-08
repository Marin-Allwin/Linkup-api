package com.example.linkup.collection;

import com.example.linkup.model.RequestReceive;
import com.example.linkup.model.RequestSent;
import com.example.linkup.model.dto.College;
import com.example.linkup.model.dto.SavedItems;
import com.example.linkup.model.dto.School;
import com.example.linkup.model.dto.Work;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(value = "person")
public class Person implements UserDetails {

    @Id
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



    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }


}
