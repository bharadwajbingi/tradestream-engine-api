package com.mphasis.tse.filter;

import com.mphasis.tse.dto.ProfileResponse;

public interface UserService {


    ProfileResponse getProfile(String email);
}