package com.sherdle.universal.providers.woocommerce;

public class WooCommerceProductFilter {
    private double minPrice;
    private double maxPrice;

    private boolean onlyFeatured;
    private boolean onlySale;
    private boolean onlyInStock;

    public WooCommerceProductFilter(){
    }

    public WooCommerceProductFilter minPrice(double minPrice){
        this.minPrice = minPrice;
        return this;
    }

    public WooCommerceProductFilter maxPrice(double maxPrice){
        this.maxPrice = maxPrice;
        return this;
    }

    public WooCommerceProductFilter onlySale(boolean onlySale){
        this.onlySale = onlySale;
        return this;
    }

    public WooCommerceProductFilter onlyFeatured(boolean onlyFeatured){
        this.onlyFeatured = onlyFeatured;
        return this;
    }


    public WooCommerceProductFilter onlyInStock(boolean onlyInStock){
        this.onlyInStock = onlyInStock;
        return this;
    }

    public String getQuery(){
        StringBuilder query = new StringBuilder();
        if (minPrice != 0)
            query.append("&min_price=" + minPrice);
        if (maxPrice != 0)
            query.append("&max_price=" + maxPrice);
        if (onlyFeatured)
            query.append("&featured=" + Boolean.toString(onlyFeatured));
        if (onlySale)
            query.append("&on_sale=" + Boolean.toString(onlySale));
        if (onlyInStock)
            query.append("&in_stock=" + Boolean.toString(onlyInStock));
        return query.toString();
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public boolean isOnlyFeatured() {
        return onlyFeatured;
    }

    public boolean isOnlySale() {
        return onlySale;
    }

    public boolean isOnlyInStock() {
        return onlyInStock;
    }

    public void clearFilters() {
        this.minPrice = 0;
        this.maxPrice = 0;
    }

}
