package com.maz.my_hotel.controller;

import com.maz.my_hotel.dto.HotelRequestDTO;
import com.maz.my_hotel.dto.HotelResponseDTO;
import com.maz.my_hotel.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hotels")
public class HotelController {
    private final HotelService hotelService;

    /**
     * Same constructor-injection pattern as in the service:
     * Spring builds HotelService first, then passes it in here.
     */
    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    public ResponseEntity<HotelResponseDTO> createHotel(@Valid @RequestBody HotelRequestDTO request) {
        HotelResponseDTO response = hotelService.createHotel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<HotelResponseDTO> getAllHotels() {
        return hotelService.getAllHotels();
    }

    @GetMapping("/{id}")
    public HotelResponseDTO getHotelById(@PathVariable Long id) {
        return hotelService.getHotelById(id);
    }

    @PutMapping("/{id}")
    public HotelResponseDTO updateHotel(@PathVariable Long id, @Valid @RequestBody HotelRequestDTO request) {
        return hotelService.updateHotelById(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}