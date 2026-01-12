package adrianmikula.projectname.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Example DTO for request validation.
 * This is a template example - replace with your own DTOs.
 */
@Data
public class CreateExampleRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    private String name;
    
    private String description;
    
    private String category;
}

