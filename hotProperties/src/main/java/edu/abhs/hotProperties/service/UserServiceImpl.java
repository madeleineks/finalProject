package edu.abhs.hotProperties.service;

import edu.abhs.hotProperties.entities.Favorite;
import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.entities.Property;
import edu.abhs.hotProperties.exceptions.NotFoundException;
import edu.abhs.hotProperties.exceptions.UserAlreadyExistsException;
import edu.abhs.hotProperties.repository.FavoriteRepository;
import edu.abhs.hotProperties.repository.PropertyRepository;
import edu.abhs.hotProperties.repository.UserRepository;
import edu.abhs.hotProperties.utils.CurrentUserContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.sql.Timestamp;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    PropertyRepository propertyRepository;
    FavoriteRepository favoriteRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PropertyRepository propertyRepository, FavoriteRepository favoriteRepository) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @Override
    public List<Property> getProperties(User user) {
        return propertyRepository.findPropertyByUser(user);
    }

    @Override
    public void prepareDashboardModel(Model model) {
        CurrentUserContext context = getCurrentUserContext();

        if(context.user().getRole() == User.Role.BUYER)
        {
            model.addAttribute("favCount", favoriteRepository.countByBuyer(context.user()));
        }

        model.addAttribute("user", context.user());
        model.addAttribute("authorization", context.auth());
    }

    @Override
    public void addProperty(Property property) {

        User user = getCurrentUserContext().user();
        property.setUser(user);
        propertyRepository.save(property);
        user.addProperty(property);
        System.out.println(user.getEmail());
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Transactional
    @Override
    public void addUser(User user) {
        if (!userRepository.existsByEmail(user.getEmail())) {
            user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            user.setRole(User.Role.AGENT);
            userRepository.save(user);
        } else {
            throw new UserAlreadyExistsException("Email already in use.");
        }

    }

    @Override
    public List<User> getAllUsers() {
        if (userRepository.count() != 0) {
            return userRepository.findAll();
        } else {
            throw new NotFoundException("No users found in database");
        }
    }

    @Transactional
    @Override
    public void deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        } else {
            throw new NotFoundException("User not found");
        }
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
