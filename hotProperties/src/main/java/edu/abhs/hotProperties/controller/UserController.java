package edu.abhs.hotProperties.controller;

import edu.abhs.hotProperties.entities.Property;
import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.service.PropertyService;
import edu.abhs.hotProperties.service.UserService;
import edu.abhs.hotProperties.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
public class UserController {

    UserService userService;
    AuthService authService;
    PasswordEncoder passwordEncoder;
    PropertyService propertyService;

    @Autowired
    public UserController(UserService userService, AuthService authService, PasswordEncoder passwordEncoder, PropertyService propertyService) {
        this.userService = userService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.propertyService = propertyService;
    }

    @GetMapping({"/login", "/"})
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
    public String showAddProperties(Model model) {
        model.addAttribute("property", new Property());
        return "add_properties";
    }

    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    @PostMapping("/properties/add")
    public String addProperty(@ModelAttribute("property") Property property, @RequestParam(value = "file", required = false)
    List<MultipartFile> files, Model model) throws IOException {

        if (files.isEmpty() || property == null) {
            model.addAttribute("fail_message", "Could not add Property. Please try again.");
            return "add_properties";
        }

        for(MultipartFile file: files) {
            if (file.isEmpty()) {
                userService.addedProperty(property);
                propertyService.addProperty(property);

            } else {
                Path destination = Paths.get("src/main/resources/static/images", file.getOriginalFilename());
                file.transferTo(destination);

                userService.addedProperty(property);
                propertyService.addProperty(property);
                propertyService.addPropertyWithImage(property, file.getOriginalFilename());
            }
        }
        model.addAttribute("success_message", "Added new property successfully!");
        return "add_properties";
    }


}
