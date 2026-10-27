// package com.example.demo.Repository;

// import com.example.demo.Entity.DeliveryPartner;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// import java.util.List;
// import java.util.Optional;
// import java.util.UUID;

// @Repository
// // public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, UUID> {
// //     List<DeliveryPartner> findByIsAvailableTrue();
// // }
// public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, UUID> {
//     List<DeliveryPartner> findByAvailableTrue();
//     Optional<DeliveryPartner> findByUserId(Long userId);
// }


package com.example.demo.Repository;

import com.example.demo.Entity.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, UUID> {

    // Matches 'available' boolean property in DeliveryPartner entity
    List<DeliveryPartner> findByAvailableTrue();

    Optional<DeliveryPartner> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndPartnerIdNot(Long userId, UUID partnerId);
}