package com.maz.my_hotel.service;

import com.maz.my_hotel.dto.BookingRequestDTO;
import com.maz.my_hotel.dto.BookingResponseDTO;
import com.maz.my_hotel.entity.Booking;
import com.maz.my_hotel.entity.Room;
import com.maz.my_hotel.exception.BusinessRuleException;
import com.maz.my_hotel.exception.ResourceNotFoundException;
import com.maz.my_hotel.repository.BookingRepository;
import com.maz.my_hotel.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        log.info("Create booking");

        // Implement booking creation logic here
        if(!request.checkOutDate().isAfter(request.checkInDate())) {
            log.warn("Checkout date must be after check-in date");
            // Custom exception, not IllegalArgumentException: our handler maps
            // by TYPE, and only our own code can throw BusinessRuleException,
            // so a 400 here can never be confused with a library bug (500).
            throw new BusinessRuleException("Check-out date must be after check-in date");
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id " + request.roomId()));

        Booking booking = new Booking(
                request.guestName(),
                request.checkInDate(),
                request.checkOutDate(),
                room);

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);

    }

    public List<BookingResponseDTO> getAllBookings() {
        log.info("Fetching all bookings");
        return bookingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BookingResponseDTO toResponse(Booking booking) {
        return new BookingResponseDTO(
                booking.getId(),
                booking.getGuestName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoom().getId(),
                booking.getRoom().getRoomNumber());
    }
}
