package com.mphasis.tse.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private String email;
    private String name;
}