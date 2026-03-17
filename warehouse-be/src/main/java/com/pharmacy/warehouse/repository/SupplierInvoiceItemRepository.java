package com.pharmacy.warehouse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pharmacy.warehouse.model.SupplierInvoiceItem;

public interface SupplierInvoiceItemRepository extends JpaRepository<SupplierInvoiceItem, Long> {

    List<SupplierInvoiceItem> findBySupplierInvoice_InvoiceId(Long invoiceId);
}
