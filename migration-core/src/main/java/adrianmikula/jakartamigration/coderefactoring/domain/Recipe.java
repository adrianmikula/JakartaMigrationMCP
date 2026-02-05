/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Represents a refactoring recipe (e.g., OpenRewrite recipe).
 * 
 * NOTE: This is a community stub. Full implementation with OpenRewrite recipes
 * is available in the premium edition.
 */
public record Recipe(
    String name,
    String description,
    String pattern,
    SafetyLevel safety,
    boolean reversible
) {
    public Recipe {
        if (name == null || name.isBlank()) {
            name = "Unknown";
        }
        if (description == null) {
            description = "";
        }
        if (pattern == null) {
            pattern = "";
        }
        if (safety == null) {
            safety = SafetyLevel.MEDIUM;
        }
    }
    
    /**
     * Creates the standard Jakarta namespace migration recipe.
     */
    public static Recipe jakartaNamespaceRecipe() {
        return new Recipe(
            "AddJakartaNamespace",
            "Converts javax.* imports and references to jakarta.*",
            "javax.* → jakarta.*",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for updating persistence.xml.
     */
    public static Recipe persistenceXmlRecipe() {
        return new Recipe(
            "UpdatePersistenceXml",
            "Updates persistence.xml namespace to Jakarta",
            "xmlns:persistence='http://java.sun.com/xml/ns/persistence' → xmlns:persistence='https://jakarta.ee/xml/ns/persistence'",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for updating web.xml.
     */
    public static Recipe webXmlRecipe() {
        return new Recipe(
            "UpdateWebXml",
            "Updates web.xml namespace to Jakarta",
            "http://java.sun.com/xml/ns/javaee → https://jakarta.ee/xml/ns/jakartaee",
            SafetyLevel.MEDIUM,
            true
        );
    }
}
