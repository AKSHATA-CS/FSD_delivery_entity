package com.example.demo.Controller;


import com.example.demo.Entity.Address;
import com.example.demo.Entity.DeliveryPartner;
import com.example.demo.Entity.DeliveryTracking;
import com.example.demo.Entity.Order;
import com.example.demo.Entity.Vendor;
import com.example.demo.Repository.DeliveryPartnerRepository;
import com.example.demo.Repository.DeliveryTrackingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-tracking")
@Tag(name = "Delivery Tracking Controller", description = "Endpoints for managing delivery assignments and tracking")
public class DeliveryTrackingController {

    private static final Set<String> VALID_STATUSES =
            Set.of("ASSIGNED", "PICKED_UP", "IN_TRANSIT", "DELIVERED", "CANCELLED");

    @Autowired
    private DeliveryTrackingRepository trackingRepository;

    @Autowired
    private DeliveryPartnerRepository partnerRepository;

    @PersistenceContext
    private EntityManager em;

    // ---------- CREATE ----------

    @PostMapping("/partner/{partnerId}")
    @Transactional
    @Operation(summary = "Assign an order to a partner (body: orderId, pickupVendorId, pickupUserId, eta)")
    public ResponseEntity<?> createAssignment(
            @PathVariable UUID partnerId,
            @RequestBody DeliveryTracking trackingRequest) {

        DeliveryPartner partner = partnerRepository.findById(partnerId).orElse(null);
        if (partner == null) {
            return ResponseEntity.notFound().build();
        }
        if (!partner.isAvailable()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Partner is not available");
        }

        // Validate the Order relationship
        Long orderId = trackingRequest.getOrderId();
        if (orderId == null) {
            return ResponseEntity.badRequest().body("orderId is required");
        }
        Order order = em.find(Order.class, orderId);
        if (order == null) {
            return ResponseEntity.badRequest().body("Order does not exist");
        }
        if (trackingRepository.existsByOrderId(orderId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Order is already assigned");
        }

        // Validate the optional Vendor relationship
        Vendor vendor = null;
        if (trackingRequest.getPickupVendorId() != null) {
            vendor = em.find(Vendor.class, trackingRequest.getPickupVendorId());
            if (vendor == null) {
                return ResponseEntity.badRequest().body("Vendor does not exist");
            }
        }

        // Validate the optional Address relationship
        Address address = null;
        if (trackingRequest.getPickupUserId() != null) {
            address = em.find(Address.class, trackingRequest.getPickupUserId());
            if (address == null) {
                return ResponseEntity.badRequest().body("Address does not exist");
            }
        }

        trackingRequest.setAssignmentId(null); // DB generates ID
        trackingRequest.setOrder(order);
        trackingRequest.setPickupVendor(vendor);
        trackingRequest.setPickupUser(address);
        trackingRequest.setDeliveryPartner(partner);
        trackingRequest.setAssignmentStatus("ASSIGNED");

        partner.setAvailable(false);
        partner.setCurrentStatus("BUSY");
        partnerRepository.save(partner);

        return ResponseEntity.ok(trackingRepository.save(trackingRequest));
    }

    // ---------- READ ----------

    @GetMapping
    @Operation(summary = "Get all tracking records")
    public List<DeliveryTracking> getAllTracking() {
        return trackingRepository.findAll();
    }

    @GetMapping("/{assignmentId}")
    @Operation(summary = "Get a tracking record by assignment ID")
    public ResponseEntity<DeliveryTracking> getTrackingById(@PathVariable UUID assignmentId) {
        return trackingRepository.findById(assignmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/partner/{partnerId}")
    @Operation(summary = "Get all tracking assignments for a specific delivery partner")
    public ResponseEntity<List<DeliveryTracking>> getAssignmentsByPartner(@PathVariable UUID partnerId) {
        return ResponseEntity.ok(trackingRepository.findByDeliveryPartnerPartnerId(partnerId));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get tracking record for an order")
    public ResponseEntity<DeliveryTracking> getTrackingByOrder(@PathVariable Long orderId) {
        return trackingRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}/live")
    @Operation(summary = "Live tracking info for an order (partner location, status, ETA)")
    public ResponseEntity<Map<String, Object>> getLiveTracking(@PathVariable Long orderId) {
        return trackingRepository.findByOrderId(orderId)
                .map(t -> {
                    DeliveryPartner p = t.getDeliveryPartner();
                    Map<String, Object> live = new HashMap<>();
                    live.put("orderId", t.getOrderId());
                    live.put("assignmentStatus", t.getAssignmentStatus());
                    live.put("eta", t.getEta());
                    live.put("vehicleNo", p.getVehicleNo());
                    live.put("latitude", p.getCurrentLatitude());
                    live.put("longitude", p.getCurrentLongitude());
                    return ResponseEntity.ok(live);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get tracking records by assignment status")
    public List<DeliveryTracking> getByStatus(@PathVariable String status) {
        return trackingRepository.findByAssignmentStatusIgnoreCase(status);
    }

    // ---------- UPDATE ----------

    @PatchMapping("/{assignmentId}/status")
    @Transactional
    @Operation(summary = "Update the assignment status and ETA")
    public ResponseEntity<?> updateTrackingStatus(
            @PathVariable UUID assignmentId,
            @RequestParam(required = false) String assignmentStatus,
            @RequestParam(required = false) String eta) {

        DeliveryTracking tracking = trackingRepository.findById(assignmentId).orElse(null);
        if (tracking == null) {
            return ResponseEntity.notFound().build();
        }

        if (assignmentStatus != null) {
            if (!VALID_STATUSES.contains(assignmentStatus)) {
                return ResponseEntity.badRequest()
                        .body("Invalid status. Allowed: " + VALID_STATUSES);
            }
            tracking.setAssignmentStatus(assignmentStatus);

            // Free the partner once the delivery is finished
            if ("DELIVERED".equals(assignmentStatus) || "CANCELLED".equals(assignmentStatus)) {
                DeliveryPartner partner = tracking.getDeliveryPartner();
                partner.setAvailable(true);
                partner.setCurrentStatus("AVAILABLE");
                if ("DELIVERED".equals(assignmentStatus)) {
                    partner.setDeliveredAt(LocalDateTime.now());
                }
                partnerRepository.save(partner);
            }
        }
        if (eta != null) tracking.setEta(eta);

        return ResponseEntity.ok(trackingRepository.save(tracking));
    }

    // ---------- DELETE ----------

    @DeleteMapping("/{assignmentId}")
    @Transactional
    @Operation(summary = "Delete a tracking record")
    public ResponseEntity<Void> deleteTracking(@PathVariable UUID assignmentId) {
        return trackingRepository.findById(assignmentId).map(t -> {
            String s = t.getAssignmentStatus();
            // If delivery was still active, free the partner
            if (!"DELIVERED".equals(s) && !"CANCELLED".equals(s)) {
                DeliveryPartner p = t.getDeliveryPartner();
                p.setAvailable(true);
                p.setCurrentStatus("AVAILABLE");
                partnerRepository.save(p);
            }
            trackingRepository.delete(t);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}