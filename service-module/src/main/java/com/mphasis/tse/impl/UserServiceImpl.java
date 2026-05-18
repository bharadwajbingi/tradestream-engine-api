package com.mphasis.tse.impl;

import com.mphasis.tse.dto.ProfileResponse;
import com.mphasis.tse.filter.UserService;
import com.mphasis.tse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repo;

    @Override
    public ProfileResponse getProfile(String email) {

        var user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new ProfileResponse(user.getEmail());
    }


}