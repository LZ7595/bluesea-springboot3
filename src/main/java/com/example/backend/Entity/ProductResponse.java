package com.example.backend.Entity;

public class ProductResponse {
    private Product product;
    private FlashSale flashSale;

    public ProductResponse(Product product, FlashSale flashSale) {
        this.product = product;
        this.flashSale = flashSale;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public FlashSale getFlashSale() {
        return flashSale;
    }

    public void setFlashSale(FlashSale flashSale) {
        this.flashSale = flashSale;
    }
}
