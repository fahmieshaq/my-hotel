package com.maz.my_hotel.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookingRequestDTO(

        @NotBlank(message = "Guest name is required")
        String guestName,

        @NotNull(message = "Check-in date is required")
        @FutureOrPresent(message = "Check-in date must be today or in a future")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
        @Future(message = "Check-out date must be in the future")
        LocalDate checkOutDate,

        @NotNull(message = "Room id is required")
        Long roomId
) {
}
