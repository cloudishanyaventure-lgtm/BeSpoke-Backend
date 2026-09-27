package com.BeSpoke.service;

import com.BeSpoke.dto.InvoiceDto;
import com.BeSpoke.dto.RequirementFormDto;
import com.BeSpoke.dto.RoomDto;
import com.BeSpoke.dto.RoomItemDto;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders invoice and PRD documents to PDF (openhtmltopdf → PDFBox) for the email
 * attachment and the download endpoints. The built-in PDF fonts don't carry the rupee
 * glyph, so money is written "Rs. 1,234.00" rather than with ₹.
 */
@Service
public class PdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public byte[] invoice(InvoiceDto inv) {
        InvoiceDto.Party s = inv.seller();
        InvoiceDto.Party b = inv.buyer();
        BigDecimal gst = inv.total().subtract(inv.amount());
        BigDecimal balance = inv.total().subtract(inv.paid());

        StringBuilder body = new StringBuilder();
        body.append("<div class='row head'>")
                .append("<div class='seller'>")
                .append("<div class='big'>").append(esc(s != null ? s.name() : "BeSpoke")).append("</div>");
        if (s != null) {
            if (notBlank(s.address())) body.append("<div class='muted'>").append(esc(s.address())).append("</div>");
            if (notBlank(s.city())) body.append("<div class='muted'>").append(esc(s.city())).append("</div>");
            if (notBlank(s.phone())) body.append("<div class='muted'>").append(esc(s.phone())).append("</div>");
            if (notBlank(s.email())) body.append("<div class='muted'>").append(esc(s.email())).append("</div>");
            if (notBlank(s.gstin())) {
                body.append("<div class='gst'>GSTIN: ").append(esc(s.gstin()));
                if (notBlank(s.pan())) body.append(" &#183; PAN: ").append(esc(s.pan()));
                body.append("</div>");
            }
        }
        body.append("</div>")
                .append("<div class='meta'>")
                .append("<div class='big'>INVOICE</div>")
                .append("<div>No: ").append(esc(inv.number())).append("</div>")
                .append("<div>Date: ").append(date(inv.createdAt())).append("</div>");
        if (inv.dueDate() != null) body.append("<div>Due: ").append(date(inv.dueDate())).append("</div>");
        body.append("<div class='status'>").append(esc(inv.status().replace('_', ' '))).append("</div>")
                .append("</div></div>");

        body.append("<div class='billto'><div class='label'>BILL TO</div>")
                .append("<div class='b'>").append(esc(b != null && notBlank(b.name()) ? b.name()
                        : (inv.clientName() != null ? inv.clientName() : "-"))).append("</div>");
        if (b != null) {
            if (notBlank(b.city())) body.append("<div class='muted'>").append(esc(b.city())).append("</div>");
            if (notBlank(b.phone())) body.append("<div class='muted'>").append(esc(b.phone())).append("</div>");
            if (notBlank(b.email())) body.append("<div class='muted'>").append(esc(b.email())).append("</div>");
        }
        if (notBlank(inv.projectName())) {
            body.append("<div class='muted'>Project: ").append(esc(inv.projectName())).append("</div>");
        }
        body.append("</div>");

        body.append("<table class='items'><thead><tr>")
                .append("<th>Description</th><th class='r'>Amount</th><th class='r'>GST</th><th class='r'>Line total</th>")
                .append("</tr></thead><tbody><tr>")
                .append("<td>").append(esc(inv.title())).append("</td>")
                .append("<td class='r'>").append(money(inv.amount())).append("</td>")
                .append("<td class='r'>").append(inv.gstPct()).append("%</td>")
                .append("<td class='r'>").append(money(inv.total())).append("</td>")
                .append("</tr></tbody></table>");

        body.append("<table class='totals'>")
                .append(totalRow("Subtotal", money(inv.amount()), false))
                .append(totalRow("GST (" + inv.gstPct() + "%)", money(gst), false))
                .append(totalRow("Total", money(inv.total()), true))
                .append(totalRow("Paid", money(inv.paid()), false))
                .append(totalRow("Balance due", money(balance), true))
                .append("</table>");

        if (inv.payments() != null && !inv.payments().isEmpty()) {
            body.append("<div class='label mt'>PAYMENTS RECEIVED</div><table class='pay'><tbody>");
            for (var p : inv.payments()) {
                body.append("<tr><td>").append(p.paidAt() != null ? date(p.paidAt()) : "-").append("</td>")
                        .append("<td>").append(esc(String.valueOf(p.mode()))).append("</td>")
                        .append("<td class='r'>").append(money(p.amount())).append("</td></tr>");
            }
            body.append("</tbody></table>");
        }

        body.append("<div class='foot'>This is a computer-generated invoice.")
                .append(s != null && notBlank(s.email()) ? " For any query, write to " + esc(s.email()) + "." : "")
                .append("</div>");

        return render("Invoice " + esc(inv.number()), body.toString());
    }

    public byte[] prd(RequirementFormDto form, String customerName) {
        StringBuilder body = new StringBuilder();
        body.append("<div class='big'>Project Requirement Document</div>");
        if (notBlank(customerName)) body.append("<div class='muted'>For ").append(esc(customerName)).append("</div>");
        int spaces = form.rooms() == null ? 0 : form.rooms().size();
        body.append("<div class='muted'>").append(spaces).append(spaces == 1 ? " space" : " spaces").append("</div>");

        body.append("<table class='overview'><tbody>")
                .append(fact("Project type", form.projectType()))
                .append(fact("Scope of work", form.scopeOfWork()))
                .append(fact("Space type", form.spaceType()))
                .append(fact("Preferred style", form.preferredStyle()))
                .append("</tbody></table>");

        if (form.rooms() != null) {
            for (RoomDto r : form.rooms()) {
                body.append("<div class='room'>");
                body.append("<div class='room-h'>").append(esc(r.label()));
                if (notBlank(r.familyMember())) body.append(" &#8212; ").append(esc(r.familyMember()));
                if (notBlank(r.floor())) body.append(" (").append(esc(r.floor())).append(")");
                String size = dims(r.lengthFt(), r.widthFt(), r.depthFt(), r.heightFt(), r.areaSqft());
                if (notBlank(size)) body.append(" <span class='muted'>&#183; ").append(size).append("</span>");
                body.append("</div>");
                if (r.items() != null && !r.items().isEmpty()) {
                    body.append("<ul>");
                    for (RoomItemDto it : r.items()) {
                        body.append("<li>").append(esc(it.item()));
                        String isize = dims(it.lengthFt(), it.widthFt(), it.depthFt(), it.heightFt(), it.areaSqft());
                        if (notBlank(isize)) body.append(" <span class='muted'>(").append(isize).append(")</span>");
                        body.append("</li>");
                    }
                    body.append("</ul>");
                }
                if (notBlank(r.notes())) body.append("<div class='muted note'>").append(esc(r.notes())).append("</div>");
                body.append("</div>");
            }
        }

        body.append("<div class='foot'>Please review this on your portal and approve, or request changes.</div>");
        return render("Project Requirement Document", body.toString());
    }

    // ---- rendering ----

    private byte[] render(String title, String bodyHtml) {
        String html = "<html><head><meta charset='UTF-8'/><style>"
                + "@page { margin: 16mm; }"
                + "body { font-family: Helvetica, sans-serif; font-size: 11px; color: #111; }"
                + ".big { font-size: 20px; font-weight: bold; }"
                + ".muted { color: #555; }"
                + ".row { display: block; }"
                + ".head { border-bottom: 2px solid #111; padding-bottom: 10px; }"
                + ".seller { display: inline-block; width: 60%; vertical-align: top; }"
                + ".meta { display: inline-block; width: 39%; vertical-align: top; text-align: right; }"
                + ".meta .big { font-size: 22px; }"
                + ".status { display: inline-block; border: 1px solid #111; padding: 1px 6px; font-weight: bold;"
                + " text-transform: uppercase; font-size: 10px; margin-top: 4px; }"
                + ".gst { margin-top: 3px; }"
                + ".billto { margin-top: 16px; } .billto .b { font-weight: bold; }"
                + ".label { color: #888; font-size: 10px; font-weight: bold; text-transform: uppercase; }"
                + ".mt { margin-top: 16px; }"
                + "table { border-collapse: collapse; width: 100%; }"
                + "table.items { margin-top: 18px; } table.items th { border-bottom: 1px solid #111; text-align: left; padding: 5px 0; }"
                + "table.items td { border-bottom: 1px solid #ccc; padding: 5px 0; }"
                + ".r { text-align: right; }"
                + "table.totals { width: 50%; margin-left: 50%; margin-top: 10px; }"
                + "table.totals td { padding: 2px 0; }"
                + "table.pay { margin-top: 4px; } table.pay td { border-bottom: 1px solid #eee; padding: 3px 0; }"
                + ".overview { margin-top: 12px; } .overview td { padding: 2px 8px 2px 0; vertical-align: top; }"
                + ".overview .k { color: #888; width: 130px; }"
                + ".room { margin-top: 12px; page-break-inside: avoid; }"
                + ".room-h { font-weight: bold; border-bottom: 1px solid #ccc; padding-bottom: 3px; }"
                + ".room ul { margin: 4px 0 0 0; padding-left: 18px; } .room li { margin: 1px 0; }"
                + ".note { margin-top: 3px; } "
                + ".foot { margin-top: 30px; border-top: 1px solid #ccc; padding-top: 8px; text-align: center; color: #777; font-size: 10px; }"
                + "</style><title>" + esc(title) + "</title></head><body>" + bodyHtml + "</body></html>";
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(os).run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Could not build the PDF", e);
        }
    }

    private static String totalRow(String label, String value, boolean strong) {
        String open = strong ? "<td class='k' style='font-weight:bold;border-top:1px solid #111'>"
                : "<td class='k'>";
        String vopen = strong ? "<td class='r' style='font-weight:bold;border-top:1px solid #111'>"
                : "<td class='r'>";
        return "<tr>" + open + esc(label) + "</td>" + vopen + value + "</td></tr>";
    }

    private static String fact(String k, String v) {
        if (!notBlank(v)) return "";
        return "<tr><td class='k'>" + esc(k) + "</td><td>" + esc(v) + "</td></tr>";
    }

    private static String dims(Double l, Double w, Double d, Double h, Double area) {
        StringBuilder sb = new StringBuilder();
        appendDim(sb, "L", l);
        appendDim(sb, "W", w);
        appendDim(sb, "D", d);
        appendDim(sb, "H", h);
        String out = sb.toString();
        if (area != null && area > 0) {
            out = out.isEmpty() ? Math.round(area) + " sqft" : out + ", " + Math.round(area) + " sqft";
        }
        return out;
    }

    private static void appendDim(StringBuilder sb, String k, Double v) {
        if (v != null && v > 0) {
            if (sb.length() > 0) sb.append("&#215;");
            sb.append(k).append(fmtNum(v));
        }
    }

    private static String fmtNum(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private static String money(BigDecimal v) {
        return "Rs. " + MONEY.format(v == null ? BigDecimal.ZERO : v);
    }

    private static String date(LocalDate d) {
        return d == null ? "" : DATE.format(d);
    }

    private static String date(Instant i) {
        return i == null ? "" : DATE.format(i.atZone(IST).toLocalDate());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
