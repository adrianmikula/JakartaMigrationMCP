package adrianmikula.jakartamigration.coderefactoring.service.util;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeType;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility to seed default recipes into the central store.
 */
@Slf4j
public class RecipeSeeder {

        public static void seedDefaultRecipes(CentralMigrationAnalysisStore store) {
                log.info("Seeding default migration recipes...");

                // 1. Java Source // JAVA Recipes
                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate javax.servlet to jakarta.servlet")
                                .description("Replaces javax.servlet imports with jakarta.servlet equivalents.")
                                .category(RecipeCategory.JAVA)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName(
                                                "org.openrewrite.java.migrate.jakarta.JavaxServletToJakartaServlet")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate JSF to Jakarta Faces")
                                .description("Replaces javax.faces imports with jakarta.faces equivalents.")
                                .category(RecipeCategory.JAVA)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName("org.openrewrite.java.migrate.jakarta.JavaxFacesToJakartaFaces")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate JMS to Jakarta Messaging")
                                .description("Replaces javax.jms imports with jakarta.jms equivalents.")
                                .category(RecipeCategory.JAVA)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName("org.openrewrite.java.migrate.jakarta.JavaxJmsToJakartaJms")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate JTA to Jakarta Transactions")
                                .description("Replaces javax.transaction imports with jakarta.transaction equivalents.")
                                .category(RecipeCategory.JAVA)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName(
                                                "org.openrewrite.java.migrate.jakarta.JavaxTransactionMigrationToJakartaTransaction")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate JAX-RS to Jakarta REST")
                                .description("Replaces javax.ws.rs imports with jakarta.ws.rs equivalents.")
                                .category(RecipeCategory.JAVA)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName("org.openrewrite.java.migrate.jakarta.JavaxWsToJakartaWs")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate EJB to Jakarta Enterprise Beans")
                                .description("Replaces javax.ejb imports with jakarta.ejb equivalents.")
                                .category(RecipeCategory.JAVA)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName("org.openrewrite.java.migrate.jakarta.JavaxEjbToJakartaEjb")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate JAXB to Jakarta XML Binding")
                                .description("Replaces javax.xml.bind imports with jakarta.xml.bind equivalents.")
                                .category(RecipeCategory.JAVA)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName(
                                                "org.openrewrite.java.migrate.jakarta.JavaxXmlBindMigrationToJakartaXmlBind")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate JAF to Jakarta Activation")
                                .description("Replaces javax.activation imports with jakarta.activation equivalents.")
                                .category(RecipeCategory.JAVA)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName(
                                                "org.openrewrite.java.migrate.jakarta.JavaxActivationMigrationToJakartaActivation")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate Batch to Jakarta Batch")
                                .description("Replaces javax.batch imports with jakarta.batch equivalents.")
                                .category(RecipeCategory.JAVA)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName(
                                                "org.openrewrite.java.migrate.jakarta.JavaxBatchMigrationToJakartaBatch")
                                .reversible(true)
                                .build());

                // XML Recipes
                store.saveRecipe(RecipeDefinition.builder()
                                .name("Update web.xml to Jakarta EE 9+")
                                .description("Updates web.xml namespaces and version to Jakarta EE equivalents.")
                                .category(RecipeCategory.XML)
                                .recipeType(RecipeType.REGEX)
                                .pattern("xmlns=\"http://xmlns\\.jcp\\.org/xml/ns/javaee\"")
                                .replacement("xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"")
                                .filePattern("**/web.xml")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Update persistence.xml to Jakarta EE 9+")
                                .description("Updates persistence.xml namespaces to Jakarta EE equivalents.")
                                .category(RecipeCategory.XML)
                                .recipeType(RecipeType.REGEX)
                                .pattern("xmlns=\"http://xmlns\\.jcp\\.org/xml/ns/persistence\"")
                                .replacement("xmlns=\"https://jakarta.ee/xml/ns/persistence\"")
                                .filePattern("**/persistence.xml")
                                .reversible(true)
                                .build());

                // ANNOTATIONS Recipes
                store.saveRecipe(RecipeDefinition.builder()
                                .name("Migrate @PersistenceContext")
                                .description("Migrates javax.persistence annotations to jakarta.persistence.")
                                .category(RecipeCategory.ANNOTATIONS)
                                .recipeType(RecipeType.OPENREWRITE)
                                .openRewriteRecipeName(
                                                "org.openrewrite.java.migrate.jakarta.JavaxPersistenceToJakartaPersistence")
                                .reversible(true)
                                .build());

                // BUILD_DEPENDENCIES Recipes
                store.saveRecipe(RecipeDefinition.builder()
                                .name("Update Hibernate to Jakarta-compatible version")
                                .description("Updates Hibernate dependencies in pom.xml or build.gradle.")
                                .category(RecipeCategory.BUILD_DEPENDENCIES)
                                .recipeType(RecipeType.REGEX)
                                .pattern("org\\.hibernate:hibernate-core:5\\.[0-9]+\\.[0-9]+")
                                .replacement("org.hibernate:hibernate-core:6.2.0.Final")
                                .filePattern("**/pom.xml,**/build.gradle*")
                                .reversible(true)
                                .build());

                store.saveRecipe(RecipeDefinition.builder()
                                .name("Update persistence.xml version")
                                .description("Update persistence.xml version to 3.0 for Jakarta EE.")
                                .category(RecipeCategory.XML)
                                .recipeType(RecipeType.REGEX)
                                .pattern("version=\"2.[0-2]\"")
                                .replacement("version=\"3.0\"")
                                .filePattern("**/persistence.xml")
                                .reversible(true)
                                .build());

                // 3. Dependency Recipes (Regex/OpenRewrite)
                store.saveRecipe(RecipeDefinition.builder()
                                .name("Upgrade Maven Dependency: javax.servlet-api")
                                .description("Replace javax.servlet-api with jakarta.servlet-api 5.0.0+")
                                .category(RecipeCategory.BUILD_DEPENDENCIES)
                                .recipeType(RecipeType.REGEX)
                                .pattern(
                                                "<groupId>javax.servlet</groupId>\\s*<artifactId>javax.servlet-api</artifactId>\\s*<version>.*?</version>")
                                .replacement(
                                                "<groupId>jakarta.servlet</groupId>\n    <artifactId>jakarta.servlet-api</artifactId>\n    <version>5.0.0</version>")
                                .filePattern("**/pom.xml")
                                .reversible(true)
                                .build());

                log.info("Finished seeding recipes.");
        }

        public static void seedUpgradeRecommendations(CentralMigrationAnalysisStore store) {
                log.info("Seeding upgrade recommendations from recipes...");

                // Seed javax.servlet -> jakarta.servlet mapping
                store.saveUpgradeRecommendation(
                        "javax.servlet",
                        "javax.servlet-api",
                        "jakarta.servlet",
                        "jakarta.servlet-api",
                        "5.0.0",
                        "Migrate javax.servlet to jakarta.servlet");

                // Seed javax.faces -> jakarta.faces mapping
                store.saveUpgradeRecommendation(
                        "javax.faces",
                        "javax.faces",
                        "jakarta.faces",
                        "jakarta.faces",
                        "4.0.0",
                        "Migrate JSF to Jakarta Faces");

                // Seed javax.jms -> jakarta.jms mapping
                store.saveUpgradeRecommendation(
                        "javax.jms",
                        "javax.jms-api",
                        "jakarta.jms",
                        "jakarta.jms-api",
                        "3.1.0",
                        "Migrate JMS to Jakarta Messaging");

                // Seed javax.transaction -> jakarta.transaction mapping
                store.saveUpgradeRecommendation(
                        "javax.transaction",
                        "javax.transaction-api",
                        "jakarta.transaction",
                        "jakarta.transaction-api",
                        "2.0.0",
                        "Migrate JTA to Jakarta Transactions");

                // Seed javax.ws.rs -> jakarta.ws.rs mapping
                store.saveUpgradeRecommendation(
                        "javax.ws.rs",
                        "javax.ws.rs-api",
                        "jakarta.ws.rs",
                        "jakarta.ws.rs-api",
                        "3.1.0",
                        "Migrate JAX-RS to Jakarta REST");

                // Seed javax.ejb -> jakarta.ejb mapping
                store.saveUpgradeRecommendation(
                        "javax.ejb",
                        "javax.ejb-api",
                        "jakarta.ejb",
                        "jakarta.ejb-api",
                        "4.0.0",
                        "Migrate EJB to Jakarta Enterprise Beans");

                // Seed javax.xml.bind -> jakarta.xml.bind mapping
                store.saveUpgradeRecommendation(
                        "javax.xml.bind",
                        "jaxb-api",
                        "jakarta.xml.bind",
                        "jakarta.xml.bind-api",
                        "4.0.0",
                        "Migrate JAXB to Jakarta XML Binding");

                // Seed javax.activation -> jakarta.activation mapping
                store.saveUpgradeRecommendation(
                        "javax.activation",
                        "activation",
                        "jakarta.activation",
                        "jakarta.activation-api",
                        "2.1.0",
                        "Migrate JAF to Jakarta Activation");

                // Seed javax.batch -> jakarta.batch mapping
                store.saveUpgradeRecommendation(
                        "javax.batch",
                        "javax.batch-api",
                        "jakarta.batch",
                        "jakarta.batch-api",
                        "2.1.0",
                        "Migrate Batch to Jakarta Batch");

                // Seed javax.persistence -> jakarta.persistence mapping
                store.saveUpgradeRecommendation(
                        "javax.persistence",
                        "javax.persistence-api",
                        "jakarta.persistence",
                        "jakarta.persistence-api",
                        "3.1.0",
                        "Migrate @PersistenceContext");

                // Seed hibernate-core mapping
                store.saveUpgradeRecommendation(
                        "org.hibernate",
                        "hibernate-core",
                        "org.hibernate",
                        "hibernate-core",
                        "6.2.0.Final",
                        "Update Hibernate to Jakarta-compatible version");

                log.info("Finished seeding upgrade recommendations.");
        }
}
