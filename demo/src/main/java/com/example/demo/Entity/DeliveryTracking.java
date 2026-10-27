package com.example.demo.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "delivery_tracking")
public class DeliveryTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assignment_id", updatable = false, nullable = false)
    private UUID assignmentId;

    // DeliveryTracking "tracks" an Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @Column(name = "order_id", insertable = false, updatable = false)
    private Long orderId;

    // DeliveryTracking "dispatches from" a Vendor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_vendor")
    @JsonIgnore
    private Vendor pickupVendor;

    @Column(name = "pickup_vendor", insertable = false, updatable = false)
    private Long pickupVendorId;

    // DeliveryTracking "routes from" an Address
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_user")
    @JsonIgnore
    private Address pickupUser;

    @Column(name = "pickup_user", insertable = false, updatable = false)
    private Long pickupUserId;

    @Column(name = "assignment_status")
    private String assignmentStatus;

    @Column(name = "eta")
    private String eta;

    // Many tracking records belong to one DeliveryPartner
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id", nullable = false)
    private DeliveryPartner deliveryPartner;

    public DeliveryTracking() {}

    public UUID getAssignmentId() { return assignmentId; }
    public void setAssignmentId(UUID assignmentId) { this.assignmentId = assignmentId; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Vendor getPickupVendor() { return pickupVendor; }
    public void setPickupVendor(Vendor pickupVendor) { this.pickupVendor = pickupVendor; }

    public Long getPickupVendorId() { return pickupVendorId; }
    public void setPickupVendorId(Long pickupVendorId) { this.pickupVendorId = pickupVendorId; }

    public Address getPickupUser() { return pickupUser; }
    public void setPickupUser(Address pickupUser) { this.pickupUser = pickupUser; }

    public Long getPickupUserId() { return pickupUserId; }
    public void setPickupUserId(Long pickupUserId) { this.pickupUserId = pickupUserId; }

    public String getAssignmentStatus() { return assignmentStatus; }
    public void setAssignmentStatus(String assignmentStatus) { this.assignmentStatus = assignmentStatus; }

    public String getEta() { return eta; }
    public void setEta(String eta) { this.eta = eta; }

    public DeliveryPartner getDeliveryPartner() { return deliveryPartner; }
    public void setDeliveryPartner(DeliveryPartner deliveryPartner) { this.deliveryPartner = deliveryPartner; }
}