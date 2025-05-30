package edu.abhs.hotProperties.controller;

import edu.abhs.hotProperties.entities.Property;
import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.service.UserService;
import edu.abhs.hotProperties.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    UserService userService;
    AuthService authService;

    @Autowired
    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@ModelAttribute("user") User user, HttpServletResponse response, org.springframework.ui.Model model) {
        try {
            Cookie jwtCookie = authService.loginAndCreateJwtCookie(user);
            response.addCookie(jwtCookie);
            return "redirect:/dashboard";
        } catch (BadCredentialsException e) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String showDashboard(Model model) {
        userService.prepareDashboardModel(model);
        return "dashboard";
    }

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/properties/manage")
    public String home() {
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

}
