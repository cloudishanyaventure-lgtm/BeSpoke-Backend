package com.BeSpoke.service;

import com.BeSpoke.entity.PlatformOption;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The picklists the product ships with. These are starting values only — once a list
 * exists in the database the admin owns it, and this class is never consulted again
 * (see {@link PlatformOptionService#seedDefaults()}).
 */
final class PlatformOptionDefaults {

    static final Map<String, List<PlatformOption>> LISTS = build();

    private PlatformOptionDefaults() {
    }

    private static Map<String, List<PlatformOption>> build() {
        Map<String, List<PlatformOption>> lists = new LinkedHashMap<>();

        plain(lists, "CITY", "Delhi", "Gurugram", "Noida", "Ghaziabad", "Faridabad", "Mumbai",
                "Pune", "Bengaluru", "Hyderabad", "Chennai", "Kolkata", "Jaipur", "Chandigarh",
                "Other");

        plain(lists, "DESIGN_STYLE", "Modern minimal", "Contemporary", "Scandinavian",
                "Mid-century modern", "Industrial", "Bohemian", "Traditional Indian", "Japandi",
                "Art deco", "Rustic / farmhouse");

        coded(lists, "PROPERTY_TYPE",
                entry("APARTMENT", "Apartment"),
                entry("BUILDER_FLOOR", "Builder floor"),
                entry("VILLA", "Villa"),
                entry("INDEPENDENT_HOUSE", "Independent house"),
                entry("COMMERCIAL", "Commercial space"));

        coded(lists, "BUDGET_BAND",
                entry("UNDER_5L", "Under ₹5 Lakh"),
                entry("L5_10", "₹5 – 10 Lakh"),
                entry("L10_25", "₹10 – 25 Lakh"),
                entry("L25_50", "₹25 – 50 Lakh"),
                entry("ABOVE_50L", "Above ₹50 Lakh"));

        coded(lists, "STORAGE_NEED",
                entry("LOW", "Low"), entry("MEDIUM", "Medium"), entry("HIGH", "High"));

        plain(lists, "PROJECT_SEGMENT", "Residential", "Commercial");

        plain(lists, "RESIDENTIAL_SCOPE", "Full home interiors", "Renovation of selected rooms",
                "Modular kitchen & wardrobes", "Styling & decor refresh", "Home office setup");

        plain(lists, "COMMERCIAL_SCOPE", "Commercial fit-out", "Full office interiors",
                "Styling & decor refresh");

        plain(lists, "OCCUPANCY", "Occupied — living in the space", "Vacant — possession taken",
                "Under construction / awaiting possession");

        plain(lists, "GUEST_FREQUENCY", "Rarely", "Monthly", "Weekly", "Almost daily");

        plain(lists, "WOOD_TONE", "Light / Ash", "Medium / Walnut", "Dark / Wenge",
                "Painted finishes", "Mixed tones");

        plain(lists, "METAL_FINISH", "Matte black", "Brushed brass", "Chrome / steel",
                "Antique bronze", "Mixed metals");

        plain(lists, "BUDGET_FLEXIBILITY", "Firm — please don't exceed",
                "Some flexibility for the right ideas", "Flexible — quality first");

        plain(lists, "PAYMENT_MILESTONE", "Standard 30–40–30 schedule",
                "Milestone-linked payments", "Monthly instalments", "Discuss with the team");

        // note = the room-catalog key this choice pulls its item list from; blank = none.
        noted(lists, "ROOM_CHOICE",
                noteEntry("LIVING_ROOM", "Living room", "LIVING_ROOM"),
                noteEntry("KITCHEN", "Kitchen", "KITCHEN"),
                noteEntry("MASTER_BEDROOM", "Master bedroom", "BEDROOM"),
                noteEntry("BEDROOM", "Bedroom", "BEDROOM"),
                noteEntry("KIDS_ROOM", "Kids' room", "BEDROOM"),
                noteEntry("BATHROOM", "Bathroom", "BATHROOM"),
                noteEntry("DINING", "Dining area", null),
                noteEntry("STUDY", "Study / home office", "STUDY"),
                noteEntry("BALCONY", "Balcony", "TERRACE"),
                noteEntry("TERRACE", "Terrace & garden", "TERRACE"),
                noteEntry("POOJA", "Pooja room", null),
                noteEntry("FOYER", "Foyer / entrance", null));

        // note = the body copy under each step on the material library landing page.
        noted(lists, "MATERIAL_HOW_IT_WORKS",
                noteEntry("select", "Selects material",
                        "Browse and choose from our vast library."),
                noteEntry("project", "Adds to project",
                        "Organize materials into specific project boards."),
                noteEntry("compare", "Compares brands",
                        "Evaluate specifications and prices side-by-side."),
                noteEntry("quote", "Get multiple quotation",
                        "Receive bids from verified suppliers."),
                noteEntry("order", "Orders material",
                        "Proceed with the best quote and track delivery."));

        return lists;
    }

    /** A list whose stored value is the label itself. */
    private static void plain(Map<String, List<PlatformOption>> lists, String key, String... labels) {
        List<PlatformOption> rows = new ArrayList<>();
        int order = 0;
        for (String label : labels) {
            rows.add(new PlatformOption(key, label, label, null, order++));
        }
        lists.put(key, rows);
    }

    /** A list that stores a stable code and shows a label. */
    private static void coded(Map<String, List<PlatformOption>> lists, String key, String[]... pairs) {
        List<PlatformOption> rows = new ArrayList<>();
        int order = 0;
        for (String[] pair : pairs) {
            rows.add(new PlatformOption(key, pair[0], pair[1], null, order++));
        }
        lists.put(key, rows);
    }

    /** A list that carries a second line of data in `note`. */
    private static void noted(Map<String, List<PlatformOption>> lists, String key, String[]... triples) {
        List<PlatformOption> rows = new ArrayList<>();
        int order = 0;
        for (String[] triple : triples) {
            rows.add(new PlatformOption(key, triple[0], triple[1], triple[2], order++));
        }
        lists.put(key, rows);
    }

    private static String[] entry(String value, String label) {
        return new String[]{value, label};
    }

    private static String[] noteEntry(String value, String label, String note) {
        return new String[]{value, label, note};
    }
}
