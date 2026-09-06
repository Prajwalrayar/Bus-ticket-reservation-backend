package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.BusCreateRequest;
import com.crimsonlogic.busticketbooking.dto.BusDTO;
import com.crimsonlogic.busticketbooking.dto.BusSeatCreateRequest;
import com.crimsonlogic.busticketbooking.dto.BusSeatDTO;
import com.crimsonlogic.busticketbooking.service.BusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
public class BusController {

    private final BusService busService;


    // =========================================================
    // BUS ENDPOINTS
    // =========================================================

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusDTO>>>
    getAllBuses() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        busService.getAllBuses()
                )
        );
    }


    @GetMapping("/{registrationNumber}")
    public ResponseEntity<ApiResponse<BusDTO>>
    getBusByRegistrationNumber(
            @PathVariable String registrationNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        busService.getBusByRegistrationNumber(
                                registrationNumber
                        )
                )
        );
    }


    @GetMapping("/operator/{companyName}")
    public ResponseEntity<ApiResponse<List<BusDTO>>>
    getBusesByOperator(
            @PathVariable String companyName) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        busService.getBusesByOperator(
                                companyName
                        )
                )
        );
    }


    @PostMapping
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<BusDTO>>
    createBus(
            @Valid @RequestBody BusCreateRequest request) {

        BusDTO createdBus =
                busService.createBus(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Bus created successfully",
                                createdBus
                        )
                );
    }


    @PutMapping("/{registrationNumber}")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<BusDTO>>
    updateBus(
            @PathVariable String registrationNumber,
            @Valid @RequestBody BusCreateRequest request) {

        BusDTO updatedBus =
                busService.updateBus(
                        registrationNumber,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Bus updated successfully",
                        updatedBus
                )
        );
    }


    @PatchMapping("/{registrationNumber}/deactivate")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<Void>>
    deactivateBus(
            @PathVariable String registrationNumber) {

        busService.deactivateBus(
                registrationNumber
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Bus deactivated successfully",
                        null
                )
        );
    }


    // =========================================================
    // BUS SEAT ENDPOINTS
    // =========================================================

    @GetMapping("/{registrationNumber}/seats")
    public ResponseEntity<ApiResponse<List<BusSeatDTO>>>
    getSeatsByBus(
            @PathVariable String registrationNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        busService.getSeatsByBus(
                                registrationNumber
                        )
                )
        );
    }


    @GetMapping("/{registrationNumber}/seats/{seatNumber}")
    public ResponseEntity<ApiResponse<BusSeatDTO>>
    getBusSeat(
            @PathVariable String registrationNumber,
            @PathVariable String seatNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        busService.getBusSeat(
                                registrationNumber,
                                seatNumber
                        )
                )
        );
    }


    @PostMapping("/{registrationNumber}/seats")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<BusSeatDTO>>
    createBusSeat(
            @PathVariable String registrationNumber,
            @Valid @RequestBody BusSeatCreateRequest request) {

        BusSeatDTO createdSeat =
                busService.createBusSeat(
                        registrationNumber,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Bus seat created successfully",
                                createdSeat
                        )
                );
    }


    @PutMapping("/{registrationNumber}/seats/{seatNumber}")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<BusSeatDTO>>
    updateBusSeat(
            @PathVariable String registrationNumber,
            @PathVariable String seatNumber,
            @Valid @RequestBody BusSeatCreateRequest request) {

        BusSeatDTO updatedSeat =
                busService.updateBusSeat(
                        registrationNumber,
                        seatNumber,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Bus seat updated successfully",
                        updatedSeat
                )
        );
    }


    @PatchMapping(
            "/{registrationNumber}/seats/{seatNumber}/deactivate"
    )
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<Void>>
    deactivateBusSeat(
            @PathVariable String registrationNumber,
            @PathVariable String seatNumber) {

        busService.deactivateBusSeat(
                registrationNumber,
                seatNumber
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Bus seat deactivated successfully",
                        null
                )
        );
    }
}