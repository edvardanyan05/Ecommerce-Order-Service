package com.ecommerce.order_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * A customer order persisted in the {@code orders} table.
 * <p>
 * Uses {@code @Getter}/{@code @Setter} rather than {@code @Data}: Lombok-generated
 * {@code equals}/{@code hashCode} over all fields misbehave for JPA entities whose
 * ID is assigned by the database.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    public static final String DEFAULT_STATUS = "PENDING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Column(nullable = false)
    private String customerEmail;

    @NotBlank
    @Column(nullable = false)
    private String productName;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 20)
    private String status;

    @PrePersist
    void applyDefaults() {
        if (status == null || status.isBlank()) {
            status = DEFAULT_STATUS;
        }
    }
}
