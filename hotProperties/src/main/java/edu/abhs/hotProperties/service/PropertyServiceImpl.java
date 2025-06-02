package edu.abhs.hotProperties.service;

import edu.abhs.hotProperties.entities.Property;
import edu.abhs.hotProperties.entities.PropertyImage;
import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.repository.PropertyImageRepository;
import edu.abhs.hotProperties.repository.PropertyRepository;
import edu.abhs.hotProperties.repository.UserRepository;
import edu.abhs.hotProperties.utils.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PropertyServiceImpl implements PropertyService {

    PropertyRepository propertyRepository;
    PropertyImageRepository propertyImageRepository;

    @Autowired
    public PropertyServiceImpl(PropertyRepository propertyRepository, PropertyImageRepository propertyImageRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
    }

    @Override
    public void addProperty(Property property) {
        propertyRepository.save(property);

    }

    @Override
    public void addPropertyWithImage(Property property, String fileName) {
        propertyRepository.save(property);
        PropertyImage propertyImage = new PropertyImage(fileName);
        property.addPropertyImage(propertyImage);
        propertyImage.setProperty(property);
        propertyImageRepository.save(propertyImage);
    }

}
