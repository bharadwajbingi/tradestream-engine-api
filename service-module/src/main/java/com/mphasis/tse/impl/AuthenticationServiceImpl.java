package com.mphasis.tse.impl;

import com.mphasis.tse.entity.User;
import com.mphasis.tse.exception.DuplicateUserException;
import com.mphasis.tse.filter.AuthenticationService;
import com.mphasis.tse.filter.JwtService;
import com.mphasis.tse.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    @Override
    public void register(String email, String password) {
        if (repo.existsByEmail(email)) {
            throw new DuplicateUserException("Email is already registered: " + email);
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(encoder.encode(password));

        repo.save(user);
    }

    @Override
    public String login(String email, String password) {

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        User user = repo.findByEmail(email).orElseThrow();

        return jwtService.generateToken(user);
    }
}