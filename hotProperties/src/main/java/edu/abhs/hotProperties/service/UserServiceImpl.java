package edu.abhs.hotProperties.service;

import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.entities.Property;
import edu.abhs.hotProperties.repository.PropertyRepository;
import edu.abhs.hotProperties.repository.UserRepository;
import edu.abhs.hotProperties.utils.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    PropertyRepository propertyRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PropertyRepository propertyRepository) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
    }

    @Override
    public List<Property> getProperties(User user) {
        return propertyRepository.findPropertyByUser(user);
    }

    @Override
    public void prepareDashboardModel(Model model) {
        CurrentUserContext context = getCurrentUserContext();
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

    private CurrentUserContext getCurrentUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new CurrentUserContext(user, auth);
    }

    public List<Property> getAllProperties()
    {
        return propertyRepository.findAll();
    }

    public Property getPropertyById(Long id)
    {
        return propertyRepository.findPropertyById(id);
    }
}
