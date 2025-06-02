package edu.abhs.hotProperties.service;

import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.entities.Property;
import org.springframework.ui.Model;

import java.util.List;

public interface UserService {
    void prepareDashboardModel(Model model);
    void addedProperty(Property property);
    List<Property> getProperties(User user);
}
