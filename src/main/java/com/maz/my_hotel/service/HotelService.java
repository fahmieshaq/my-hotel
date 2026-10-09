package com.maz.my_hotel.service;

import com.maz.my_hotel.dto.HotelRequestDTO;
import com.maz.my_hotel.dto.HotelResponseDTO;
import com.maz.my_hotel.dto.RoomRequestDTO;
import com.maz.my_hotel.dto.RoomResponseDTO;
import com.maz.my_hotel.entity.Hotel;
import com.maz.my_hotel.exception.ResourceNotFoundException;
import com.maz.my_hotel.repository.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HotelService {
    private static final Logger log = LoggerFactory.getLogger(HotelService.class);
    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Transactional
    public HotelResponseDTO createHotel(HotelRequestDTO request) {
        log.info("Creating hotel with name={} location {}", request.name(), request.location());

        Hotel hotel = new Hotel(request.name(), request.location());
        Hotel saved = hotelRepository.save(hotel);

        log.info("Hotel created with id={}", saved.getId());
        return toResponse(saved);
    }

    public List<HotelResponseDTO> getAllHotels() {
        log.info("Getting all hotels");
        return hotelRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public HotelResponseDTO getHotelById(Long id) {
        log.info("Getting hotel with id={}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        return toResponse(hotel);
    }

    @Transactional
    public HotelResponseDTO updateHotelById(Long id, HotelRequestDTO request) {
        log.info("Updating hotel with id={}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));

        hotel.setName(request.name());
        hotel.setLocation(request.location());
        Hotel updated = hotelRepository.save(hotel);

        log.info("Hotel updated with id={}", updated.getId());
        return toResponse(updated);
    }

    @Transactional
    public void deleteHotel(Long id) {
        log.info("Deleting hotel with id={}", id);
        if (!hotelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hotel not found with id: " + id);
        }
        hotelRepository.deleteById(id);
        log.info("Hotel deleted with id={}", id);
    }

    private HotelResponseDTO toResponse(Hotel hotel) {
        return new HotelResponseDTO(hotel.getId(), hotel.getName(), hotel.getLocation());
    }
}
