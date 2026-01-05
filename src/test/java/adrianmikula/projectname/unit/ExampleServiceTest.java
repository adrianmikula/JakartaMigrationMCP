package adrianmikula.projectname.unit;

import adrianmikula.projectname.dao.ExampleRepository;
import adrianmikula.projectname.entity.ExampleEntity;
import adrianmikula.projectname.rest.Example;
import adrianmikula.projectname.service.ExampleMapper;
import adrianmikula.projectname.service.ExampleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Example unit test using JUnit and Mockito.
 * This is a template example - replace with your own unit tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExampleService Unit Tests")
class ExampleServiceTest {

    @Mock
    private ExampleRepository exampleRepository;

    @Mock
    private ExampleMapper exampleMapper;

    @InjectMocks
    private ExampleService exampleService;

    private Example testExample;
    private ExampleEntity testEntity;

    @BeforeEach
    void setUp() {
        UUID testId = UUID.randomUUID();
        testExample = Example.builder()
                .id(testId)
                .name("Test Example")
                .description("Test Description")
                .category("Test Category")
                .build();

        testEntity = ExampleEntity.builder()
                .id(testId)
                .name("Test Example")
                .description("Test Description")
                .category("Test Category")
                .build();
    }

    @Test
    @DisplayName("Should create example successfully")
    void shouldCreateExample() {
        // Given
        when(exampleRepository.existsByName(anyString())).thenReturn(false);
        when(exampleMapper.toEntity(any(Example.class))).thenReturn(testEntity);
        when(exampleRepository.save(any(ExampleEntity.class))).thenReturn(testEntity);

        // When
        ExampleEntity result = exampleService.create(testExample);

        // Then
        assertNotNull(result);
        assertEquals(testEntity.getName(), result.getName());
        verify(exampleRepository, times(1)).save(any(ExampleEntity.class));
    }

    @Test
    @DisplayName("Should get all examples")
    void shouldGetAllExamples() {
        // Given
        when(exampleRepository.findAll()).thenReturn(List.of(testEntity));
        when(exampleMapper.toDomain(any(ExampleEntity.class))).thenReturn(testExample);

        // When
        List<Example> result = exampleService.getAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(exampleRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get example by ID when exists")
    void shouldGetExampleByIdWhenExists() {
        // Given
        UUID testId = UUID.randomUUID();
        when(exampleRepository.findById(testId)).thenReturn(java.util.Optional.of(testEntity));
        when(exampleMapper.toDomain(any(ExampleEntity.class))).thenReturn(testExample);

        // When
        var result = exampleService.getById(testId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testExample.getName(), result.get().getName());
        verify(exampleRepository, times(1)).findById(testId);
    }

    @Test
    @DisplayName("Should return empty when example not found by ID")
    void shouldReturnEmptyWhenExampleNotFound() {
        // Given
        UUID testId = UUID.randomUUID();
        when(exampleRepository.findById(testId)).thenReturn(java.util.Optional.empty());

        // When
        var result = exampleService.getById(testId);

        // Then
        assertTrue(result.isEmpty());
        verify(exampleRepository, times(1)).findById(testId);
        verify(exampleMapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("Should throw exception when name is null")
    void shouldThrowExceptionWhenNameIsNull() {
        // Given
        Example exampleWithNullName = Example.builder()
                .name(null)
                .description("Test Description")
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            exampleService.create(exampleWithNullName);
        });
        assertEquals("Name is required", exception.getMessage());
        verify(exampleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when name is empty")
    void shouldThrowExceptionWhenNameIsEmpty() {
        // Given
        Example exampleWithEmptyName = Example.builder()
                .name("   ")
                .description("Test Description")
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            exampleService.create(exampleWithEmptyName);
        });
        assertEquals("Name is required", exception.getMessage());
        verify(exampleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when duplicate name exists")
    void shouldThrowExceptionWhenDuplicateNameExists() {
        // Given
        when(exampleRepository.existsByName(testExample.getName())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            exampleService.create(testExample);
        });
        assertTrue(exception.getMessage().contains("already exists"));
        verify(exampleRepository, times(1)).existsByName(testExample.getName());
        verify(exampleRepository, never()).save(any());
    }
}

