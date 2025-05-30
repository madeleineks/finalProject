package edu.abhs.hotProperties.repositories;

import edu.abhs.hotProperties.entities.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {

}
