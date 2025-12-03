package com.example.boutique.dto;

public class PanierTemporaireDto {

    private Long id;
    private Long tabId;
    private String cartData;

    public PanierTemporaireDto() {
    }

    public PanierTemporaireDto(Long id, Long tabId, String cartData) {
        this.id = id;
        this.tabId = tabId;
        this.cartData = cartData;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTabId() {
        return tabId;
    }

    public void setTabId(Long tabId) {
        this.tabId = tabId;
    }

    public String getCartData() {
        return cartData;
    }

    public void setCartData(String cartData) {
        this.cartData = cartData;
    }
}
