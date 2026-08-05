package com.BeSpoke.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Platform hierarchy. SUPER_ADMIN/ADMIN operate the BeSpoke platform (company
 * == null); DIRECTOR..PRODUCT_SME are company staff (design companies use the
 * full design set, vendor companies the vendor subset); CUSTOMER is a client
 * account.
 */
public enum Role {
    CUSTOMER,
    // platform (BeSpoke) — company == null
    SUPER_ADMIN, ADMIN,
    // company staff (design companies use all 9; vendor companies use the marked subset)
    DIRECTOR,             // design + vendor
    ACCOUNT_MANAGER,      // design + vendor
    PRINCIPAL_ARCHITECT,  // design
    DESIGN_MANAGER,       // design
    DESIGNER,             // design
    PROJECT_MANAGER,      // design
    SALES_MANAGER,        // design + vendor
    CUSTOMER_CONSULTANT,  // design + vendor
    SALES_EXECUTIVE,      // design
    PRODUCT_MANAGER,      // vendor
    PRODUCT_SME;          // vendor

    private static final Set<Role> DESIGN_ROLES = Collections.unmodifiableSet(EnumSet.of(
            DIRECTOR, ACCOUNT_MANAGER, PRINCIPAL_ARCHITECT, DESIGN_MANAGER, DESIGNER,
            PROJECT_MANAGER, SALES_MANAGER, CUSTOMER_CONSULTANT, SALES_EXECUTIVE));

    private static final Set<Role> VENDOR_ROLES = Collections.unmodifiableSet(EnumSet.of(
            DIRECTOR, ACCOUNT_MANAGER, SALES_MANAGER, CUSTOMER_CONSULTANT,
            PRODUCT_MANAGER, PRODUCT_SME));

    private static final Map<Role, Role> DESIGN_REPORTS = Map.of(
            ACCOUNT_MANAGER, DIRECTOR,
            PRINCIPAL_ARCHITECT, DIRECTOR,
            DESIGN_MANAGER, DIRECTOR,
            SALES_MANAGER, DIRECTOR,
            DESIGNER, DESIGN_MANAGER,
            PROJECT_MANAGER, DESIGN_MANAGER,
            CUSTOMER_CONSULTANT, SALES_MANAGER,
            SALES_EXECUTIVE, CUSTOMER_CONSULTANT);

    private static final Map<Role, Role> VENDOR_REPORTS = Map.of(
            ACCOUNT_MANAGER, DIRECTOR,
            PRODUCT_MANAGER, DIRECTOR,
            SALES_MANAGER, DIRECTOR,
            PRODUCT_SME, PRODUCT_MANAGER,
            CUSTOMER_CONSULTANT, SALES_MANAGER);

    /** BeSpoke platform operators — no company. */
    public boolean isPlatform() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /** Company staff — always attached to a company. Not platform, not customer. */
    public boolean isStaff() {
        return this != CUSTOMER && !isPlatform();
    }

    /** Sees the whole company book; assigned-only roles see just their own work. */
    public boolean seesWholeCompany() {
        return this == DIRECTOR || this == ACCOUNT_MANAGER || this == PRINCIPAL_ARCHITECT
                || this == DESIGN_MANAGER || this == SALES_MANAGER || this == PRODUCT_MANAGER;
    }

    /** May assign work and manage the team. */
    public boolean isManager() {
        return isPlatform() || this == DIRECTOR || this == DESIGN_MANAGER || this == SALES_MANAGER;
    }

    /** May see money: budgets, invoices, payments, revenue. */
    public boolean seesFinance() {
        return isPlatform() || this == DIRECTOR || this == ACCOUNT_MANAGER;
    }

    /** May see commercial terms: quote line items, rates, totals. Mirrors the /api/quotes ACL. */
    public boolean seesQuotes() {
        return isPlatform() || this == DIRECTOR || this == ACCOUNT_MANAGER
                || this == DESIGN_MANAGER || this == SALES_MANAGER || this == CUSTOMER_CONSULTANT;
    }

    /** May approve drawings in the WIP → approved pipeline. */
    public boolean canApproveDrawings() {
        return this == DIRECTOR || this == PRINCIPAL_ARCHITECT || this == DESIGN_MANAGER;
    }

    /** Works the lead funnel: design-company roles plus the platform. */
    public boolean worksLeads() {
        return isPlatform() || DESIGN_ROLES.contains(this);
    }

    /** Default reporting line for this role in a company of the given type; null = top. */
    public Role reportsTo(CompanyType type) {
        return (type == CompanyType.VENDOR ? VENDOR_REPORTS : DESIGN_REPORTS).get(this);
    }

    /** Roles a company of the given type can employ. */
    public static Set<Role> applicableTo(CompanyType type) {
        return type == CompanyType.VENDOR ? VENDOR_ROLES : DESIGN_ROLES;
    }
}
