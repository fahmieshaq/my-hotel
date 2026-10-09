package com.maz.my_hotel.dto;

import java.math.BigDecimal;

public record RoomResponseDTO(
        Long id,
        String roomNumber,
        BigDecimal price,
        Long hotelId,
        String hotelName
) {
}
