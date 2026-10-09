package com.maz.my_hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;

public record RoomRequestDTO(

        @NotBlank(message = "Room number is required")
        String roomNumber,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        BigDecimal price,

        @NotNull(message = "Hotel id is required")
        Long hotelId
) {
}
