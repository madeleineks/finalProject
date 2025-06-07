package edu.abhs.hotProperties.service;

import edu.abhs.hotProperties.entities.Favorite;
import edu.abhs.hotProperties.entities.Messages;
import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.entities.Property;
import edu.abhs.hotProperties.repository.FavoriteRepository;
import edu.abhs.hotProperties.repository.PropertyRepository;
import edu.abhs.hotProperties.repository.UserRepository;
import edu.abhs.hotProperties.utils.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final AuthService authService;
    private final MessagesService messagesService;
    UserRepository userRepository;
    PropertyRepository propertyRepository;
    FavoriteRepository favoriteRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PropertyRepository propertyRepository, AuthService authService, FavoriteRepository favoriteRepository, MessagesService messagesService) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.authService = authService;
        this.favoriteRepository = favoriteRepository;
        this.messagesService = messagesService;
    }

    @Override
    public List<Property> getProperties(User user) {
        return propertyRepository.findPropertyByUser(user);
    }



    @Override
    public void prepareDashboardModel(Model model) {
        CurrentUserContext context = getCurrentUserContext();
        User user = authService.getCurrentUser();

        if(context.user().getRole() == User.Role.BUYER)
        {
            model.addAttribute("messageCountBuyer", user.getMessageList().size());
            model.addAttribute("favCount", favoriteRepository.countByBuyer(context.user()));
        }
        if (context.user().getRole() == User.Role.AGENT) {
            int messageCount = 0;
            for (Property property : user.getPropertyList()) {
                for (Messages message : property.getMessageList()) {
                    messageCount++;
                }
            }
            model.addAttribute("messageCountAgent", messageCount);
        }

        model.addAttribute("user", context.user());
        model.addAttribute("authorization", context.auth());
    }

    @Override
    public void addedProperty(Property property) {
        User user = getCurrentUserContext().user();
        user.addProperty(property);
        property.setUser(user);
    }

    @Override
    public User getUserById(long id) {
        return userRepository.findUserById(id);
    }

    @Override
    public List<Messages> getAgentMessages() {
        User user = authService.getCurrentUser();
        List<Property> properties = user.getPropertyList();

        List<Messages> messages = new ArrayList<>();
        for (Property property : properties) {
            for (Messages message : property.getMessageList()) {
                messages.add(message);
            }
        }
        return messages;
    }

    @Override
    public List<Messages> getBuyerMessages() {
        User user = authService.getCurrentUser();

        List<Messages> messages = new ArrayList<>();
        for (Messages message : user.getMessageList()) {
            messages.add(message);
        }
        return messages;
    }


    @Override
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Override
    public void updateProfile(User newUser) {

        User oldUser = authService.getCurrentUser();
        oldUser.setFirstName(newUser.getFirstName());
        oldUser.setLastName(newUser.getLastName());

        userRepository.save(oldUser);
    }

    @Override
    public void removeProperty(Property property) {
        User user = authService.getCurrentUser();
        user.removeProperty(property);
        userRepository.save(user);
    }

    private CurrentUserContext getCurrentUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new CurrentUserContext(user, auth);
    }

    public List<Property> getAllProperties()
    {
        return propertyRepository.findByOrderByPriceAsc();
    }

    public Property getPropertyById(Long id)
    {
        return propertyRepository.findPropertyById(id);
    }


    @Override
    public List<Property> findPropertyWithAllFilters(String zipcode, String minSqFt, String minPrice,
                                                       String maxPrice)
    {
        return propertyRepository.findPropertyByWithAllFiltersAsc(zipcode, minSqFt, minPrice, maxPrice);
    }

    @Override
    public List<Property> findPropertyByFiltersNoMinMaxAsc(String zipcode, String minSqFt)
    {
        return propertyRepository.findPropertyByFiltersNoMinMaxAsc(zipcode, minSqFt);
    }

    @Override
    public List<Property> findPropertyByFiltersNoMinAsc(String zipcode, String minSqFt, String maxPrice)
    {
        return propertyRepository.findPropertyLessThanMaxPriceAsc(zipcode, minSqFt, maxPrice);
    }

    @Override
    public List<Property> findPropertyByFiltersNoMaxAsc(String zipcode, String minSqFt, String minPrice)
    {
        return propertyRepository.findPropertyGreaterThanMinPriceAsc(zipcode, minSqFt, minPrice);
    }

    @Override
    public List<Property> findPropertyByFiltersNoMinMaxDesc(String zipcode, String minSqFt)
    {
        return propertyRepository.findPropertyByFiltersNoMinMaxDesc(zipcode, minSqFt);
    }

    @Override
    public List<Property> findPropertyByFiltersNoMinDesc(String zipcode, String minSqFt, String maxPrice)
    {
        return propertyRepository.findPropertyLessThanMaxPriceDesc(zipcode, minSqFt, maxPrice);
    }

    @Override
    public List<Property> findPropertyByFiltersNoMaxDesc(String zipcode, String minSqFt, String minPrice)
    {
        return propertyRepository.findPropertyGreaterThanMinPriceDesc(zipcode, minSqFt, minPrice);
    }

    @Override
    public List<Property> findPropertyWithAllFiltersDesc(String zipcode, String minSqFt, String minPrice, String maxPrice)
    {
        return propertyRepository.findPropertyByWithAllFiltersDesc(zipcode, minSqFt, minPrice, maxPrice);
    }

    public List<Favorite> getUsersFavorites(User user)
    {
        return favoriteRepository.findByBuyer(user);
    }

    @Override
    public Favorite getSpecificFavorite(User u, Long id)
    {
        Property p = propertyRepository.findPropertyById(id);
        return favoriteRepository.findByBuyerAndProperty(u, p);
    }

    @Override
    public void removeFavorite(Favorite favorite)
    {
        favoriteRepository.delete(favorite);
    }

    @Override
    public void addFavorite(Favorite favorite)
    {
        favoriteRepository.save(favorite);
    }

    @Override
    public boolean isFavorited(User u, Property property)
    {
        return favoriteRepository.existsByBuyerAndProperty(u, property);
    }
}
