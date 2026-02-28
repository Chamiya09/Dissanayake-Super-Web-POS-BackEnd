package com.dissayakesuper.web_pos_backend.supplier.service;

import com.dissayakesuper.web_pos_backend.supplier.entity.Supplier;
import com.dissayakesuper.web_pos_backend.supplier.dto.SupplierRequest;
import com.dissayakesuper.web_pos_backend.supplier.repository.SupplierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return repository.findAll();
    }

    // ── READ ONE ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Supplier getSupplierById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Supplier not found with id: " + id));
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public Supplier createSupplier(SupplierRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A supplier with email '" + request.email() + "' already exists.");
        }

        Supplier supplier = new Supplier(
                request.companyName(),
                request.contactPerson(),
                request.email(),
                request.phone(),
                request.leadTime(),
                request.isAutoReorderEnabled()
        );

        return repository.save(supplier);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public Supplier updateSupplier(Long id, SupplierRequest request) {
        Supplier existing = getSupplierById(id);

        // Guard: reject if the new email is already taken by a *different* supplier
        repository.findByEmail(request.email())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Email '" + request.email() + "' is already used by another supplier.");
                });

        existing.setCompanyName(request.companyName());
        existing.setContactPerson(request.contactPerson());
        existing.setEmail(request.email());
        existing.setPhone(request.phone());
        existing.setLeadTime(request.leadTime());
        existing.setAutoReorderEnabled(request.isAutoReorderEnabled());

        return repository.save(existing);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void deleteSupplier(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Supplier not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
