package com.teleport.loadplanner.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler cut;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private MethodArgumentNotValidException validationException;
    
    @Mock
    private BindingResult bindingResult;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cut = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }
    
    @Test
    void handleValidationException() {
        FieldError fieldError = new FieldError("object", "field", "Field is required");
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        
        ResponseEntity<ErrorResponse> response = cut.handleValidationException(validationException, request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Field is required", response.getBody().getMessage());
        assertEquals("/api/v1/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }
    
    @Test
    void handleValidationException_multiple_errors() {
        FieldError error1 = new FieldError("object", "field1", "Error 1");
        FieldError error2 = new FieldError("object", "field2", "Error 2");
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));
        
        ResponseEntity<ErrorResponse> response = cut.handleValidationException(validationException, request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Error 1"));
        assertTrue(response.getBody().getMessage().contains("Error 2"));
    }
    
    @Test
    void handleInvalidRequestException() {
        InvalidRequestException exception = new InvalidRequestException("Invalid request");
        
        ResponseEntity<ErrorResponse> response = cut.handleInvalidRequestException(exception, request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid request", response.getBody().getMessage());
        assertEquals("/api/v1/test", response.getBody().getPath());
    }
    
    @Test
    void handleGenericException() {
        Exception exception = new RuntimeException("Unexpected error");
        
        ResponseEntity<ErrorResponse> response = cut.handleGenericException(exception, request);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertEquals("/api/v1/test", response.getBody().getPath());
    }
}
