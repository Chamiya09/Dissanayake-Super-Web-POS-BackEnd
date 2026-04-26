package com.dissayakesuper.web_pos_backend.sale.controller;

import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dissayakesuper.web_pos_backend.sale.service.SaleService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class SalesExportController {

    private final SaleService saleService;

    public SalesExportController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping(value = "/export-sales", produces = "text/csv")
    public ResponseEntity<byte[]> exportSalesForMlRetraining() {
        byte[] csv = saleService.exportSalesForMlCsv();

        String filename = "ml-sales-dataset-" + LocalDate.now() + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(csv.length);

        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
