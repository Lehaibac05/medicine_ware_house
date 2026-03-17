package com.pharmacy.warehouse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pharmacy.warehouse.model.SupplierInvoice;
import com.pharmacy.warehouse.model.SupplierInvoice.InvoiceStatus;

public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long> {

    List<SupplierInvoice> findByStatus(InvoiceStatus status);

    List<SupplierInvoice> findBySupplier_SupplierId(Long supplierId);

    Optional<SupplierInvoice> findByGoodsReceipt_ReceiptId(Long receiptId);

    @Query("SELECT si FROM SupplierInvoice si LEFT JOIN FETCH si.items WHERE si.invoiceId = :id")
    SupplierInvoice findByIdWithItems(@Param("id") Long id);
}
