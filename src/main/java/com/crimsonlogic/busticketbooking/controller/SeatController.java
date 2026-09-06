package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.TripSeatDTO;
import com.crimsonlogic.busticketbooking.service.SeatService;
import com.crimsonlogic.busticketbooking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/seats")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TripSeatDTO>>> getTripSeats(@PathVariable String tripId) {
        return ResponseEntity.ok(ApiResponse.success(seatService.getTripSeats(tripId)));
    }

    @PostMapping("/lock")
    public ResponseEntity<ApiResponse<List<TripSeatDTO>>> lockSeats(@PathVariable String tripId, @RequestBody List<String> seatIds) {
        String userId = userService.getCurrentAuthenticatedUser().getUserId();
        return ResponseEntity.ok(ApiResponse.success(seatService.lockSeats(tripId, seatIds, userId)));
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Void>> releaseSeats(@PathVariable String tripId, @RequestBody List<String> seatIds) {
        String userId = userService.getCurrentAuthenticatedUser().getUserId();
        seatService.releaseSeats(tripId, seatIds, userId);
        return ResponseEntity.ok(ApiResponse.success("Seats released", null));
    }
}
