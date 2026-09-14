package com.saas.automotriz.repository;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Review;
import com.saas.automotriz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBusiness(Business business);
    boolean existsByClientAndBusiness(User client, Business business);


}