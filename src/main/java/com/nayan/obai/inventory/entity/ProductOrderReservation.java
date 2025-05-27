package com.nayan.obai.inventory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class ProductOrderReservation
{
	@Id
	@GeneratedValue
	private long reservationId;
	private UUID orderId;
	private UUID productId;
	private int reservedQuantity;
	private long timestamp;
}
