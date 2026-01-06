package adrianmikula.projectname.unit;

import adrianmikula.projectname.controller.ExampleController;
import adrianmikula.projectname.entity.ExampleEntity;
import adrianmikula.projectname.rest.CreateExampleRequest;
import adrianmikula.projectname.rest.ErrorResponse;
import adrianmikula.projectname.rest.Example;
import adrianmikula.projectname.service.ExampleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ExampleController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExampleController Unit Tests")
class ExampleControllerTest {

    @Mock
    private ExampleService exampleService;

    @InjectMocks
    private ExampleController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Example testExample;
    private ExampleEntity testEntity;
    private CreateExampleRequest testRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        objectMapper = new ObjectMapper();

        UUID testId = UUID.randomUUID();
        testExample = Example.builder()
                .id(testId)
                .name("Test Example")
                .description("Test Description")
                .category("Test Category")
                .status("ACTIVE")
                .build();

        testEntity = ExampleEntity.builder()
                .id(testId)
                .name("Test Example")
                .description("Test Description")
                .category("Test Category")
                .status("ACTIVE")
                .build();

        testRequest = new CreateExampleRequest();
        testRequest.setName("Test Example");
        testRequest.setDescription("Test Description");
        testRequest.setCategory("Test Category");
    }

    @Test
    @DisplayName("Should create example successfully")
    void shouldCreateExampleSuccessfully() throws Exception {
        // Given
        when(exampleService.create(any(Example.class))).thenReturn(testEntity);

        // When & Then
        mockMvc.perform(post("/api/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testEntity.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Example"));

        verify(exampleService, times(1)).create(any(Example.class));
    }

    @Test
    @DisplayName("Should return conflict when duplicate name exists")
    void shouldReturnConflictWhenDuplicateNameExists() throws Exception {
        // Given
        when(exampleService.create(any(Example.class)))
                .thenThrow(new IllegalArgumentException("Example with this name already exists: Test Example"));

        // When & Then
        mockMvc.perform(post("/api/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());

        verify(exampleService, times(1)).create(any(Example.class));
    }

    @Test
    @DisplayName("Should get all examples successfully")
    void shouldGetAllExamplesSuccessfully() throws Exception {
        // Given
        when(exampleService.getAll()).thenReturn(List.of(testExample));

        // When & Then
        mockMvc.perform(get("/api/examples")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Test Example"));

        verify(exampleService, times(1)).getAll();
    }

    @Test
    @DisplayName("Should get example by ID when exists")
    void shouldGetExampleByIdWhenExists() throws Exception {
        // Given
        UUID testId = UUID.randomUUID();
        when(exampleService.getById(testId)).thenReturn(Optional.of(testExample));

        // When & Then
        mockMvc.perform(get("/api/examples/{id}", testId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testExample.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Example"));

        verify(exampleService, times(1)).getById(testId);
    }

    @Test
    @DisplayName("Should return not found when example does not exist")
    void shouldReturnNotFoundWhenExampleDoesNotExist() throws Exception {
        // Given
        UUID testId = UUID.randomUUID();
        when(exampleService.getById(testId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/examples/{id}", testId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(exampleService, times(1)).getById(testId);
    }

    @Test
    @DisplayName("Should handle validation exception")
    void shouldHandleValidationException() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("createExampleRequest", "name", "Name is required");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // When
        var response = controller.handleValidationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("name: Name is required", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle validation exception with no field errors")
    void shouldHandleValidationExceptionWithNoFieldErrors() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        // When
        var response = controller.handleValidationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation failed", response.getBody().getMessage());
    }
}

