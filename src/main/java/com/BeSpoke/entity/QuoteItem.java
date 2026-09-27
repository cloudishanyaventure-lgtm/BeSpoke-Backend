package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal rate;

    @Column(nullable = false)
    private int gstPct;

    /**
     * The space this line belongs to, e.g. "Master Bedroom" — set on a room's own line and
     * on each element under it, so the proposal can render as rooms with their elements
     * nested. Null for a standalone hand-typed line.
     */
    @Column(length = 200)
    private String section;

    /** True on the room's own line (the group header); false on the elements under it. */
    @Column(nullable = false)
    private boolean heading = false;

    public QuoteItem() {
    }

    public QuoteItem(Quote quote, String description,
                     BigDecimal qty, BigDecimal rate, int gstPct) {
        this.quote = quote;
        this.description = description;
        this.qty = qty;
        this.rate = rate;
        this.gstPct = gstPct;
    }

    public QuoteItem(Quote quote, String description, BigDecimal qty, BigDecimal rate,
                     int gstPct, String section, boolean heading) {
        this(quote, description, qty, rate, gstPct);
        this.section = section;
        this.heading = heading;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public boolean isHeading() {
        return heading;
    }

    public void setHeading(boolean heading) {
        this.heading = heading;
    }
}
