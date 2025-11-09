package com.example.boutique.dto;

import java.math.BigDecimal;
import java.util.List;

public class VenteRequestDto {
    private List<CartItemDto> cart;
    private String saleType;
    private String paymentMethod;
    private Long clientId;
    private BigDecimal discountAmount;
    private String codeCaissier;

    public List<CartItemDto> getCart() {
        return cart;
    }

    public void setCart(List<CartItemDto> cart) {
        this.cart = cart;
    }

    public String getSaleType() {
        return saleType;
    }

    public void setSaleType(String saleType) {
        this.saleType = saleType;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getCodeCaissier() {
        return codeCaissier;
    }

    public void setCodeCaissier(String codeCaissier) {
        this.codeCaissier = codeCaissier;
    }
}
