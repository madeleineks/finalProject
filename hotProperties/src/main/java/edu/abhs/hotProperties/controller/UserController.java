package edu.abhs.hotProperties.controller;

import edu.abhs.hotProperties.dtos.JwtResponse;
import edu.abhs.hotProperties.entities.Property;
import edu.abhs.hotProperties.entities.PropertyImage;
import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.service.UserService;
import edu.abhs.hotProperties.service.AuthService;
import io.jsonwebtoken.io.Encoders;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Controller
public class UserController {

    UserService userService;
    AuthService authService;
    PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserService userService, AuthService authService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@ModelAttribute("user") User user, HttpServletResponse response, Model model) {
        try {
            Cookie jwtCookie = authService.loginAndCreateJwtCookie(user);
            response.addCookie(jwtCookie);
            return "redirect:/dashboard";
        } catch (BadCredentialsException e) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") User user, String confirmPassword, Model model) {
        try {
            // Validate password match
            if (!user.getPassword().equals(confirmPassword)) {
                model.addAttribute("errorMessage", "Passwords do not match");
                return "register";
            }

            // Validate password strength
            if (!isPasswordStrong(user.getPassword())) {
                model.addAttribute("errorMessage", "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character");
                return "register";
            }

            // Check if email already exists
            if (userService.emailExists(user.getEmail())) {
                model.addAttribute("errorMessage", "Email already registered");
                return "register";
            }

            // Set createdAt timestamp
            user.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            // Encode password and save user
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userService.saveUser(user);

            model.addAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    private boolean isPasswordStrong(String password) {
        return password != null && password.length() >= 8 &&
                password.matches(".*[A-Z].*") && // uppercase
                password.matches(".*[a-z].*") && // lowercase
                password.matches(".*\\d.*") &&   // digit
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*"); // special char
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String showDashboard(Model model) {
        userService.prepareDashboardModel(model);
        return "dashboard";
    }

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/properties/manage")
    public String agentManage(Model model) {
        User user = authService.getCurrentUser();
        model.addAttribute("user", user);
        return "manage_properties";
    }

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/properties/add")
    public String add(Model model) {
        model.addAttribute("property", new Property());
        return "add_properties";
    }

    @PreAuthorize("hasRole('AGENT')")
    @PostMapping("/properties/add")
    public String processAdd(@ModelAttribute("property") Property property) {
        userService.addProperty(property);
        return "manage_properties";
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String viewProfile(Model model) {
        User user = authService.getCurrentUser();
        model.addAttribute(user);
        return "my_profile";
    }

    @GetMapping("/profile/edit")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String editProfile(Model model) {
        User user = authService.getCurrentUser();
        model.addAttribute(user);
        return "edit_profile";
    }

    @PostMapping("/profile/edit")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String editProfile(@ModelAttribute("user") User user, Model model) {
        User u = authService.getCurrentUser();

        model.addAttribute(u);
        u.setFirstName(user.getFirstName());
        u.setLastName(user.getLastName());

        if(!u.getEmail().equals(user.getEmail()))
        {
            u.setEmail(user.getEmail());

            //Update user data
            userService.saveUser(u);

            // need to login if email was changed for authentication
            return "login";
        }

        //Update user data
        userService.saveUser(u);
        return "my_profile";
    }

    @GetMapping("/properties/list")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String browseProperties(Model model) {

        List<Property> properties = userService.getAllProperties();

        model.addAttribute("properties", properties);
        model.addAttribute("count", properties.size());
        return "browse_properties";
    }

    @GetMapping("/properties/view/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String viewProperty(@PathVariable Long id, Model model) {
        User u = authService.getCurrentUser();
        Property property = userService.getPropertyById(id);
        model.addAttribute("user", u);
        model.addAttribute("property", property);
        return "property_view";
    }
}
