package edu.abhs.hotProperties.controller;

import edu.abhs.hotProperties.entities.Messages;
import edu.abhs.hotProperties.entities.Favorite;
import edu.abhs.hotProperties.entities.Property;
import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.service.*;
import edu.abhs.hotProperties.service.UserService;
import edu.abhs.hotProperties.service.AuthService;
import io.jsonwebtoken.security.Message;
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
import java.util.List;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;

@Controller
public class UserController {

    UserService userService;
    AuthService authService;
    PasswordEncoder passwordEncoder;
    PropertyService propertyService;
    MessagesService messagesService;

    @Autowired
    public UserController(UserService userService, AuthService authService, PasswordEncoder passwordEncoder,
                          PropertyService propertyService,  MessagesService messagesService) {
        this.userService = userService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.propertyService = propertyService;
        this.messagesService = messagesService;
    }

    @GetMapping({"/login", "/"})
    public String login(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login" )
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
    @PostMapping("/properties/add")
    public String addProperty(@ModelAttribute("property") Property property, @RequestParam(value = "file", required = false)
    List<MultipartFile> files, Model model) {

        if (property == null) {
            model.addAttribute("fail_message", "Could not add Property. Please try again.");
            return "add_properties";
        }

        userService.addedProperty(property);
        propertyService.addProperty(property);
        propertyService.addPropertyImages(property, files);

        User user = authService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("success_message", "Added new property successfully!");
        return "manage_properties";
    }

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/editProperty")
    public String showEditProperty(@RequestParam("title") String title, Model model) {
        Property property = propertyService.getByTitle(title);
        model.addAttribute("property", property);
        model.addAttribute("newProperty", new Property());
        return "edit_property";
    }

    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    @PostMapping("/editProperty")
    public String editProperty(@ModelAttribute("newProperty") Property newProperty,@RequestParam("id") long id,
                               @RequestParam(value = "file", required = false)
    List<MultipartFile> files, Model model)  {
        User user = authService.getCurrentUser();
        Property property = propertyService.getPropertyById(id);
        propertyService.updateProperty(newProperty, property);
        propertyService.addPropertyImages(property, files);
        model.addAttribute("successMessage", "Property updated successfully!");
        model.addAttribute("user", user);
        return "manage_properties";
    }

    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    @PostMapping("/deletePropertyImage")
    public String deletePropertyImage(@RequestParam("propsid") long id, @RequestParam("imageId") long imageId, Model model) {

        Property property = propertyService.getPropertyById(id);

        propertyService.deletePropertyImage(property, imageId);
        model.addAttribute("property", property);
        model.addAttribute("newProperty", new Property());
        model.addAttribute("successMessage", "Property image deleted successfully");
        return "edit_property";
    }

    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    @PostMapping("/deleteProperty")
    public String deleteProperty(@RequestParam("id") long id, Model model) {
        Property property=  propertyService.getPropertyById(id);
        userService.removeProperty(property);
        model.addAttribute("successMessage", "Property removed successfully");
        model.addAttribute("user", authService.getCurrentUser());
        return "manage_properties";
    }

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/properties/add")
    public String showAddProperties(Model model) {
        model.addAttribute("property", new Property());
        return "add_properties";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/myProfile")
    public String showMyProfile(Model model) {
        model.addAttribute("user", authService.getCurrentUser());
        return "my_profile";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/editProfile")
    public String editProfile(Model model) {
        model.addAttribute("newUser", new User());
        model.addAttribute("oldUser", authService.getCurrentUser());
        return "edit_profile";
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/editProfile")
    public String editedProfile(@ModelAttribute("newUser") User newUser, Model model) {

        try {
            userService.updateProfile(newUser);
            model.addAttribute("user", authService.getCurrentUser());
            model.addAttribute("update", "Name Changed successfully!");
            return "dashboard";
        } catch (Exception e) {
            model.addAttribute("failed", "Could not update profile name!. Please try again.");
            return "redirect:/editProfile";
        }
    }

    @PreAuthorize("hasAnyRole('AGENT', 'BUYER')")
    @GetMapping("/messages")
    public String showMessages(Model model) {
        User user = authService.getCurrentUser();
        model.addAttribute("user", user);
        List<Property> properties = user.getPropertyList();

        List<Messages> messages = new ArrayList<>();
        for (Property property : properties) {
            for (Messages message : property.getMessageList()) {
                messages.add(message);
            }
        }

        model.addAttribute("messages", messages);
        return "messages";
    }

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/buyer/sendMessageToAgent")
    public String sendMessageToAgent(@RequestParam("msg") String msg ,@RequestParam("prop") long id, Model model) {
        Property prop = propertyService.getPropertyById(id);
        long agentId = propertyService.getAgent(prop);
        User agent = userService.getUserById(agentId);

        Messages sentMessages = new Messages(msg);

        agent.addMessage(sentMessages);
        sentMessages.setSender(authService.getCurrentUser());

        prop.addMessage(sentMessages);
        sentMessages.setProperty(prop);
        messagesService.addMessages(sentMessages);

        model.addAttribute("sent_agent_worked", "Message sent to agent!");
        model.addAttribute("user", authService.getCurrentUser());
        model.addAttribute("property", prop);

        if (userService.isFavorited(authService.getCurrentUser(), prop)) {
            model.addAttribute("showRemoveFavoriteButton", true);
        } else {
            model.addAttribute("showAddFavoriteButton", true);
        }
        return "property_view";
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
}
