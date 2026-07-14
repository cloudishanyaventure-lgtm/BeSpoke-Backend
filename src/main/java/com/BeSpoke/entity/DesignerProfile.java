package com.BeSpoke.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "designer_profiles")
public class DesignerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 2000)
    private String bio;

    /** Comma-separated specialties, e.g. "Kitchen, Wardrobe, Full Home". */
    private String specialties;

    private String city;

    private Double rating;

    private BigDecimal startingPrice;

    @Column(nullable = false)
    private long viewCount = 0L;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "designer_portfolio_images", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> portfolioImageUrls = new ArrayList<>();

    public DesignerProfile() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getSpecialties() {
        return specialties;
    }

    public void setSpecialties(String specialties) {
        this.specialties = specialties;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public List<String> getPortfolioImageUrls() {
        return portfolioImageUrls;
    }

    public void setPortfolioImageUrls(List<String> portfolioImageUrls) {
        this.portfolioImageUrls = portfolioImageUrls;
    }
}
