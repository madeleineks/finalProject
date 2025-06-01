package edu.abhs.hotProperties.repository;

import edu.abhs.hotProperties.entities.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Integer> {
}
