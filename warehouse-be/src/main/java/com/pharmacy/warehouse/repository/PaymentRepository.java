package com.pharmacy.warehouse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pharmacy.warehouse.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySupplierInvoice_InvoiceId(Long invoiceId);
}
