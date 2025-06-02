package com.nayan.obai.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Product")
@EqualsAndHashCode(of={"quantity", "productId"})
@ToString
public class Product
{
//	// I don't this this id is required. We already have product id
//	// UUID column that is not the primary key
//	@Column(nullable = false, unique = true, updatable = false, name = "id")
//	private UUID id;

	@Id
	@GeneratedValue
	@Column(name = "productId")
	private UUID productId;

	@Column(name = "quantity")
	private Integer quantity;

	@UpdateTimestamp
	@Column(name = "lastUpdated")
	private LocalDateTime lastUpdated;

//	// Automatically generate UUID before persisting
//	@PrePersist
//	public void generateUUID()
//	{
//		if (id == null)
//		{
//			id = UUID.randomUUID();
//		}
//	}

}
