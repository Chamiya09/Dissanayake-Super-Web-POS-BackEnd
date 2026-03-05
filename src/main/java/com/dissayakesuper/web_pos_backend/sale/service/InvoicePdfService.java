package com.dissayakesuper.web_pos_backend.sale.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.dissayakesuper.web_pos_backend.sale.entity.Sale;
import com.dissayakesuper.web_pos_backend.sale.entity.SaleItem;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a professional PDF invoice for a completed sale.
 *
 * Layout (A4 portrait):
 *   ┌─ Header ──────── store branding + receipt meta ───────────────┐
 *   ├─ Items table ─── # │ Product │ Qty │ Unit Price │ Total ───────┤
 *   ├─ Totals section ── Subtotal / Grand Total ────────────────────┤
 *   └─ Footer ─────── payment method + thank-you note ─────────────┘
 */
@Service
public class InvoicePdfService {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color COL_BRAND    = new Color(15,  23,  42);   // slate-900
    private static final Color COL_ACCENT   = new Color(99,  102, 241);  // indigo-500
    private static final Color COL_TH_BG    = new Color(30,  41,  59);   // slate-800
    private static final Color COL_TH_TEXT  = Color.WHITE;
    private static final Color COL_ROW_ALT  = new Color(248, 250, 252);  // slate-50
    private static final Color COL_BORDER   = new Color(203, 213, 225);  // slate-300
    private static final Color COL_TOTAL_BG = new Color(241, 245, 249);  // slate-100
    private static final Color COL_TEXT     = new Color(30,  41,  59);   // slate-800
    private static final Color COL_MUTED    = new Color(100, 116, 139);  // slate-500

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final String FONT = FontFactory.HELVETICA;

    private Font font(float size, int style, Color color) {
        return FontFactory.getFont(FONT, size, style, color);
    }

    // ── Currency formatter ────────────────────────────────────────────────────
    private static String lkr(double value) {
        return "LKR " + BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private static String qty(BigDecimal q) {
        return (q == null ? BigDecimal.ZERO : q)
                .setScale(3, RoundingMode.HALF_UP)
                .toPlainString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a PDF invoice for the given {@link Sale} and returns the raw bytes.
     *
     * @param sale the persisted Sale entity (items must be loaded)
     * @return byte array of the generated PDF
     */
    public byte[] generateInvoice(Sale sale) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // A4 with 36pt margins (≈ 1.27 cm)
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, sale);
            doc.add(Chunk.NEWLINE);
            addItemsTable(doc, sale.getItems());
            doc.add(Chunk.NEWLINE);
            addTotalsSection(doc, sale);
            doc.add(Chunk.NEWLINE);
            addFooter(doc, sale);

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF invoice: " + e.getMessage(), e);
        } finally {
            doc.close();
        }

        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HEADER
    // ─────────────────────────────────────────────────────────────────────────

    private void addHeader(Document doc, Sale sale) throws DocumentException {

        // ── Top banner ────────────────────────────────────────────────────────
        PdfPTable banner = new PdfPTable(2);
        banner.setWidthPercentage(100);
        banner.setWidths(new float[]{60, 40});

        // Left: store identity
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.setBackgroundColor(COL_BRAND);
        brandCell.setPadding(14);

        Paragraph brandName = new Paragraph("DISSANAYAKE SUPER",
                font(18, Font.BOLD, Color.WHITE));
        brandName.setSpacingAfter(3);
        brandCell.addElement(brandName);
        brandCell.addElement(new Phrase("Your Trusted Neighbourhood Market",
                font(8, Font.NORMAL, COL_MUTED)));
        banner.addCell(brandCell);

        // Right: invoice label
        PdfPCell labelCell = new PdfPCell();
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(COL_ACCENT);
        labelCell.setPadding(14);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph invoiceLabel = new Paragraph("INVOICE",
                font(20, Font.BOLD, Color.WHITE));
        invoiceLabel.setAlignment(Element.ALIGN_RIGHT);
        labelCell.addElement(invoiceLabel);
        banner.addCell(labelCell);

        doc.add(banner);

        // ── Meta strip ────────────────────────────────────────────────────────
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm:ss a");
        String date = sale.getSaleDate() != null ? sale.getSaleDate().format(dateFmt) : "—";
        String time = sale.getSaleDate() != null ? sale.getSaleDate().format(timeFmt) : "—";

        PdfPTable meta = new PdfPTable(4);
        meta.setWidthPercentage(100);
        meta.setWidths(new float[]{28, 24, 24, 24});
        meta.setSpacingBefore(0);

        addMetaCell(meta, "RECEIPT NO.",    sale.getReceiptNo());
        addMetaCell(meta, "DATE",           date);
        addMetaCell(meta, "TIME",           time);
        addMetaCell(meta, "STATUS",         sale.getStatus() != null ? sale.getStatus().toUpperCase() : "—");

        doc.add(meta);
    }

    private void addMetaCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(COL_ACCENT);
        cell.setBorderWidth(2f);
        cell.setBackgroundColor(COL_TOTAL_BG);
        cell.setPadding(8);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", font(7, Font.BOLD, COL_MUTED)));
        p.add(new Chunk(value,        font(9, Font.BOLD, COL_TEXT)));
        cell.addElement(p);
        table.addCell(cell);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ITEMS TABLE
    // ─────────────────────────────────────────────────────────────────────────

    private void addItemsTable(Document doc, List<SaleItem> items) throws DocumentException {

        // 5 columns: #, Product Name, Qty, Unit Price, Total
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{6, 42, 12, 20, 20});
        table.setSpacingBefore(8);

        // ── Column headers ────────────────────────────────────────────────────
        String[] headers = {"#", "PRODUCT NAME", "QTY", "UNIT PRICE", "TOTAL"};
        int[]    aligns  = {
                Element.ALIGN_CENTER,
                Element.ALIGN_LEFT,
                Element.ALIGN_CENTER,
                Element.ALIGN_RIGHT,
                Element.ALIGN_RIGHT
        };

        for (int i = 0; i < headers.length; i++) {
            PdfPCell hCell = new PdfPCell(new Phrase(headers[i],
                    font(8, Font.BOLD, COL_TH_TEXT)));
            hCell.setBackgroundColor(COL_TH_BG);
            hCell.setHorizontalAlignment(aligns[i]);
            hCell.setPadding(8);
            hCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(hCell);
        }

        // ── Data rows ─────────────────────────────────────────────────────────
        if (items == null || items.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No items recorded.",
                    font(9, Font.ITALIC, COL_MUTED)));
            empty.setColspan(5);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(10);
            empty.setBorderColor(COL_BORDER);
            table.addCell(empty);
        } else {
            for (int i = 0; i < items.size(); i++) {
                SaleItem item = items.get(i);
                boolean alt   = (i % 2 == 1);
                Color   rowBg = alt ? COL_ROW_ALT : Color.WHITE;

                addDataCell(table, String.valueOf(i + 1),
                        Element.ALIGN_CENTER, rowBg, true);
                addDataCell(table, item.getProductName() != null ? item.getProductName() : "—",
                        Element.ALIGN_LEFT, rowBg, false);
                addDataCell(table, qty(item.getQuantity()),
                        Element.ALIGN_CENTER, rowBg, false);
                addDataCell(table, lkr(item.getUnitPrice() != null ? item.getUnitPrice() : 0.0),
                        Element.ALIGN_RIGHT, rowBg, false);
                addDataCell(table, lkr(item.getLineTotal() != null ? item.getLineTotal() : 0.0),
                        Element.ALIGN_RIGHT, rowBg, false);
            }
        }

        doc.add(table);
    }

    private void addDataCell(PdfPTable table, String text,
                              int align, Color bg, boolean firstCol) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                font(firstCol ? 8 : 9, Font.NORMAL, COL_TEXT)));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(bg);
        cell.setPaddingTop(7);
        cell.setPaddingBottom(7);
        cell.setPaddingLeft(firstCol ? 4 : 6);
        cell.setPaddingRight(6);
        cell.setBorderColor(COL_BORDER);
        cell.setBorder(Rectangle.BOTTOM);
        table.addCell(cell);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOTALS SECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void addTotalsSection(Document doc, Sale sale) throws DocumentException {

        // Right-aligned 2-column mini table pinned to full width
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100);
        totals.setWidths(new float[]{65, 35});

        // Empty left spacer
        PdfPCell spacer = new PdfPCell();
        spacer.setBorder(Rectangle.NO_BORDER);
        spacer.setRowspan(2);
        totals.addCell(spacer);

        // Subtotal row
        double total = sale.getTotalAmount() != null ? sale.getTotalAmount() : 0.0;
        addTotalRow(totals, "SUBTOTAL",   lkr(total), false);
        addTotalRow(totals, "GRAND TOTAL", lkr(total), true);

        doc.add(totals);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean highlight) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(highlight ? Rectangle.BOX : Rectangle.TOP);
        cell.setBorderColor(highlight ? COL_ACCENT : COL_BORDER);
        if (highlight) cell.setBorderWidth(1.5f);
        cell.setBackgroundColor(highlight ? COL_BRAND : COL_TOTAL_BG);
        cell.setPadding(10);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "   ",
                font(8, Font.BOLD, highlight ? COL_MUTED : COL_MUTED)));
        p.add(new Chunk(value,
                font(highlight ? 12 : 10, Font.BOLD,
                        highlight ? Color.WHITE : COL_TEXT)));
        p.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(p);
        table.addCell(cell);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FOOTER
    // ─────────────────────────────────────────────────────────────────────────

    private void addFooter(Document doc, Sale sale) throws DocumentException {

        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);
        footer.setWidths(new float[]{50, 50});
        footer.setSpacingBefore(8);

        // Left: payment method pill
        PdfPCell payCell = new PdfPCell();
        payCell.setBackgroundColor(COL_TOTAL_BG);
        payCell.setBorderColor(COL_BORDER);
        payCell.setBorder(Rectangle.BOX);
        payCell.setPadding(10);

        Paragraph payPara = new Paragraph();
        payPara.add(new Chunk("PAYMENT METHOD   ", font(7, Font.BOLD, COL_MUTED)));
        String method = sale.getPaymentMethod() != null
                ? sale.getPaymentMethod().toUpperCase() : "—";
        payPara.add(new Chunk(method, font(11, Font.BOLD, COL_ACCENT)));
        payCell.addElement(payPara);
        footer.addCell(payCell);

        // Right: thank-you
        PdfPCell thankCell = new PdfPCell();
        thankCell.setBackgroundColor(COL_BRAND);
        thankCell.setBorder(Rectangle.NO_BORDER);
        thankCell.setPadding(10);
        thankCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph thankPara = new Paragraph("Thank you for shopping with us!",
                font(9, Font.BOLDITALIC, Color.WHITE));
        thankPara.setAlignment(Element.ALIGN_RIGHT);
        thankCell.addElement(thankPara);

        Paragraph subThank = new Paragraph("Dissanayake Super — Always Fresh, Always Fair.",
                font(7, Font.NORMAL, COL_MUTED));
        subThank.setAlignment(Element.ALIGN_RIGHT);
        thankCell.addElement(subThank);
        footer.addCell(thankCell);

        doc.add(footer);
    }
}
