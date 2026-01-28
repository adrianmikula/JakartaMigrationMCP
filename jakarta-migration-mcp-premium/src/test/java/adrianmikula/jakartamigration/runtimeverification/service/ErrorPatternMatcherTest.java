package adrianmikula.jakartamigration.runtimeverification.service;

import adrianmikula.jakartamigration.runtimeverification.domain.ErrorCategory;
import adrianmikula.jakartamigration.runtimeverification.domain.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorPatternMatcher Tests")
class ErrorPatternMatcherTest {

    @Test
    @DisplayName("determineErrorType returns correct type for ClassNotFoundException")
    void determineErrorTypeClassNotFound() {
        assertThat(ErrorPatternMatcher.determineErrorType("java.lang.ClassNotFoundException: javax.servlet.Servlet"))
            .isEqualTo(ErrorType.CLASS_NOT_FOUND);
    }

    @Test
    @DisplayName("determineErrorType returns correct type for NoClassDefFoundError")
    void determineErrorTypeNoClassDef() {
        assertThat(ErrorPatternMatcher.determineErrorType("NoClassDefFoundError: javax.persistence.Entity"))
            .isEqualTo(ErrorType.NO_CLASS_DEF_FOUND);
    }

    @Test
    @DisplayName("determineErrorType returns OTHER for null message")
    void determineErrorTypeNull() {
        assertThat(ErrorPatternMatcher.determineErrorType(null)).isEqualTo(ErrorType.OTHER);
    }

    @Test
    @DisplayName("determineErrorType returns OTHER for unrelated message")
    void determineErrorTypeOther() {
        assertThat(ErrorPatternMatcher.determineErrorType("NullPointerException at line 42"))
            .isEqualTo(ErrorType.OTHER);
    }

    @Test
    @DisplayName("determineErrorCategory returns NAMESPACE_MIGRATION for javax ClassNotFoundException")
    void determineErrorCategoryNamespaceMigration() {
        ErrorCategory cat = ErrorPatternMatcher.determineErrorCategory(
            "ClassNotFoundException: javax.servlet.Servlet",
            "javax.servlet.http.HttpServlet"
        );
        assertThat(cat).isEqualTo(ErrorCategory.NAMESPACE_MIGRATION);
    }

    @Test
    @DisplayName("determineErrorCategory returns CLASSPATH_ISSUE for jakarta ClassNotFoundException")
    void determineErrorCategoryClasspathIssue() {
        ErrorCategory cat = ErrorPatternMatcher.determineErrorCategory(
            "ClassNotFoundException: jakarta.servlet.Servlet",
            "jakarta.servlet.http.HttpServlet"
        );
        assertThat(cat).isEqualTo(ErrorCategory.CLASSPATH_ISSUE);
    }

    @Test
    @DisplayName("determineErrorCategory returns UNKNOWN when both null")
    void determineErrorCategoryUnknownWhenNull() {
        assertThat(ErrorPatternMatcher.determineErrorCategory(null, null))
            .isEqualTo(ErrorCategory.UNKNOWN);
    }

    @Test
    @DisplayName("isJakartaMigrationRelated returns true for namespace migration category")
    void isJakartaMigrationRelatedTrue() {
        assertThat(ErrorPatternMatcher.isJakartaMigrationRelated(
            "ClassNotFoundException: javax.servlet.Servlet",
            "javax.servlet.http.HttpServlet"
        )).isTrue();
    }

    @Test
    @DisplayName("isJakartaMigrationRelated returns false for unrelated error")
    void isJakartaMigrationRelatedFalse() {
        assertThat(ErrorPatternMatcher.isJakartaMigrationRelated(
            "NullPointerException at Main.java:10",
            "com.example.Main"
        )).isFalse();
    }
}
