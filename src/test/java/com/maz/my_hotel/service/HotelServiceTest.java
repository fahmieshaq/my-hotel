package com.maz.my_hotel.service;

import com.maz.my_hotel.dto.HotelRequestDTO;
import com.maz.my_hotel.dto.HotelResponseDTO;
import com.maz.my_hotel.entity.Hotel;
import com.maz.my_hotel.exception.ResourceNotFoundException;
import com.maz.my_hotel.repository.HotelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

// tells JUnit 5 to activate Mockito support for this test class
// You need it because you're using Mockito annotations such @Mock, @InjectMocks, etc.
@ExtendWith(MockitoExtension.class)
public class HotelServiceTest {
    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelService hotelService;

    @Test
    void createHotel_returnSavedHotelResponse_success() {
        HotelRequestDTO request = new HotelRequestDTO("Grand Plaza", "Morocco");
        // Stub the mocked repo
        when(hotelRepository.save(any(Hotel.class)))
                .thenAnswer(invocation -> {
                    Hotel hotel = invocation.getArgument(0); // Get the actual Hotel passed by HotelService to save().
                    hotel.setId(1L); // Simulate the database generating a primary key.
                    return hotel; // Return the same Hotel back to HotelService.
                });

        HotelResponseDTO response = hotelService.createHotel(request);

        // Does NOT call save() again.
        // Verifies that save() was called exactly once with any Hotel object
        // during hotelService.createHotel(request).
        verify(hotelRepository).save(any(Hotel.class));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Grand Plaza");
        assertThat(response.location()).isEqualTo("Morocco");
    }

    @Test
    void getAllHotels_returnListOfHotelResponse_success() {
        // Create fake hotel entities
        // These simulate Hotel records that would normally come from the database.
        // We set IDs because we're pretending these are Hotels that were already retrieved from the database.
        Hotel hotel1 = new Hotel();
        hotel1.setId(1L);
        hotel1.setName("Grand Plaza");
        hotel1.setLocation("Morocco");

        Hotel hotel2 = new Hotel();
        hotel2.setId(2L);
        hotel2.setName("Fairmont");
        hotel2.setLocation("Italy");

        // hotelRepository is a MOCK, so there is no real database involved.
        //
        // Tell the MOCK repository:
        // "When the REAL HotelService calls findAll(),
        // pretend the database returned these two Hotel entities."
        //
        // The fake/simulated part of this test is the repository response.
        when(hotelRepository.findAll()).thenReturn(List.of(hotel1, hotel2));

        // Call the REAL HotelService.getAllHotels() method.
        //
        // hotelService itself is NOT mocked.
        //
        // Therefore the actual production code inside getAllHotels() executes:
        //
        // hotelRepository.findAll()   <-- MOCK provides our two Hotels
        //          .stream()           <-- REAL service code
        //          .map(this::toResponse) <-- REAL service code
        //          .toList();          <-- REAL service code
        //
        // IMPORTANT:
        // "response" is the ACTUAL response produced by the REAL service.
        // We are NOT manually creating or mocking this response.
        //
        // Only the data coming FROM the repository is fake.
        // What HotelService does with that data, including converting
        // Hotel -> HotelResponseDTO, is real production code.
        List<HotelResponseDTO> response = hotelService.getAllHotels();

        // Verify that the REAL service called repository.findAll() exactly once.
        //
        // verify() does NOT call findAll() again.
        // Mockito simply checks its recorded history to confirm that
        // findAll() was called during hotelService.getAllHotels().
        // Verify that findAll() was actually called so we know the response above
        // was produced after retrieving the hotels through findAll(), rather than
        // the service simply forging/hardcoding a response and returning it without calling findAll().
        verify(hotelRepository).findAll();

        assertThat(response.size()).isEqualTo(2);
        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(0).name()).isEqualTo("Grand Plaza");
        assertThat(response.get(0).location()).isEqualTo("Morocco");
        assertThat(response.get(1).id()).isEqualTo(2L);
        assertThat(response.get(1).name()).isEqualTo("Fairmont");
        assertThat(response.get(1).location()).isEqualTo("Italy");
    }

    @Test
    void getAllHotels_returnEmptyListOfHotelResponse_success() {
        when(hotelRepository.findAll()).thenReturn(List.of());
        List<HotelResponseDTO> response = hotelService.getAllHotels();
        verify(hotelRepository).findAll();
        assertThat(response).isEmpty();
    }

    @Test
    void getHotelById_returnHotelResponse_success() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Grad Plaza");
        hotel.setLocation("Morocco");

        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        HotelResponseDTO response = hotelService.getHotelById(1L);

        verify(hotelRepository).findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Grad Plaza");
        assertThat(response.location()).isEqualTo("Morocco");
    }

    /**
     * Meaning:
     *
     * Pretend the database did not find hotel 999.
     *
     * Then the real service has to decide what to do. Your test expects it to throw ResourceNotFoundException.
     */
    @Test
    void getHotelById_passWrongHotelId_fail() {
        when(hotelRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hotelService.getHotelById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Hotel not found with id: 999");
        verify(hotelRepository).findById(999L);
    }

    @Test
    void updateHotelById_passAllFields_success() {
        Hotel existingHotel = new Hotel();
        existingHotel.setId(1L);
        existingHotel.setName("Old Name");
        existingHotel.setLocation("Old Location");

        HotelRequestDTO request = new HotelRequestDTO("New Name", "New Location");

        when(hotelRepository.findById(1L)).thenReturn(Optional.of(existingHotel));
        when(hotelRepository.save(existingHotel)).thenReturn(existingHotel);

        HotelResponseDTO response = hotelService.updateHotelById(1L, request);
        verify(hotelRepository).findById(1L);
        verify(hotelRepository).save(existingHotel);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.location()).isEqualTo("New Location");
    }

    @Test
    void updateHotelById_passSomeFields_success() {
        Hotel existingHotel = new Hotel();
        existingHotel.setId(1L);
        existingHotel.setName("Old Name");
        existingHotel.setLocation("Old Location");

        HotelRequestDTO request = new HotelRequestDTO("New Name", "Old Location");

        when(hotelRepository.findById(1L)).thenReturn(Optional.of(existingHotel));
        when(hotelRepository.save(existingHotel)).thenReturn(existingHotel);

        HotelResponseDTO response = hotelService.updateHotelById(1L, request);
        verify(hotelRepository).findById(1L);
        verify(hotelRepository).save(existingHotel);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.location()).isEqualTo("Old Location");
    }

    @Test
    void updateHotelById_passWrongHotelId_fail() {
        when(hotelRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hotelService.updateHotelById(999L, new HotelRequestDTO("Name", "Location")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Hotel not found with id: 999");
        verify(hotelRepository).findById(999L);
    }

    @Test
    void deleteHotel_passValidHotelId_success() {
        when(hotelRepository.existsById(1L)).thenReturn(true);
        hotelService.deleteHotel(1L);
        verify(hotelRepository).existsById(1L); // verify(hotelRepository).existsById(1L); confirms the guard check happened before delete.
        verify(hotelRepository).deleteById(1L); // confirms hotelRepository.deleteById(id) was called with that ID.
    }

    @Test
    void deleteHotel_passWrongHotelId_fail() {
        when(hotelRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> hotelService.deleteHotel(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Hotel not found with id: 999");
        verify(hotelRepository).existsById(999L); // confirms the guard check happened before attempting delete.
        verify(hotelRepository, never()).deleteById(999L); // confirms deleteById was never called.
    }
}
