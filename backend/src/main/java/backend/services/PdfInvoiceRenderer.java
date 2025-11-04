package backend.services;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import backend.models.Invoice;
import backend.models.Purchase;
import backend.models.User;
import backend.models.PaymentDetails;
import backend.services.invoice.InvoiceService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfInvoiceRenderer {

    private final InvoiceService invoiceService;

    /** Génère la page 1 en PDF (bytes) à partir d'une Invoice. */
    public byte[] renderInvoicePage1(Invoice invoice) {
        BigDecimal totalHT  = invoiceService.calculateTotalHT(invoice);
        BigDecimal totalTTC = invoiceService.calculateTotalTTC(invoice);

        Map<BigDecimal, BigDecimal> totalTvaByRate = Optional.ofNullable(invoice.getPurchases())
                .orElseGet(List::of).stream()
                .collect(Collectors.groupingBy(
                        Purchase::getTvaApplied,
                        Collectors.reducing(BigDecimal.ZERO,
                                p -> nv(p.getTotalTva()),
                                BigDecimal::add)));

        String html = buildHtml(invoice, totalHT, totalTTC, totalTvaByRate);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur génération PDF facture " + invoice.getReference(), e);
        }
    }

    // ---------------------------------------------------------------------
    // HTML
    // ---------------------------------------------------------------------

    private String buildHtml(Invoice invoice,
                             BigDecimal totalHT,
                             BigDecimal totalTTC,
                             Map<BigDecimal, BigDecimal> totalTvaByRate) {

        PaymentDetails pd = invoice.getPaymentDetails();
        User emitter  = (pd != null && pd.getUser() != null) ? pd.getUser() : invoice.getCreator();
        User customer = invoice.getCustomer();

        String ref          = safe(invoice.getReference());
        String billingDate  = fmtDate(invoice.getBillingDate());
        String dueDate      = fmtDate(invoice.getDueDate());
        String customerCode = (customer != null) ? safe(customer.getCodeCustomer()) : "-";
        String statut       = invoice.isConfirmed() ? "Confirmée" : "Brouillon";

        String densityClass = densityClass(invoice.getPurchases()); // compact/ultra si bcp de lignes

        String emitterBlock      = buildEmitterBlock(emitter);
        String customerBlock     = buildCustomerBlock(customer);
        String rows              = buildRows(invoice.getPurchases());
        String tvaLines          = buildTvaLines(totalTvaByRate);
        String conditionsRegl    = "Conditions de règlement : " + paymentTermLabel(pd);
        String virementBlock     = buildVirementBlock(pd);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
          .append("<html lang=\"fr\">\n")
          .append("<head>\n")
          .append("  <meta charset=\"utf-8\"/>\n")
          .append("  <style>\n")
          // Marges serrées pour tenir sur 1 page
          .append("    @page { size: A4; margin: 16mm 12mm 16mm 12mm; }\n")
          .append("    * { box-sizing: border-box; }\n")
          .append("    body { font-family: Arial, sans-serif; font-size: 11px; color: #111; line-height: 1.25; }\n")
          .append("    h1 { margin: 0 0 3mm 0; font-size: 20px; }\n")
          .append("    .meta { margin-bottom: 5mm; }\n")
          .append("    .row { display:flex; gap:12mm; margin: 2mm 0; flex-wrap: wrap; }\n")
          .append("    .row span { display:inline-block; min-width: 40mm; }\n")
          .append("    .cols { display:flex; gap:8mm; }\n")
          .append("    .box { border: 1px solid #bbb; padding: 3mm; flex:1; min-height: 26mm; break-inside: avoid; page-break-inside: avoid; }\n")
          .append("    .muted { color:#555; }\n")
          // Tableau compact + colonnes figées → évite les retours à la ligne imprévus
          .append("    table { width:100%; border-collapse: collapse; margin-top: 6mm; table-layout: fixed; break-inside: avoid; page-break-inside: avoid; }\n")
          .append("    th, td { border:1px solid #bbb; padding: 2.2mm; vertical-align: top; }\n")
          .append("    thead th { background:#eee; }\n")
          .append("    .left { text-align:left; } .right { text-align:right; } .center { text-align:center; }\n")
          .append("    .col-designation { width:46%; }\n")
          .append("    .col-tva         { width:8%; }\n")
          .append("    .col-unit        { width:18%; }\n")
          .append("    .col-qty         { width:8%; }\n")
          .append("    .col-total       { width:20%; }\n")
          .append("    tbody tr { break-inside: avoid; page-break-inside: avoid; }\n")
          .append("    .foot { margin-top: 6mm; break-inside: avoid; page-break-inside: avoid; }\n")

          // --- Totaux en 2 colonnes (label + montant) → espace garanti ---
          .append("    .totals { margin-top: 5mm; }\n")
          .append("    .tot-line { display: table; width: 100%; margin: 1mm 0; }\n")
          .append("    .tot-line .label { display: table-cell; padding-right: 4mm; }\n")
          .append("    .tot-line .amount { display: table-cell; text-align: right; white-space: nowrap; }\n")
          .append("    .grand .label, .grand .amount { font-weight: bold; font-size: 13px; }\n")
          .append("    .grand .amount { border-top: 1px solid #000; padding-top: 1.5mm; }\n")

          .append("    .small { font-size: 9.5px; color:#444; }\n")

          // Bloc clé/valeur (virement) au propre en 2 colonnes
          .append("    .kv { margin-top: 3mm; display: table; width: 100%; }\n")
          .append("    .kv-row { display: table-row; }\n")
          .append("    .kv-label { display: table-cell; padding-right: 3mm; color:#444; white-space: nowrap; }\n")
          .append("    .kv-value { display: table-cell; }\n")

          // Densification progressive si bcp de lignes
          .append("    .dense table th, .dense table td { padding: 1.8mm; }\n")
          .append("    .dense body, .dense td, .dense th { font-size: 10.5px; }\n")
          .append("    .ultra table th, .ultra table td { padding: 1.5mm; }\n")
          .append("    .ultra body, .ultra td, .ultra th { font-size: 10px; }\n")
          .append("  </style>\n")
          .append("</head>\n")
          .append("<body class=\"").append(densityClass).append("\">\n")
          .append("  <h1>Facture</h1>\n")

          .append("  <div class=\"meta\">\n")
          .append("    <div class=\"row\">\n")
          .append("      <span><strong>Réf. :</strong> ").append(escapeHtml(ref)).append("</span>\n")
          .append("      <span><strong>Date facturation :</strong> ").append(escapeHtml(billingDate)).append("</span>\n")
          .append("      <span><strong>Date échéance :</strong> ").append(escapeHtml(dueDate)).append("</span>\n")
          .append("      <span><strong>Code client :</strong> ").append(escapeHtml(customerCode)).append("</span>\n")
          .append("      <span><strong>Statut :</strong> ").append(escapeHtml(statut)).append("</span>\n")
          .append("    </div>\n")
          .append("  </div>\n")

          .append("  <div class=\"cols\">\n")
          .append("    <div>\n")
          .append("      <div class=\"muted\">Émetteur :</div>\n")
          .append(          emitterBlock).append("\n")
          .append("    </div>\n")
          .append("    <div>\n")
          .append("      <div class=\"muted\">Adressé à :</div>\n")
          .append(          customerBlock).append("\n")
          .append("    </div>\n")
          .append("  </div>\n")

          .append("  <div class=\"muted\" style=\"margin-top:5mm\">Montants exprimés en Euros</div>\n")

          .append("  <table>\n")
          .append("    <thead>\n")
          .append("      <tr>\n")
          .append("        <th class=\"left  col-designation\">Désignation</th>\n")
          .append("        <th class=\"center col-tva\">TVA</th>\n")
          .append("        <th class=\"right col-unit\">P.U. HT</th>\n")
          .append("        <th class=\"center col-qty\">Qté</th>\n")
          .append("        <th class=\"right col-total\">Total HT</th>\n")
          .append("      </tr>\n")
          .append("    </thead>\n")
          .append("    <tbody>\n")
          .append(         rows).append("\n")
          .append("    </tbody>\n")
          .append("  </table>\n")

          .append("  <div class=\"foot\">\n")
          .append("    <div>").append(escapeHtml(conditionsRegl)).append("</div>\n")

          // --- Totaux avec espacement robuste ---
          .append("    <div class=\"totals\">\n")
          .append("      <div class=\"tot-line\"><span class=\"label\"><strong>Total HT</strong></span><span class=\"amount\">")
          .append(escapeHtml(fmtMoney(totalHT))).append("</span></div>\n")
          .append(       tvaLines).append("\n")
          .append("      <div class=\"tot-line grand\"><span class=\"label\">Total TTC</span><span class=\"amount\">")
          .append(escapeHtml(fmtMoney(totalTTC))).append("</span></div>\n")
          .append("    </div>\n")

          .append(     virementBlock).append("\n")
          .append("  </div>\n")

          .append("</body>\n")
          .append("</html>\n");

        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Densité (compacte si beaucoup de lignes)
    // ---------------------------------------------------------------------

    private String densityClass(List<Purchase> list) {
        int n = (list == null) ? 0 : list.size();
        if (n >= 28) return "ultra";   // police ~10px, padding 1.5mm
        if (n >= 18) return "dense";   // police ~10.5px, padding 1.8mm
        return "";                     // normal
    }

    // ---------------------------------------------------------------------
    // Blocks
    // ---------------------------------------------------------------------

    private String buildEmitterBlock(User emitter) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"box\">");
        sb.append("<div><strong>").append(escapeHtml(nameOrUsername(emitter))).append("</strong></div>");
        sb.append(lineIfNotBlank(emitter != null ? emitter.getAddress()     : null));
        sb.append(joinIfAny(List.of(
                concatNonEmpty(safe(emitter != null ? emitter.getPostalCode() : null),
                               safe(emitter != null ? emitter.getCity()       : null)))));
        sb.append(lineIfNotBlank(emitter != null ? emitter.getPhoneNumber() : null, "Tél.: "));
        sb.append(lineIfNotBlank(emitter != null ? emitter.getEmail()       : null, "Email: "));
        sb.append(lineIfNotBlank(emitter != null ? emitter.getWebsite()     : null, "Web: "));
        sb.append("</div>");
        return sb.toString();
    }

    private String buildCustomerBlock(User customer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"box\">");
        if (customer == null) {
            sb.append("<div><strong>-</strong></div>");
        } else {
            sb.append("<div><strong>").append(escapeHtml(nameOrUsername(customer))).append("</strong></div>");
            sb.append(lineIfNotBlank(customer.getAddress()));
            sb.append(joinIfAny(List.of(
                    concatNonEmpty(safe(customer.getPostalCode()), safe(customer.getCity())))));
            sb.append(lineIfNotBlank(customer.getPhoneNumber(), "Tél.: "));
            sb.append(lineIfNotBlank(customer.getWebsite(),     "Web: "));
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildRows(List<Purchase> purchases) {
        if (purchases == null || purchases.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Purchase p : purchases) {
            sb.append("<tr>")
              .append("<td class=\"left\">").append(escapeHtml(safe(p.getName()))).append("</td>")
              .append("<td class=\"center\">").append(escapeHtml(fmtRate(p.getTvaApplied()))).append("%</td>")
              .append("<td class=\"right\">").append(escapeHtml(fmtMoney(p.getUnitPriceHT()))).append("</td>")
              .append("<td class=\"center\">").append(escapeHtml(fmtQty(p.getQuantity()))).append("</td>")
              .append("<td class=\"right\">").append(escapeHtml(fmtMoney(p.getTotalHT()))).append("</td>")
              .append("</tr>\n");
        }
        return sb.toString();
    }

    /** Lignes de TVA au format 2 colonnes (label/amount) pour éviter les textes collés */
    private String buildTvaLines(Map<BigDecimal, BigDecimal> totalTvaByRate) {
        if (totalTvaByRate == null || totalTvaByRate.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        totalTvaByRate.entrySet().stream()
                .sorted(Map.Entry.<BigDecimal, BigDecimal>comparingByKey().reversed())
                .forEach(e -> {
                    sb.append("<div class=\"tot-line\">")
                      .append("<span class=\"label\">Total TVA ")
                      .append(escapeHtml(fmtRate(e.getKey()))).append("%</span>")
                      .append("<span class=\"amount\">")
                      .append(escapeHtml(fmtMoney(e.getValue())))
                      .append("</span>")
                      .append("</div>\n");
                });
        return sb.toString();
    }

    /** Bloc virement en 2 colonnes (label/valeur) pour une lecture plus nette */
    private String buildVirementBlock(PaymentDetails pd) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"margin-top:5mm\">");
        sb.append("<div><strong>Règlement par virement :</strong></div>");
        sb.append("<div class=\"kv\">");
        if (pd != null) {
            if (notBlank(pd.getDomiciliation())) {
                sb.append("<div class=\"kv-row\"><div class=\"kv-label\">Domiciliation:</div><div class=\"kv-value\">")
                  .append(escapeHtml(pd.getDomiciliation())).append("</div></div>");
            }
            if (notBlank(pd.getHolderName())) {
                sb.append("<div class=\"kv-row\"><div class=\"kv-label\">Titulaire:</div><div class=\"kv-value\">")
                  .append(escapeHtml(pd.getHolderName())).append("</div></div>");
            }
            if (notBlank(pd.getIban())) {
                sb.append("<div class=\"kv-row\"><div class=\"kv-label\">IBAN:</div><div class=\"kv-value\">")
                  .append(escapeHtml(pd.getIban())).append("</div></div>");
            }
            if (notBlank(pd.getBic())) {
                sb.append("<div class=\"kv-row\"><div class=\"kv-label\">BIC/SWIFT:</div><div class=\"kv-value\">")
                  .append(escapeHtml(pd.getBic())).append("</div></div>");
            }
        }
        sb.append("</div>"); // .kv
        sb.append("</div>");
        return sb.toString();
    }

    private String paymentTermLabel(PaymentDetails pd) {
        if (pd == null || pd.getPaymentTerm() == null) return "-";
        return humanizeEnum(pd.getPaymentTerm().name());
    }

    // ---------------------------------------------------------------------
    // Utils
    // ---------------------------------------------------------------------

    private static BigDecimal nv(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private String fmtDate(Date d) {
        if (d == null) return "-";
        return new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(d);
    }

    private String fmtMoney(BigDecimal v) {
        v = nv(v).setScale(2, RoundingMode.HALF_UP);
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }

    private String fmtRate(BigDecimal rate) {
        if (rate == null) return "-";
        return rate.stripTrailingZeros().toPlainString();
    }

    private String fmtQty(BigDecimal q) {
        if (q == null) return "-";
        return q.stripTrailingZeros().toPlainString();
    }

    private String safe(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private String lineIfNotBlank(String s) {
        if (s == null || s.isBlank()) return "";
        return "<div>" + escapeHtml(s) + "</div>";
    }

    private String lineIfNotBlank(String s, String prefix) {
        if (s == null || s.isBlank()) return "";
        return "<div>" + escapeHtml(prefix + s) + "</div>";
    }

    private String joinIfAny(List<String> parts) {
        String joined = parts.stream().filter(p -> p != null && !p.isBlank()).collect(Collectors.joining(" "));
        return lineIfNotBlank(joined);
    }

    private String concatNonEmpty(String a, String b) {
        if ((a == null || a.isBlank()) && (b == null || b.isBlank())) return "";
        if (a == null || a.isBlank()) return b;
        if (b == null || b.isBlank()) return a;
        return a + " " + b;
    }

    private String nameOrUsername(User u) {
        if (u == null) return "-";
        return safe(u.getUsername());
    }

    private String humanizeEnum(String name) {
        if (name == null) return "-";
        String s = name.replace('_', ' ').toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Échappement minimal HTML */
    private String escapeHtml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
}
