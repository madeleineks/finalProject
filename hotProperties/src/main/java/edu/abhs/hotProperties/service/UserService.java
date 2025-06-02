package edu.abhs.hotProperties.service;

import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.entities.Property;
import org.springframework.ui.Model;

import java.util.List;

public interface UserService {
    void prepareDashboardModel(Model model);
    List<Property> getProperties(User user);
    void addedProperty(Property property);
    boolean emailExists(String email);
    void saveUser(User user);

}
