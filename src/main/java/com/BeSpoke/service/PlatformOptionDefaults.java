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

        // note = "lat,lng" — how the public directory sorts studios near-to-far from a
        // visitor who has shared their location. The admin owns these like any other
        // option, so a new city is added with its coordinates and nothing else changes.
        // The catalogue itself lives in cities.csv: ~190 rows is data, not Java.
        lists.put("CITY", cities());

        // What a vendor company supplies. Multi-select, set by the admin at approval.
        plain(lists, "VENDOR_CATEGORY", "Glass", "Electricals", "Modular furniture", "Lighting",
                "Flooring & tiles", "Sanitaryware & CP fittings", "Hardware & fittings",
                "Paints & finishes", "Kitchen appliances", "Furnishings & upholstery",
                "Stone & marble", "Carpentry & joinery", "False ceiling & POP",
                "Wallpaper & wall finishes", "HVAC", "Home automation");

        // The shop's left-hand rail.
        plain(lists, "SHOP_CATEGORY", "Sofas & seating", "Beds", "Wardrobes & storage", "Tables",
                "Chairs & stools", "Kitchen", "TV & media units", "Lighting", "Soft furnishings",
                "Decor & art", "Outdoor");

        // note = the SHOP_CATEGORY this sub-type sits under. That one column is the whole
        // parent-child link: adding a sub-type is one row, not a second table.
        noted(lists, "SHOP_SUBCATEGORY",
                sub("3-seater sofa", "Sofas & seating"),
                sub("2-seater sofa", "Sofas & seating"),
                sub("L-shaped sofa", "Sofas & seating"),
                sub("Recliner", "Sofas & seating"),
                sub("Sofa cum bed", "Sofas & seating"),
                sub("Chaise lounge", "Sofas & seating"),
                sub("Ottoman & pouffe", "Sofas & seating"),
                sub("King bed", "Beds"),
                sub("Queen bed", "Beds"),
                sub("Single bed", "Beds"),
                sub("Bunk bed", "Beds"),
                sub("Storage bed", "Beds"),
                sub("Upholstered bed", "Beds"),
                sub("Bedside table", "Beds"),
                sub("Sliding wardrobe", "Wardrobes & storage"),
                sub("Hinged wardrobe", "Wardrobes & storage"),
                sub("Walk-in wardrobe", "Wardrobes & storage"),
                sub("Chest of drawers", "Wardrobes & storage"),
                sub("Shoe rack", "Wardrobes & storage"),
                sub("Bookshelf", "Wardrobes & storage"),
                sub("Crockery unit", "Wardrobes & storage"),
                sub("Dining table", "Tables"),
                sub("Coffee table", "Tables"),
                sub("Side table", "Tables"),
                sub("Console table", "Tables"),
                sub("Study desk", "Tables"),
                sub("Dining chair", "Chairs & stools"),
                sub("Accent chair", "Chairs & stools"),
                sub("Office chair", "Chairs & stools"),
                sub("Bar stool", "Chairs & stools"),
                sub("Bench", "Chairs & stools"),
                sub("Rocking chair", "Chairs & stools"),
                sub("Modular kitchen", "Kitchen"),
                sub("Kitchen island", "Kitchen"),
                sub("Base unit", "Kitchen"),
                sub("Wall unit", "Kitchen"),
                sub("Tall unit", "Kitchen"),
                sub("Pantry unit", "Kitchen"),
                sub("Wall-mounted TV unit", "TV & media units"),
                sub("Floor TV unit", "TV & media units"),
                sub("Entertainment centre", "TV & media units"),
                sub("Ceiling light", "Lighting"),
                sub("Pendant light", "Lighting"),
                sub("Chandelier", "Lighting"),
                sub("Floor lamp", "Lighting"),
                sub("Table lamp", "Lighting"),
                sub("Wall sconce", "Lighting"),
                sub("Cove & profile lighting", "Lighting"),
                sub("Curtains", "Soft furnishings"),
                sub("Blinds", "Soft furnishings"),
                sub("Rugs & carpets", "Soft furnishings"),
                sub("Cushions", "Soft furnishings"),
                sub("Bedding", "Soft furnishings"),
                sub("Upholstery fabric", "Soft furnishings"),
                sub("Wall art", "Decor & art"),
                sub("Mirrors", "Decor & art"),
                sub("Planters", "Decor & art"),
                sub("Vases & showpieces", "Decor & art"),
                sub("Clocks", "Decor & art"),
                sub("Balcony seating", "Outdoor"),
                sub("Garden furniture", "Outdoor"),
                sub("Swing & hammock", "Outdoor"),
                sub("Outdoor lighting", "Outdoor"));

        plain(lists, "DESIGN_STYLE", "Modern minimal", "Contemporary", "Scandinavian",
                "Mid-century modern", "Industrial", "Bohemian", "Traditional Indian", "Japandi",
                "Art deco", "Rustic / farmhouse");

        // Rooms a studio files its portfolio photos under — each section shows only its
        // own photos on the public profile.
        plain(lists, "PORTFOLIO_SECTION", "Master bedroom", "Living room", "Kitchen",
                "Washroom", "Study room", "Terrace garden");

        // What a proposal is for, and the per-square-foot rate it is quoted at (note = the
        // rate, blank where the price is built by hand). The admin owns both.
        noted(lists, "QUOTE_RATE",
                noteEntry("DESIGN_DRAWINGS_STRUCTURE_INTERIOR", "Design & Drawings - Structure & Interior", "200"),
                noteEntry("DESIGN_DRAWINGS_STRUCTURE", "Design & Drawings - Structure", "70"),
                noteEntry("DESIGN_DRAWINGS_INTERIOR_MEP", "Design & Drawings - Interior with MEP", "100"),
                noteEntry("DESIGN_DRAWINGS_INTERIOR_NO_MEP", "Design & Drawings - Interior without MEP", "80"),
                noteEntry("DESIGN_WITH_PROJECT_MANAGEMENT", "Design with Project Management", "150"),
                noteEntry("TURNKEY", "Turnkey [ Design & Build ]", null),
                noteEntry("MODULAR_FURNITURE", "Modular Furniture", null));

        // How a won project is billed. note = "<percent of the quote>|<progress that triggers it>".
        // The advance is raised the moment the customer accepts; the rest follow the work,
        // measured in drawings the customer has signed off. The studio owns these numbers.
        noted(lists, "PAYMENT_SCHEDULE",
                noteEntry("ADVANCE", "Advance", "50|0"),
                noteEntry("MILESTONE", "Milestone", "30|40"),
                noteEntry("COMPLETION", "Completion", "20|70"));

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
                noteEntry("FOYER", "Foyer / entrance", null),
                noteEntry("LOBBY", "Lobby", null),
                noteEntry("FIRE_AREA", "Fire area / refuge", null),
                noteEntry("OTS", "Open to sky (OTS)", null),
                noteEntry("PARKING_STILT", "Stilt parking", null),
                noteEntry("PARKING", "Parking", null));

        // The WhatsApp bot's reply book: `value` = the keywords that pick the rule
        // (comma separated, matched anywhere in the message), `label` = what it replies.
        // The "*" rule answers everything else — that is the one that hands over to a
        // human, so the number to call lives there and the admin can change it without
        // a deploy. Order is the order they are tried.
        noted(lists, "WHATSAPP_BOT",
                noteEntry("hi,hello,hey,start", "Hi! This is BeSpoke. Your project is with"
                        + " our design team — ask me about your designer, your quote or"
                        + " your next step, and I'll help.", null),
                noteEntry("designer,architect,who", "Your designer is assigned once BeSpoke"
                        + " matches your brief to a studio in your city — usually the same"
                        + " day. You'll get an email the moment they're on it.", null),
                noteEntry("quote,price,cost,budget,boq", "Your quote is prepared after the"
                        + " design brief and PRD are agreed. You'll see it in your BeSpoke"
                        + " account, and you approve or ask for changes right there.", null),
                noteEntry("status,update,progress,where", "You can follow every stage —"
                        + " brief, designs, quote, project — in your BeSpoke account. Sign"
                        + " in with the code we email you.", null),
                noteEntry("brief,prd,requirement", "The design brief is the room-by-room"
                        + " questionnaire in your account. Fill what you can; your designer"
                        + " goes through the rest with you.", null),
                noteEntry("time,timeline,long,when", "Most homes take 8-12 weeks from"
                        + " approved design to handover, depending on scope. Your designer"
                        + " will give you the dates for your project.", null),
                noteEntry("*", "I'm not sure about that one — our team can help you"
                        + " properly. Please call us on 6387427935 and we'll pick it up"
                        + " from there.", null));

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

    /** A shop sub-type: its own label, filed under a SHOP_CATEGORY. */
    private static String[] sub(String label, String parentCategory) {
        return new String[]{label, label, parentCategory};
    }

    /** cities.csv — "name,lat,lng" per line, blank coordinates allowed ("Other"). */
    private static List<PlatformOption> cities() {
        List<PlatformOption> rows = new ArrayList<>();
        int order = 0;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                PlatformOptionDefaults.class.getResourceAsStream("/cities.csv"),
                java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",", 3);
                String name = parts[0].trim();
                String note = parts.length == 3 && !parts[1].isBlank()
                        ? parts[1].trim() + "," + parts[2].trim() : null;
                rows.add(new PlatformOption("CITY", name, name, note, order++));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("cities.csv is missing or unreadable", ex);
        }
        return rows;
    }
}
