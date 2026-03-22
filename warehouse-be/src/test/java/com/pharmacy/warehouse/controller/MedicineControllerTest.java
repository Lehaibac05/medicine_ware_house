package com.pharmacy.warehouse.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.service.MedicineService;

@ExtendWith(MockitoExtension.class)
public class MedicineControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicineService medicineService;

    @InjectMocks
    private MedicineController medicineController;

    private ObjectMapper objectMapper;
    private Medicine medicine;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(medicineController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .build();

        medicine = new Medicine();
        medicine.setMedicineId(1L);
        medicine.setName("Ibuprofen");
        medicine.setManufacturer("PharmaCorp");
        medicine.setStorageCondition("Room temperature");
        medicine.setDescription("Pain reliever and fever reducer");
    }

    @Test
    @DisplayName("Kiểm thử Hộp Đen: Gọi API getMedicines")
    public void testGetMedicines() throws Exception {
        Page<Medicine> page = new PageImpl<>(List.of(medicine), PageRequest.of(0, 10), 1);
        when(medicineService.getMedicines(anyInt(), anyInt(), any(), any(), any(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/medicines")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ibuprofen"));
    }

    @Test
    @DisplayName("Kiểm thử Create API")
    public void testCreateMedicine() throws Exception {
        when(medicineService.create(any(Medicine.class))).thenReturn(medicine);

        mockMvc.perform(post("/medicines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medicine)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ibuprofen"));
    }

    @Test
    @DisplayName("Kiểm thử Update API")
    public void testUpdateMedicine() throws Exception {
        when(medicineService.update(eq(1L), any(Medicine.class))).thenReturn(medicine);

        mockMvc.perform(put("/medicines/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medicine)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ibuprofen"));
    }

    @Test
    @DisplayName("Kiểm thử Delete API")
    public void testDeleteMedicine() throws Exception {
        mockMvc.perform(delete("/medicines/{id}", 1L))
                .andExpect(status().isOk());
    }
}
