package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "quote_items")
public class QuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteItemCategory category;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal rate;

    @Column(nullable = false)
    private int gstPct;

    public QuoteItem() {
    }

    public QuoteItem(Quote quote, QuoteItemCategory category, String description,
                     BigDecimal qty, BigDecimal rate, int gstPct) {
        this.quote = quote;
        this.category = category;
        this.description = description;
        this.qty = qty;
        this.rate = rate;
        this.gstPct = gstPct;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    public QuoteItemCategory getCategory() {
        return category;
    }

    public void setCategory(QuoteItemCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public int getGstPct() {
        return gstPct;
    }

    public void setGstPct(int gstPct) {
        this.gstPct = gstPct;
    }
}
