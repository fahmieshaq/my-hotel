package com.maz.my_hotel.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookingResponseDTO(
        Long id,
        String guestName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long roomId,
        String roomNumber
) {
}
