package edu.abhs.hotProperties.service;

import edu.abhs.hotProperties.dtos.JwtResponse;
import edu.abhs.hotProperties.entities.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

public interface AuthService {
    JwtResponse authenticateAndGenerateToken(User user);

    public Cookie loginAndCreateJwtCookie(User user) throws BadCredentialsException;

    void clearJwtCookie(HttpServletResponse response);

    User getCurrentUser();
}
