// package com.example.demo.Repository;

// import com.example.demo.Entity.DeliveryTracking;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// import java.util.List;
// import java.util.UUID;

// @Repository
// public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, UUID> {
//     List<DeliveryTracking> findByDeliveryPartnerPartnerId(UUID partnerId);
//     List<DeliveryTracking> findByOrderId(Long orderId);
// }

package com.example.demo.Repository;

import com.example.demo.Entity.DeliveryTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, UUID> {

    // Navigates deliveryPartner -> partnerId
    List<DeliveryTracking> findByDeliveryPartnerPartnerId(UUID partnerId);

    Optional<DeliveryTracking> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<DeliveryTracking> findByAssignmentStatusIgnoreCase(String status);
}