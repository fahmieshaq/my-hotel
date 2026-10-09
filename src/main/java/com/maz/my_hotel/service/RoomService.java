package com.maz.my_hotel.service;


import com.maz.my_hotel.dto.HotelResponseDTO;
import com.maz.my_hotel.dto.RoomRequestDTO;
import com.maz.my_hotel.dto.RoomResponseDTO;
import com.maz.my_hotel.entity.Hotel;
import com.maz.my_hotel.entity.Room;
import com.maz.my_hotel.exception.ResourceNotFoundException;
import com.maz.my_hotel.repository.HotelRepository;
import com.maz.my_hotel.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomService {
    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    public RoomService(RoomRepository roomRepository, HotelRepository hotelRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
    }

    @Transactional
    public RoomResponseDTO createRoom(RoomRequestDTO request) {
        log.info("Create room with room number={}, price={}, and hotelId={}", request.roomNumber(), request.price(), request.hotelId());
        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + request.hotelId()));
        Room room = new Room(request.roomNumber(), request.price(), hotel);
        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    public List<RoomResponseDTO> getAllRooms() {
        log.info("Get all rooms");
        return roomRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RoomResponseDTO getRoomById(Long id) {
        log.info("Get room with id={}", id);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
        return toResponse(room);
    }

    private RoomResponseDTO toResponse(Room room) {
        return new RoomResponseDTO(room.getId(),
                room.getRoomNumber(),
                room.getPrice(),
                room.getHotel().getId(),
                room.getHotel().getName());
    }
}
