package com.BeSpoke.dto;

import com.BeSpoke.entity.Company;

/** Public directory card for a verified vendor company. */
public record ShopVendorDto(Long id, String name, String slug, String city, String about,
                            String logoUrl, String accentColor, long productCount) {

    public static ShopVendorDto from(Company company, long productCount) {
        return new ShopVendorDto(company.getId(), company.getName(), company.getSlug(),
                company.getCity(), company.getAbout(), company.getLogoUrl(),
                company.getAccentColor(), productCount);
    }
}
