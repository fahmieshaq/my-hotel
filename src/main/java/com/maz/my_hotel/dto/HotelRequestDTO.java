package com.maz.my_hotel.dto;

import jakarta.validation.constraints.NotBlank;

// NEW CMMENT
public record HotelRequestDTO(

        @NotBlank(message = "Hotel name is required")
        String name,

        @NotBlank(message = "Hotel location is required")
        String location

) {
}
