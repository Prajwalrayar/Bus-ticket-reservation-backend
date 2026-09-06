package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.TripSeatDTO;
import com.crimsonlogic.busticketbooking.entity.TripSeat;
import com.crimsonlogic.busticketbooking.enums.SeatStatus;
import com.crimsonlogic.busticketbooking.exception.BusinessException;
import com.crimsonlogic.busticketbooking.repository.TripSeatRepository;
import com.crimsonlogic.busticketbooking.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final TripSeatRepository tripSeatRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TripSeatDTO> getTripSeats(String tripId) {
        return tripSeatRepository.findByTrip_TripIdOrderByBusSeat_SeatNumberAsc(tripId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripSeatDTO> lockSeats(String tripId, List<String> tripSeatIds, String userId) {
        List<TripSeat> seats = tripSeatRepository.findAllById(tripSeatIds);

        for (TripSeat seat : seats) {
            if (!seat.getTrip().getTripId().equals(tripId)) {
                throw new BusinessException("Seat does not belong to the trip", "INVALID_TRIP");
            }
            boolean alreadyLockedBySelf = seat.getSeatStatus() == SeatStatus.TEMPORARILY_LOCKED
                    && userId.equals(seat.getLockedByUserId());
            if (seat.getSeatStatus() != SeatStatus.AVAILABLE && !alreadyLockedBySelf) {
                throw new BusinessException("Seat " + seat.getBusSeat().getSeatNumber() + " is not available", "SEAT_NOT_AVAILABLE");
            }

            seat.setSeatStatus(SeatStatus.TEMPORARILY_LOCKED);
            seat.setLockedByUserId(userId);
            seat.setLockExpiryTime(LocalDateTime.now().plusMinutes(10));
        }

        seats = tripSeatRepository.saveAll(seats);
        return seats.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void releaseSeats(String tripId, List<String> tripSeatIds, String userId) {
        List<TripSeat> seats = tripSeatRepository.findAllById(tripSeatIds);
        for (TripSeat seat : seats) {
            if (seat.getSeatStatus() == SeatStatus.TEMPORARILY_LOCKED
                    && userId.equals(seat.getLockedByUserId())) {
                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seat.setLockedByUserId(null);
                seat.setLockExpiryTime(null);
            }
        }
        tripSeatRepository.saveAll(seats);
    }

    @Override
    @Transactional
    public void releaseExpiredLocks() {
        List<TripSeat> expired = tripSeatRepository
                .findBySeatStatusAndLockExpiryTimeBefore(SeatStatus.TEMPORARILY_LOCKED, LocalDateTime.now());
        for (TripSeat seat : expired) {
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seat.setLockedByUserId(null);
            seat.setLockExpiryTime(null);
        }
        if (!expired.isEmpty()) {
            tripSeatRepository.saveAll(expired);
            log.info("Released {} expired seat locks", expired.size());
        }
    }

    private TripSeatDTO toDTO(TripSeat ts) {
        TripSeatDTO dto = new TripSeatDTO();
        dto.setTripSeatId(ts.getTripSeatId());
        dto.setSeatStatus(ts.getSeatStatus());
        dto.setSeatFare(ts.getSeatFare());
        dto.setLockExpiryTime(ts.getLockExpiryTime());
        if (ts.getBusSeat() != null) {
            dto.setSeatNumber(ts.getBusSeat().getSeatNumber());
            dto.setSeatPosition(ts.getBusSeat().getSeatPosition());
        }
        return dto;
    }
}
