package com.pharmacy.warehouse.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pharmacy.warehouse.dto.SupplierRequest;
import com.pharmacy.warehouse.dto.SupplierResponse;
import com.pharmacy.warehouse.model.Supplier;
import com.pharmacy.warehouse.model.Supplier.SupplierStatus;
import com.pharmacy.warehouse.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository.findAllActiveSuppliers().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        return convertToResponse(supplier);
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        log.info("Creating new supplier: {}", request.getSupplierName());
        
        // Check if tax code already exists
        if (request.getTaxCode() != null) {
            supplierRepository.findByTaxCode(request.getTaxCode())
                    .ifPresent(s -> {
                        throw new RuntimeException("Supplier with tax code " + request.getTaxCode() + " already exists");
                    });
        }
        
        Supplier supplier = new Supplier();
        supplier.setSupplierName(request.getSupplierName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhoneNumber(request.getPhoneNumber());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setTaxCode(request.getTaxCode());
        supplier.setStatus(request.getStatus() != null ? 
                SupplierStatus.valueOf(request.getStatus()) : SupplierStatus.ACTIVE);
        
        Supplier savedSupplier = supplierRepository.save(supplier);
        log.info("Supplier created successfully with id: {}", savedSupplier.getSupplierId());
        
        return convertToResponse(savedSupplier);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        log.info("Updating supplier with id: {}", id);
        
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        
        supplier.setSupplierName(request.getSupplierName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhoneNumber(request.getPhoneNumber());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setTaxCode(request.getTaxCode());
        if (request.getStatus() != null) {
            supplier.setStatus(SupplierStatus.valueOf(request.getStatus()));
        }
        
        Supplier updatedSupplier = supplierRepository.save(supplier);
        log.info("Supplier updated successfully");
        
        return convertToResponse(updatedSupplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        log.info("Deleting supplier with id: {}", id);
        
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        
        // Soft delete by setting status to INACTIVE
        supplier.setStatus(SupplierStatus.INACTIVE);
        supplierRepository.save(supplier);
        
        log.info("Supplier deleted successfully");
    }

    private SupplierResponse convertToResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .supplierId(supplier.getSupplierId())
                .supplierName(supplier.getSupplierName())
                .contactPerson(supplier.getContactPerson())
                .phoneNumber(supplier.getPhoneNumber())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .taxCode(supplier.getTaxCode())
                .status(supplier.getStatus().name())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
