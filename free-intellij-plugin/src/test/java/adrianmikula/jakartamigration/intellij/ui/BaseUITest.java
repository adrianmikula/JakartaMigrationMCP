package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

/**
 * Base test class for UI components with IntelliJ test framework setup
 */
public abstract class BaseUITest extends LightJavaCodeInsightFixtureTestCase {

    @BeforeEach
    public void setUpTest() throws Exception {
        super.setUp();
    }

    @AfterEach  
    public void tearDownTest() throws Exception {
        super.tearDown();
    }

    /**
     * Find component by type recursively in container
     */
    @SuppressWarnings("unchecked")
    protected <T> T findComponentByType(java.awt.Container container, Class<T> type) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            java.awt.Component component = container.getComponent(i);
            if (type.isInstance(component)) {
                return (T) component;
            }
            if (component instanceof java.awt.Container) {
                T found = findComponentByType((java.awt.Container) component, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}