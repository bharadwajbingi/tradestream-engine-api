package com.mphasis.tse.filter;

public interface AuthenticationService {
    String login(String email, String password);
    void register(String email, String password);
}
