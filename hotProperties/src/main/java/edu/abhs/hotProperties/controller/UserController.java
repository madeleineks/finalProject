package edu.abhs.hotProperties.controller;

import edu.abhs.hotProperties.entities.*;
import edu.abhs.hotProperties.service.UserService;
import edu.abhs.hotProperties.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.ArrayList;

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

        if(userService.isFavorited(u, property))
        {
            model.addAttribute("showRemoveFavoriteButton", true);
        }
        else
        {
            model.addAttribute("showAddFavoriteButton", true);
        }

        model.addAttribute("user", u);
        model.addAttribute("property", property);
        return "property_view";
    }

    @GetMapping("/properties/search")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String viewForm(@RequestParam("zipcode") String zipcode, @RequestParam("minSqFt") String minSqFt,
                           @RequestParam("minPrice") String minPrice, @RequestParam("maxPrice") String maxPrice,
                           @RequestParam("sort") String sort, Model model) {

        List<Property> properties;

        if(minSqFt.isEmpty())
        {
            minSqFt = "0";
        }

        if(sort.equals("lowToHigh"))
        {
            if(minPrice.isEmpty() && maxPrice.isEmpty())
            {
                properties = userService.findPropertyByFiltersNoMinMaxAsc(zipcode, minSqFt);
            }
            else if(minPrice.isEmpty())
            {
                properties = userService.findPropertyByFiltersNoMinAsc(zipcode, minSqFt, maxPrice);
            }
            else if(maxPrice.isEmpty())
            {
                properties = userService.findPropertyByFiltersNoMaxAsc(zipcode, minSqFt, minPrice);
            }
            else
            {
                properties = userService.findPropertyWithAllFilters(zipcode, minSqFt, minPrice, maxPrice);
            }
        }
        else
        {
            if(minPrice.isEmpty() && maxPrice.isEmpty())
            {
                properties = userService.findPropertyByFiltersNoMinMaxDesc(zipcode, minSqFt);
            }
            else if(minPrice.isEmpty())
            {
                properties = userService.findPropertyByFiltersNoMinDesc(zipcode, minSqFt, maxPrice);
            }
            else if(maxPrice.isEmpty())
            {
                properties = userService.findPropertyByFiltersNoMaxDesc(zipcode, minSqFt, minPrice);
            }
            else
            {
                properties = userService.findPropertyWithAllFiltersDesc(zipcode, minSqFt, minPrice, maxPrice);
            }
        }

        model.addAttribute("properties", properties);
        model.addAttribute("count", properties.size());
        return "browse_properties";
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String viewFavorites(Model model) {
        User u = authService.getCurrentUser();

        List<Favorite> favorites = userService.getUsersFavorites(u);
        List<Property> properties = new ArrayList<>();

        for(Favorite fav : favorites)
        {
            properties.add(fav.getProperty());
        }

        model.addAttribute("user", u);
        model.addAttribute("properties", properties);
        return "favorites";
    }

    @GetMapping("/favorites/remove/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String removeFavorite(@PathVariable Long id, Model model) {
        User u = authService.getCurrentUser();

        Favorite favorite = userService.getSpecificFavorite(u, id);

        userService.removeFavorite(favorite);

        return "redirect:/favorites";
    }

    @GetMapping("/properties/remove/favorites/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String removeFavoriteFromPropertyPage(@PathVariable Long id, Model model) {
        User u = authService.getCurrentUser();

        Favorite favorite = userService.getSpecificFavorite(u, id);

        userService.removeFavorite(favorite);

        return "redirect:/properties/view/" + id;
    }

    @PostMapping("/favorites/add/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'BUYER', 'ADMIN')")
    public String addFavorite(@PathVariable Long id, Model model) {
        User u = authService.getCurrentUser();
        Property p = userService.getPropertyById(id);

        boolean favorited = false;

        List<Favorite> favorites = userService.getUsersFavorites(u);

        //Check if user already has property favored
        for(Favorite fav: favorites)
        {
            if(fav.getProperty().getId() == id)
            {
                favorited = true;
            }
        }

        if(!favorited)
        {
            Favorite favorite = new Favorite(u, p);
            userService.addFavorite(favorite);
        }

        return "redirect:/properties/view/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/create-agent")
    public String showCreateAgent(Model model) {
        model.addAttribute("user", new User());
        return "add_agent";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/create-agent")
    public String createAgent(@Valid @ModelAttribute("user") User user, BindingResult result, RedirectAttributes redirectAttributes) {
        try {
            if (result.hasErrors()) {
                return "add_agent";
            } else {
                userService.addUser(user);
                redirectAttributes.addFlashAttribute("successMessage", "Agent created successfully");
                return "redirect:/dashboard";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("failureMessage", "Agent creation failed: " + e.getMessage());
            return "redirect:/admin/users/create-agent";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public String showAllUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "all_users";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/delete")
    public String handleDeleteUser(@RequestParam("userId") Long userId, Model model,
                                   RedirectAttributes redirectAttributes) {
        userService.deleteUser(userId);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully");
        return "redirect:/admin/users";
    }

}
