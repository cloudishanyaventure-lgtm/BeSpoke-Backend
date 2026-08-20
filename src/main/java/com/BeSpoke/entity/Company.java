package com.BeSpoke.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An onboarded company (tenant): a design studio or a product vendor. Staff
 * users and leads hang off a company; platform admins and CUSTOMERs have none.
 * New columns are DB-nullable (ddl-auto:update on existing rows) — defaults
 * live in the getters and the SeedRunner migration block.
 */
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    /** Legacy single city; kept in sync with headquartersCity by its setter. */
    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String headquartersCity;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 2000)
    private String about;

    @Column(length = 1000)
    private String logoUrl;

    /** Wide hero photo behind the public directory card and profile header. */
    @Column(length = 1000)
    private String coverUrl;

    /** Year the studio started — the public card shows the years since. */
    private Integer foundedYear;

    /** Brand accent used to theme the studio's workspace and public card. */
    @Column(length = 20)
    private String accentColor;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    private CompanyType type;

    /** Solo-designer studio (one-person design company). */
    private Boolean solo;

    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;

    @Column(length = 30)
    private String gstin;

    @Column(length = 30)
    private String pan;

    @Column(length = 30)
    private String cin;

    @Column(length = 255)
    private String registeredName;

    @Column(length = 500)
    private String registeredAddress;

    // EAGER: tiny collections, and companies travel on detached users (open-in-view is off).
    // SUBSELECT, not the default JOIN: a Lead eager-loads six User/Company references, and
    // joining three collections per Company multiplies the row count once per reference —
    // a single lead ran to hundreds of millions of rows and exhausted the heap. Separate
    // selects keep these eager (no lazy-init on detached companies) without the product.
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "company_kyc_docs", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "doc_url", length = 1000)
    private List<String> kycDocUrls = new ArrayList<>();

    /** Cities the studio actually works in, beyond its headquarters. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "company_operational_cities", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "city", length = 120)
    private List<String> operationalCities = new ArrayList<>();

    /** Design styles the studio works in — the public directory filters on these. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "company_styles", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "style", length = 60)
    private List<String> styles = new ArrayList<>();

    /** Project photos shown on the public profile; the first one backs the card. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "company_portfolio", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> portfolioUrls = new ArrayList<>();

    /** Empty set == all applicable roles enabled (grandfathering trick). */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "company_enabled_roles", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "role", length = 40)
    private Set<Role> enabledRoles = new LinkedHashSet<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Company() {
    }

    public Company(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    /** Code default: the legacy city, for rows the boot migration has not touched yet. */
    public String getHeadquartersCity() {
        return headquartersCity != null ? headquartersCity : city;
    }

    /** Writes both columns — `city` stays populated for backwards compatibility. */
    public void setHeadquartersCity(String headquartersCity) {
        this.headquartersCity = headquartersCity;
        this.city = headquartersCity;
    }

    public List<String> getOperationalCities() {
        return operationalCities;
    }

    public void setOperationalCities(List<String> operationalCities) {
        this.operationalCities = operationalCities;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(Integer foundedYear) {
        this.foundedYear = foundedYear;
    }

    public List<String> getStyles() {
        return styles;
    }

    public void setStyles(List<String> styles) {
        this.styles = styles;
    }

    public List<String> getPortfolioUrls() {
        return portfolioUrls;
    }

    public void setPortfolioUrls(List<String> portfolioUrls) {
        this.portfolioUrls = portfolioUrls;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Code default DESIGN — pre-V2 rows are design studios. */
    public CompanyType getType() {
        return type != null ? type : CompanyType.DESIGN;
    }

    public void setType(CompanyType type) {
        this.type = type;
    }

    public Boolean getSolo() {
        return solo;
    }

    public void setSolo(Boolean solo) {
        this.solo = solo;
    }

    /**
     * May a customer lead be attached to this company? Only live, KYC-verified design
     * studios — vendors have no lead book, and an unverified studio must not receive
     * customer PII. Enforced at every lead entry point (signup, enquiry, transfer).
     */
    public boolean canTakeLeads() {
        return isActive() && getType() == CompanyType.DESIGN && getKycStatus() == KycStatus.VERIFIED;
    }

    /** Code default PENDING; pre-V2 rows are grandfathered to VERIFIED by the SeedRunner. */
    public KycStatus getKycStatus() {
        return kycStatus != null ? kycStatus : KycStatus.PENDING;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public String getRegisteredName() {
        return registeredName;
    }

    public void setRegisteredName(String registeredName) {
        this.registeredName = registeredName;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }

    public List<String> getKycDocUrls() {
        return kycDocUrls;
    }

    public void setKycDocUrls(List<String> kycDocUrls) {
        this.kycDocUrls = kycDocUrls;
    }

    public Set<Role> getEnabledRoles() {
        return enabledRoles;
    }

    public void setEnabledRoles(Set<Role> enabledRoles) {
        this.enabledRoles = enabledRoles;
    }

    /** Stored set empty → all applicable roles; else stored set + DIRECTOR (never disableable). */
    public Set<Role> effectiveEnabledRoles() {
        if (enabledRoles == null || enabledRoles.isEmpty()) {
            return Role.applicableTo(getType());
        }
        Set<Role> effective = EnumSet.copyOf(enabledRoles);
        effective.add(Role.DIRECTOR);
        return effective;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
