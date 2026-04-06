package com.dissayakesuper.web_pos_backend.sale.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dissayakesuper.web_pos_backend.sale.dto.SaleReturnRequest;
import com.dissayakesuper.web_pos_backend.sale.dto.SaleUpdateRequest;
import com.dissayakesuper.web_pos_backend.sale.dto.StatusRequest;
import com.dissayakesuper.web_pos_backend.sale.dto.TransactionSeedRequest;
import com.dissayakesuper.web_pos_backend.sale.dto.TransactionSeedResponse;
import com.dissayakesuper.web_pos_backend.sale.entity.Sale;
import com.dissayakesuper.web_pos_backend.sale.service.InvoicePdfService;
import com.dissayakesuper.web_pos_backend.sale.service.SaleService;
import com.dissayakesuper.web_pos_backend.sale.service.TransactionIdService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class SaleController {

    private final SaleService       saleService;
    private final InvoicePdfService  invoicePdfService;
    private final TransactionIdService transactionIdService;

    public SaleController(SaleService saleService,
                          InvoicePdfService invoicePdfService,
                          TransactionIdService transactionIdService) {
        this.saleService      = saleService;
        this.invoicePdfService = invoicePdfService;
        this.transactionIdService = transactionIdService;
    }
    // ── GET /api/sales/{id}/invoice ─────────────────────────────────────────
    /**
     * Generates and returns a PDF invoice for the given sale.
     * The response carries {@code Content-Disposition: attachment} so the
     * browser triggers a file download named
     * {@code invoice-<receiptNo>.pdf}.
     *
     * @return 200 OK with the PDF binary, or 404 if the sale is not found
     */
    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        Sale sale = saleService.getSaleById(id);
        byte[] pdf = invoicePdfService.generateInvoice(sale);

        String filename = "invoice-" + sale.getReceiptNo() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
    // ── GET /api/sales ────────────────────────────────────────────────────────
    /** Returns the full sales history. */
    @GetMapping
    public ResponseEntity<List<Sale>> getAll() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    // ── GET /api/sales/{id} ───────────────────────────────────────────────────
    /** Returns a single sale (with its line items). 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<Sale> getById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    // ── POST /api/sales ───────────────────────────────────────────────────────
    /** Records a new sale from the POS checkout. Returns 201 Created. */
    @PostMapping
    public ResponseEntity<Sale> create(@Valid @RequestBody Sale sale) {
        Sale created = saleService.createSale(sale);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/transaction-seed")
    public ResponseEntity<TransactionSeedResponse> setTransactionSeed(
            @Valid @RequestBody TransactionSeedRequest request) {
        String appliedSeed = transactionIdService.setMigrationSeed(request.lastTransactionId());
        String nextPreview = transactionIdService.peekNextTransactionId();
        TransactionSeedResponse response = new TransactionSeedResponse(
                appliedSeed,
                nextPreview,
                "Seed applied. TransactionID sequence is ready for migration continuity."
        );
        return ResponseEntity.ok(response);
    }
    // ── PUT /api/sales/{id} ────────────────────────────────────────────────────
    /**
     * Updates an existing sale.
     * Reverses old inventory deductions, updates sale fields,
     * then applies the new line items ― all in a single transaction.
     * Returns 409 if the sale is Voided; 400 if stock is insufficient.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Sale> update(
            @PathVariable Long id,
            @Valid @RequestBody SaleUpdateRequest request) {
        Sale updated = saleService.updateSale(id, request);
        return ResponseEntity.ok(updated);
    }
    // ── PUT /api/sales/{id}/status ────────────────────────────────────────────
    /** Updates the status of a sale (e.g., "Completed" → "Voided"). */
    @PutMapping("/{id}/status")
    public ResponseEntity<Sale> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        Sale updated = saleService.updateSaleStatus(id, request.status());
        return ResponseEntity.ok(updated);
    }

    // ── POST /api/sales/{id}/return ───────────────────────────────────────────
    /**
     * Processes a sales return for a Completed sale.
     * Restocks all sold items back into inventory and marks the sale as "Returned".
     *
     * @return 200 OK with the updated Sale, 404 if not found, 409 if already Voided/Returned.
     */
    @PostMapping("/{id}/return")
    public ResponseEntity<Sale> returnSale(@PathVariable Long id) {
        Sale returned = saleService.returnSale(id);
        return ResponseEntity.ok(returned);
    }

    // ── POST /api/sales/{id}/return-items ─────────────────────────────────────
    /**
     * Processes an item-level return for a sale.
     * Allows selecting specific sold items and return quantities.
     */
    @PostMapping("/{id}/return-items")
    public ResponseEntity<Sale> returnSaleItems(
            @PathVariable Long id,
            @Valid @RequestBody SaleReturnRequest request) {
        Sale returned = saleService.returnSelectedItems(id, request);
        return ResponseEntity.ok(returned);
    }
}
