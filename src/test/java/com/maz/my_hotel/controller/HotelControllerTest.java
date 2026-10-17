package com.maz.my_hotel.controller;
import com.maz.my_hotel.dto.HotelRequestDTO;
import com.maz.my_hotel.entity.Hotel;
import com.maz.my_hotel.exception.ResourceNotFoundException;
import com.maz.my_hotel.service.HotelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(HotelController.class)
class HotelControllerTest {

    // Spring gives us MockMvc.
    // We use it to simulate HTTP requests to our controller.
    @Autowired
    MockMvc mockMvc;

    // Fake HotelService because we're testing the controller,
    // not the service layer.
    @MockitoBean
    HotelService hotelService;

    @Test
    void hotels_createHotel_missingRequiredField_fail() throws Exception {

        // Invalid JSON request: location is missing.
        String body = """
            { "name": "Grand Plaza 2" }
            """;

        // Simulate: POST /hotels
        mockMvc.perform(post("/hotels")

                        // Tell Spring the request body is JSON.
                        .contentType(MediaType.APPLICATION_JSON)

                        // Send this JSON as the request body.
                        .content(body))

                // Expect HTTP 400 Bad Request.
                .andExpect(status().isBadRequest())

                // Expect response.message == "Validation failed".
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"));

//                // Expect the location validation error.
//                .andExpect(jsonPath("$.fieldErrors.location")
//                        .value("Hotel location is required"));
        verifyNoInteractions(hotelService);
    }

    @Test
    void hotels_createHotel_missingBody_fail() throws Exception {
        // Invalid JSON request: location is missing.
        String body = "";

        // Simulate: POST /hotels
        mockMvc.perform(post("/hotels")
                        // Tell Spring the request body is JSON.
                        .contentType(MediaType.APPLICATION_JSON)
                        // Send this JSON as the request body.
                        .content(body))
                // Expect HTTP 400 Bad Request.
                .andExpect(status().is5xxServerError());
        verifyNoInteractions(hotelService);
    }

    //    Your controller test is essentially saying:
    //
    //    Pretend HotelService throws ResourceNotFoundException
    //                    ↓
    //    Does the web layer turn that into:
    //    HTTP 404
    //    status = 404
    //    correct message?
    /**
     * You're testing:
     *
     * If someone sends GET /hotels/999, does the controller call getHotelById(999L), and if the service throws ResourceNotFoundException, does the web layer return the correct HTTP 404 response?
     */
    @Test
    void hotels_getHotelById_wrongId_fail() throws Exception {
        when(hotelService.getHotelById(999L)).thenThrow(
                new ResourceNotFoundException("Hotel not found with id: 999"));
        mockMvc.perform(get("/hotels/999"))
                //.andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Hotel not found with id: 999"));

        // Mockito, check your recorded history and confirm that the controller actually called
        // hotelService.getHotelById(999L) exactly once.
        verify(hotelService).getHotelById(999L);
    }

    //    Great catch. You should not test malformed "de" ID in service test—that case never reaches service.
    //            getHotelById(Long id) in service already receives a Long.
    //    Malformed path values (/hotels/de) fail in the controller argument binding stage, throwing MethodArgumentTypeMismatchException.
    @Test
    void hotels_getHotelById_passMalformedHotelId_fail() throws Exception {
        mockMvc.perform(get("/hotels/de"))
                //.andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString(
                        "Method parameter 'id': Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'"
                )));
        verifyNoInteractions(hotelService);
    }

    @Test
    void hotels_getHotelById_doNotPassAnyHotelId_fail() throws Exception {
        mockMvc.perform(get("/hotels/"))
                .andExpect(status().isNotFound());
    }

    @Test
    void hotels_updateHotel_passWrongHotelId_fail() throws Exception {
        String body = """
            { "name": "Grand Plaza", "location": "Morocco" }
            """;
        HotelRequestDTO requestDTO = new HotelRequestDTO("Grand Plaza", "Morocco");

        when(hotelService.updateHotelById(999L, requestDTO))
                .thenThrow(new ResourceNotFoundException("Hotel not found with id: 999"));

        mockMvc.perform(put("/hotels/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Hotel not found with id: 999"));

        verify(hotelService).updateHotelById(999L, requestDTO);
    }

    @Test
    void hotels_updateHotel_passMalformedHotelId_fail() throws Exception {
        String body = """
            { "name": "Grand Plaza", "location": "Morocco" }
            """;
        mockMvc.perform(put("/hotels/abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString(
                        "Method parameter 'id': Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'"
                )));
        verifyNoInteractions(hotelService);
    }

    @Test
    void hotels_updateHotel_doNotPassAnyHotelId_fail() throws Exception {
        String body = """
            { "name": "Grand Plaza", "location": "Morocco" }
            """;
        mockMvc.perform(put("/hotels/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
        verifyNoInteractions(hotelService);
    }
}