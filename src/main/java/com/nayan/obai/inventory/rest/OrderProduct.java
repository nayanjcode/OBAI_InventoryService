package com.nayan.obai.inventory.rest;

import com.nayan.obai.inventory.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderProduct
{
	private UUID orderId;
	private List<Product> products;

}
