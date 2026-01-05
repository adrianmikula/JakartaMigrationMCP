package adrianmikula.projectname.unit;

import adrianmikula.projectname.entity.ExampleEntity;
import adrianmikula.projectname.rest.Example;
import adrianmikula.projectname.service.ExampleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExampleMapper.
 */
@DisplayName("ExampleMapper Unit Tests")
class ExampleMapperTest {

    private ExampleMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ExampleMapper();
    }

    @Test
    @DisplayName("Should map domain to entity successfully")
    void shouldMapDomainToEntity() {
        // Given
        UUID testId = UUID.randomUUID();
        Example domain = Example.builder()
                .id(testId)
                .name("Test Name")
                .description("Test Description")
                .category("Test Category")
                .status("ACTIVE")
                .build();

        // When
        ExampleEntity entity = mapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals("Test Name", entity.getName());
        assertEquals("Test Description", entity.getDescription());
        assertEquals("Test Category", entity.getCategory());
        assertEquals("ACTIVE", entity.getStatus());
    }

    @Test
    @DisplayName("Should return null when mapping null domain to entity")
    void shouldReturnNullWhenMappingNullDomainToEntity() {
        // When
        ExampleEntity entity = mapper.toEntity(null);

        // Then
        assertNull(entity);
    }

    @Test
    @DisplayName("Should map entity to domain successfully")
    void shouldMapEntityToDomain() {
        // Given
        UUID testId = UUID.randomUUID();
        ExampleEntity entity = ExampleEntity.builder()
                .id(testId)
                .name("Test Name")
                .description("Test Description")
                .category("Test Category")
                .status("ACTIVE")
                .build();

        // When
        Example domain = mapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(testId, domain.getId());
        assertEquals("Test Name", domain.getName());
        assertEquals("Test Description", domain.getDescription());
        assertEquals("Test Category", domain.getCategory());
        assertEquals("ACTIVE", domain.getStatus());
    }

    @Test
    @DisplayName("Should return null when mapping null entity to domain")
    void shouldReturnNullWhenMappingNullEntityToDomain() {
        // When
        Example domain = mapper.toDomain(null);

        // Then
        assertNull(domain);
    }

    @Test
    @DisplayName("Should handle domain with null fields")
    void shouldHandleDomainWithNullFields() {
        // Given
        Example domain = Example.builder()
                .id(null)
                .name(null)
                .description(null)
                .category(null)
                .status(null)
                .build();

        // When
        ExampleEntity entity = mapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertNull(entity.getId());
        assertNull(entity.getName());
        assertNull(entity.getDescription());
        assertNull(entity.getCategory());
        assertNull(entity.getStatus());
    }

    @Test
    @DisplayName("Should handle entity with null fields")
    void shouldHandleEntityWithNullFields() {
        // Given
        ExampleEntity entity = ExampleEntity.builder()
                .id(null)
                .name(null)
                .description(null)
                .category(null)
                .status(null)
                .build();

        // When
        Example domain = mapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertNull(domain.getId());
        assertNull(domain.getName());
        assertNull(domain.getDescription());
        assertNull(domain.getCategory());
        assertNull(domain.getStatus());
    }
}

