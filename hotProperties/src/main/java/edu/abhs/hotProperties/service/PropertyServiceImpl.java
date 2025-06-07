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
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class PropertyServiceImpl implements PropertyService {

    private final AuthService authService;
    PropertyRepository propertyRepository;
    PropertyImageRepository propertyImageRepository;

    @Autowired
    public PropertyServiceImpl(PropertyRepository propertyRepository, PropertyImageRepository propertyImageRepository, AuthService authService) {
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.authService = authService;
    }

    @Override
    public void addProperty(Property property) {
        propertyRepository.save(property);

    }

    @Override
    public void addPropertyImages(Property property, List<MultipartFile> files) {

        for(MultipartFile file: files) {
            try {
                Path destination = Paths.get("src/main/resources/static/images", file.getOriginalFilename());
                file.transferTo(destination);

                PropertyImage propertyImage = new PropertyImage(file.getOriginalFilename());
                property.addPropertyImage(propertyImage);
                propertyImage.setProperty(property);
                propertyImageRepository.save(propertyImage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Property getByTitle(String title) {
        return propertyRepository.findPropertyByTitle(title);
    }

    @Override
    public void deletePropertyImage(Property property, long imageId) {
        PropertyImage propertyImage =  propertyImageRepository.findById(imageId);
        property.removePropertyImage(propertyImage);
        propertyRepository.save(property);
    }

    @Override
    public Property getPropertyById(long id) {
        return propertyRepository.findPropertyById(id);
    }

    @Override
    public void updateProperty(Property newProperty, Property property) {
        property.setTitle(newProperty.getTitle());
        property.setPrice(newProperty.getPrice());
        property.setLocation(newProperty.getLocation());
        property.setSize(newProperty.getSize());
        property.setDescription(newProperty.getDescription());
        propertyRepository.save(property);

    }

    @Override
    public long getAgent(Property property) {
        return propertyRepository.findUserIdByPropertyId(property.getId());
    }


}
