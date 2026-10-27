package com.example.demo.Controller;

import com.example.demo.Entity.DeliveryPartner;
import com.example.demo.Entity.User;
import com.example.demo.Repository.DeliveryPartnerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-partners")
@Tag(name = "Delivery Partner Controller", description = "CRUD Endpoints for Delivery Partners")
public class DeliveryPartnerController {

    @Autowired
    private DeliveryPartnerRepository repository;

    @PersistenceContext
    private EntityManager em;

    @PostMapping
    @Transactional
    @Operation(summary = "Create a new delivery partner (optionally linked to a user via userId)")
    public ResponseEntity<?> createPartner(@RequestBody DeliveryPartner partner) {
        partner.setPartnerId(null); // DB generates ID

        if (partner.getUserId() != null) {
            User user = em.find(User.class, partner.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body("User does not exist");
            }
            if (repository.existsByUserId(partner.getUserId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("This user is already a delivery partner");
            }
            partner.setUser(user);
        }
        return ResponseEntity.ok(repository.save(partner));
    }

    @GetMapping
    @Operation(summary = "Get all delivery partners")
    public List<DeliveryPartner> getAllPartners() {
        return repository.findAll();
    }

    @GetMapping("/available")
    @Operation(summary = "Get all available delivery partners")
    public List<DeliveryPartner> getAvailablePartners() {
        return repository.findByAvailableTrue();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Find a delivery partner by user ID")
    public ResponseEntity<DeliveryPartner> getPartnerByUserId(@PathVariable Long userId) {
        return repository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a delivery partner by ID")
    public ResponseEntity<DeliveryPartner> getPartnerById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update all details of a delivery partner")
    public ResponseEntity<?> updatePartner(@PathVariable UUID id, @RequestBody DeliveryPartner updatedData) {
        DeliveryPartner partner = repository.findById(id).orElse(null);
        if (partner == null) {
            return ResponseEntity.notFound().build();
        }

        Long newUserId = updatedData.getUserId();
        if (newUserId != null) {
            User user = em.find(User.class, newUserId);
            if (user == null) {
                return ResponseEntity.badRequest().body("User does not exist");
            }
            if (repository.existsByUserIdAndPartnerIdNot(newUserId, id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("This user is already a delivery partner");
            }
            partner.setUser(user);
        } else {
            partner.setUser(null);
        }
        partner.setUserId(newUserId);

        partner.setVehicleNo(updatedData.getVehicleNo());
        partner.setCurrentLatitude(updatedData.getCurrentLatitude());
        partner.setCurrentLongitude(updatedData.getCurrentLongitude());
        partner.setCurrentStatus(updatedData.getCurrentStatus());
        partner.setDeliveredAt(updatedData.getDeliveredAt());
        partner.setAvailable(updatedData.isAvailable());
        return ResponseEntity.ok(repository.save(partner));
    }

    @PatchMapping("/{id}/location")
    @Operation(summary = "Partially update driver location and status")
    public ResponseEntity<DeliveryPartner> updateLocation(
            @PathVariable UUID id,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) String status) {

        return repository.findById(id).map(partner -> {
            if (latitude != null) partner.setCurrentLatitude(latitude);
            if (longitude != null) partner.setCurrentLongitude(longitude);
            if (status != null) partner.setCurrentStatus(status);
            return ResponseEntity.ok(repository.save(partner));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/availability")
    @Operation(summary = "Toggle partner availability")
    public ResponseEntity<DeliveryPartner> updateAvailability(
            @PathVariable UUID id,
            @RequestParam boolean available) {
        return repository.findById(id).map(partner -> {
            partner.setAvailable(available);
            partner.setCurrentStatus(available ? "AVAILABLE" : "OFFLINE");
            return ResponseEntity.ok(repository.save(partner));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete delivery partner record")
    public ResponseEntity<?> deletePartner(@PathVariable UUID id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete: partner has delivery history");
        }
    }
}