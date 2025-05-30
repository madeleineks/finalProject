package edu.abhs.hotProperties.repository;

import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.entities.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer> {
    List<Property> findPropertyByUser(User user);
    Property findPropertyByTitle(String title);
}
