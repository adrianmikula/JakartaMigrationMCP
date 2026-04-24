package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.service.impl.AppServerScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.BeanValidationScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.BuildConfigScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.CdiInjectionScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.ClassloaderModuleScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.ConfigFileScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.DeprecatedApiScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.DockerCicdScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.IntegrationPointsScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.JpaAnnotationScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.JmsMessagingScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.LoggingMetricsScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.ReflectionUsageScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.RestSoapScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.SecurityApiScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.SerializationCacheScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.ServletJspScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.TestContainersScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.ThirdPartyLibScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.TransitiveDependencyScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.UnitTestScannerImpl;
import adrianmikula.jakartamigration.advancedscanning.service.impl.ScanRecipeRecommendationServiceImpl;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;

/**
 * Module that provides access to all premium advanced scanning services.
 * This is the main entry point for the advanced scanning features.
 */
public class AdvancedScanningModule {

    private final JpaAnnotationScanner jpaAnnotationScanner;
    private final BeanValidationScanner beanValidationScanner;
    private final ServletJspScanner servletJspScanner;
    private final CdiInjectionScanner cdiInjectionScanner;
    private final BuildConfigScanner buildConfigScanner;
    private final RestSoapScanner restSoapScanner;
    private final DeprecatedApiScanner deprecatedApiScanner;
    private final SecurityApiScanner securityApiScanner;
    private final JmsMessagingScanner jmsMessagingScanner;
    private final TransitiveDependencyScanner transitiveDependencyScanner;
    private final ConfigFileScanner configFileScanner;
    private final ClassloaderModuleScanner classloaderModuleScanner;
    private final LoggingMetricsScanner loggingMetricsScanner;
    private final SerializationCacheScanner serializationCacheScanner;
    private final ReflectionUsageScanner reflectionUsageScanner;
    private final ThirdPartyLibScanner thirdPartyLibScanner;
    private final UnitTestScanner unitTestScanner;
    private final TestContainersScanner testContainersScanner;
    private final IntegrationPointsScanner integrationPointsScanner;
    private final AppServerScanner appServerScanner;
    private final DockerCicdScanner dockerCicdScanner;
    private final ScanRecipeRecommendationService recipeRecommendationService;

    public AdvancedScanningModule(RecipeService recipeService) {
        // Initialize all scanners
        this.jpaAnnotationScanner = new JpaAnnotationScannerImpl();
        this.beanValidationScanner = new BeanValidationScannerImpl();
        this.servletJspScanner = new ServletJspScannerImpl();
        this.cdiInjectionScanner = new CdiInjectionScannerImpl();
        this.buildConfigScanner = new BuildConfigScannerImpl();
        this.restSoapScanner = new RestSoapScannerImpl();
        this.deprecatedApiScanner = new DeprecatedApiScannerImpl();
        this.securityApiScanner = new SecurityApiScannerImpl();
        this.jmsMessagingScanner = new JmsMessagingScannerImpl();
        this.transitiveDependencyScanner = new TransitiveDependencyScannerImpl();
        this.configFileScanner = new ConfigFileScannerImpl();
        this.classloaderModuleScanner = new ClassloaderModuleScannerImpl();
        this.loggingMetricsScanner = new LoggingMetricsScannerImpl();
        this.serializationCacheScanner = new SerializationCacheScannerImpl();
        this.reflectionUsageScanner = new ReflectionUsageScannerImpl();
        this.thirdPartyLibScanner = new ThirdPartyLibScannerImpl();
        this.unitTestScanner = new UnitTestScannerImpl();
        this.testContainersScanner = new TestContainersScannerImpl();
        this.integrationPointsScanner = new IntegrationPointsScannerImpl();
        this.appServerScanner = new AppServerScannerImpl();
        this.dockerCicdScanner = new DockerCicdScannerImpl();
        this.recipeRecommendationService = new ScanRecipeRecommendationServiceImpl(recipeService);
    }

    /**
     * Gets the JPA Annotation Scanner.
     */
    public JpaAnnotationScanner getJpaAnnotationScanner() {
        return jpaAnnotationScanner;
    }

    /**
     * Gets the Bean Validation Scanner.
     */
    public BeanValidationScanner getBeanValidationScanner() {
        return beanValidationScanner;
    }

    /**
     * Gets the Servlet/JSP Scanner.
     */
    public ServletJspScanner getServletJspScanner() {
        return servletJspScanner;
    }

    /**
     * Gets the CDI/Injection Scanner.
     */
    public CdiInjectionScanner getCdiInjectionScanner() {
        return cdiInjectionScanner;
    }

    /**
     * Gets the Build Configuration Scanner.
     */
    public BuildConfigScanner getBuildConfigScanner() {
        return buildConfigScanner;
    }

    /**
     * Gets the REST/SOAP Scanner.
     */
    public RestSoapScanner getRestSoapScanner() {
        return restSoapScanner;
    }

    /**
     * Gets the Deprecated API Scanner.
     */
    public DeprecatedApiScanner getDeprecatedApiScanner() {
        return deprecatedApiScanner;
    }

    /**
     * Gets the Security API Scanner.
     */
    public SecurityApiScanner getSecurityApiScanner() {
        return securityApiScanner;
    }

    /**
     * Gets the JMS/Messaging Scanner.
     */
    public JmsMessagingScanner getJmsMessagingScanner() {
        return jmsMessagingScanner;
    }

    /**
     * Gets the Transitive Dependency Scanner.
     */
    public TransitiveDependencyScanner getTransitiveDependencyScanner() {
        return transitiveDependencyScanner;
    }

    /**
     * Gets the Configuration File Scanner.
     */
    public ConfigFileScanner getConfigFileScanner() {
        return configFileScanner;
    }

    /**
     * Gets the Classloader/Module Scanner.
     */
    public ClassloaderModuleScanner getClassloaderModuleScanner() {
        return classloaderModuleScanner;
    }

    /**
     * Gets the Logging/Metrics Scanner.
     */
    public LoggingMetricsScanner getLoggingMetricsScanner() {
        return loggingMetricsScanner;
    }

    /**
     * Gets the Serialization/Cache Scanner.
     */
    public SerializationCacheScanner getSerializationCacheScanner() {
        return serializationCacheScanner;
    }

    /**
     * Gets the Reflection Usage Scanner.
     */
    public ReflectionUsageScanner getReflectionUsageScanner() {
        return reflectionUsageScanner;
    }

    /**
     * Gets the Third-Party Library Scanner.
     */
    public ThirdPartyLibScanner getThirdPartyLibScanner() {
        return thirdPartyLibScanner;
    }

    public UnitTestScanner getUnitTestScanner() {
        return unitTestScanner;
    }

    public TestContainersScanner getTestContainersScanner() {
        return testContainersScanner;
    }

    public IntegrationPointsScanner getIntegrationPointsScanner() {
        return integrationPointsScanner;
    }

    public AppServerScanner getAppServerScanner() {
        return appServerScanner;
    }

    /**
     * Gets the Docker/CI-CD Scanner.
     */
    public DockerCicdScanner getDockerCicdScanner() {
        return dockerCicdScanner;
    }

    /**
     * Gets the Recipe Recommendation Service.
     */
    public ScanRecipeRecommendationService getRecipeRecommendationService() {
        return recipeRecommendationService;
    }
}
