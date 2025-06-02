package edu.abhs.hotProperties.service;

import edu.abhs.hotProperties.entities.Property;

public interface PropertyService {
    void addProperty(Property property);
    void addPropertyWithImage(Property property, String fileName);
}
