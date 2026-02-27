package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Represents a refactoring recipe (e.g., OpenRewrite recipe).
 */
public record Recipe(
        String name,
        String description,
        String pattern,
        SafetyLevel safety,
        boolean reversible) {
    public Recipe {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        if (safety == null) {
            throw new IllegalArgumentException("Safety cannot be null");
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
                true);
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
                true);
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
                true);
    }

    /**
     * Creates a recipe for JPA migration.
     */
    public static Recipe jpaRecipe() {
        return new Recipe(
                "MigrateJPA",
                "Converts javax.persistence.* to jakarta.persistence.*",
                "javax.persistence.* → jakarta.persistence.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Bean Validation migration.
     */
    public static Recipe beanValidationRecipe() {
        return new Recipe(
                "MigrateBeanValidation",
                "Converts javax.validation.* to jakarta.validation.*",
                "javax.validation.* → jakarta.validation.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Servlet migration.
     */
    public static Recipe servletRecipe() {
        return new Recipe(
                "MigrateServlet",
                "Converts javax.servlet.* to jakarta.servlet.*",
                "javax.servlet.* → jakarta.servlet.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for CDI migration.
     */
    public static Recipe cdiRecipe() {
        return new Recipe(
                "MigrateCDI",
                "Converts javax.enterprise.* and javax.inject.* to jakarta equivalents",
                "javax.enterprise.* → jakarta.enterprise.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JAX-RS (REST) migration.
     */
    public static Recipe restRecipe() {
        return new Recipe(
                "MigrateREST",
                "Converts javax.ws.rs.* to jakarta.ws.rs.*",
                "javax.ws.rs.* → jakarta.ws.rs.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JAX-WS (SOAP) migration.
     */
    public static Recipe soapRecipe() {
        return new Recipe(
                "MigrateSOAP",
                "Converts javax.xml.ws.* to jakarta.xml.ws.*",
                "javax.xml.ws.* → jakarta.xml.ws.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JMS migration.
     */
    public static Recipe jmsRecipe() {
        return new Recipe(
                "MigrateJMS",
                "Converts javax.jms.* to jakarta.jms.*",
                "javax.jms.* → jakarta.jms.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Batch migration.
     */
    public static Recipe batchRecipe() {
        return new Recipe(
                "MigrateBatch",
                "Converts javax.batch.* to jakarta.batch.*",
                "javax.batch.* → jakarta.batch.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Mail migration.
     */
    public static Recipe mailRecipe() {
        return new Recipe(
                "MigrateMail",
                "Converts javax.mail.* to jakarta.mail.*",
                "javax.mail.* → jakarta.mail.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JTA migration.
     */
    public static Recipe jtaRecipe() {
        return new Recipe(
                "MigrateJTA",
                "Converts javax.transaction.* to jakarta.transaction.*",
                "javax.transaction.* → jakarta.transaction.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for EJB migration.
     */
    public static Recipe ejbRecipe() {
        return new Recipe(
                "MigrateEJB",
                "Converts javax.ejb.* to jakarta.ejb.*",
                "javax.ejb.* → jakarta.ejb.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JSF migration.
     */
    public static Recipe jsfRecipe() {
        return new Recipe(
                "MigrateJSF",
                "Converts javax.faces.* to jakarta.faces.*",
                "javax.faces.* → jakarta.faces.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for WebSocket migration.
     */
    public static Recipe websocketRecipe() {
        return new Recipe(
                "MigrateWebSocket",
                "Converts javax.websocket.* to jakarta.websocket.*",
                "javax.websocket.* → jakarta.websocket.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JSON-P migration.
     */
    public static Recipe jsonpRecipe() {
        return new Recipe(
                "MigrateJSONP",
                "Converts javax.json.* to jakarta.json.*",
                "javax.json.* → jakarta.json.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JSON-B migration.
     */
    public static Recipe jsonbRecipe() {
        return new Recipe(
                "MigrateJSONB",
                "Converts javax.json.bind.* to jakarta.json.bind.*",
                "javax.json.bind.* → jakarta.json.bind.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Security API migration.
     */
    public static Recipe securityRecipe() {
        return new Recipe(
                "MigrateSecurity",
                "Converts javax.security.enterprise.* to jakarta.security.enterprise.*",
                "javax.security.enterprise.* → jakarta.security.enterprise.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Concurrency migration.
     */
    public static Recipe concurrencyRecipe() {
        return new Recipe(
                "MigrateConcurrency",
                "Converts javax.enterprise.concurrent.* to jakarta.enterprise.concurrent.*",
                "javax.enterprise.concurrent.* → jakarta.enterprise.concurrent.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JCA migration.
     */
    public static Recipe jcaRecipe() {
        return new Recipe(
                "MigrateJCA",
                "Converts javax.resource.* to jakarta.resource.*",
                "javax.resource.* → jakarta.resource.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JAXB migration.
     */
    public static Recipe jaxbRecipe() {
        return new Recipe(
                "MigrateJAXB",
                "Converts javax.xml.bind.* to jakarta.xml.bind.*",
                "javax.xml.bind.* → jakarta.xml.bind.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JAX-RPC migration.
     */
    public static Recipe jaxrpcRecipe() {
        return new Recipe(
                "MigrateJAXRPC",
                "Converts javax.xml.rpc.* to jakarta.xml.rpc.*",
                "javax.xml.rpc.* → jakarta.xml.rpc.*",
                SafetyLevel.MEDIUM,
                true);
    }

    /**
     * Creates a recipe for JASPIC migration.
     */
    public static Recipe jaspicRecipe() {
        return new Recipe(
                "MigrateJASPIC",
                "Converts javax.security.auth.message.* to jakarta.security.auth.message.*",
                "javax.security.auth.message.* → jakarta.security.auth.message.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Annotation migration.
     */
    public static Recipe annotationRecipe() {
        return new Recipe(
                "MigrateAnnotation",
                "Converts javax.annotation.* to jakarta.annotation.*",
                "javax.annotation.* → jakarta.annotation.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Activation migration.
     */
    public static Recipe activationRecipe() {
        return new Recipe(
                "MigrateActivation",
                "Converts javax.activation.* to jakarta.activation.*",
                "javax.activation.* → jakarta.activation.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for EL migration.
     */
    public static Recipe elRecipe() {
        return new Recipe(
                "MigrateEL",
                "Converts javax.el.* to jakarta.el.*",
                "javax.el.* → jakarta.el.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Interceptor migration.
     */
    public static Recipe interceptorRecipe() {
        return new Recipe(
                "MigrateInterceptor",
                "Converts javax.interceptor.* to jakarta.interceptor.*",
                "javax.interceptor.* → jakarta.interceptor.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Resource migration.
     */
    public static Recipe resourceRecipe() {
        return new Recipe(
                "MigrateResource",
                "Converts @Resource javax.annotation to jakarta.annotation",
                "@Resource → jakarta.annotation.Resource",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Returns a list of all standard Jakarta migration recipes.
     */
    public static java.util.List<Recipe> allRecipes() {
        return java.util.List.of(
                jakartaNamespaceRecipe(),
                persistenceXmlRecipe(),
                webXmlRecipe(),
                jpaRecipe(),
                beanValidationRecipe(),
                servletRecipe(),
                cdiRecipe(),
                restRecipe(),
                soapRecipe(),
                jmsRecipe(),
                batchRecipe(),
                mailRecipe(),
                jtaRecipe(),
                ejbRecipe(),
                jsfRecipe(),
                websocketRecipe(),
                jsonpRecipe(),
                jsonbRecipe(),
                securityRecipe(),
                concurrencyRecipe(),
                jcaRecipe(),
                jaxbRecipe(),
                jaxrpcRecipe(),
                jaspicRecipe(),
                annotationRecipe(),
                activationRecipe(),
                elRecipe(),
                interceptorRecipe(),
                resourceRecipe());
    }
    
    /**
     * Creates a recipe for migrating Servlet API from javax to jakarta.
     */
    public static Recipe servletApiRecipe() {
        return new Recipe(
            "MigrateServletApi",
            "Migrates javax.servlet.* packages to jakarta.servlet.*",
            "javax.servlet → jakarta.servlet",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JPA from javax to jakarta.
     */
    public static Recipe jpaRecipe() {
        return new Recipe(
            "MigrateJpa",
            "Migrates javax.persistence.* packages to jakarta.persistence.*",
            "javax.persistence → jakarta.persistence",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating CDI from javax to jakarta.
     */
    public static Recipe cdiRecipe() {
        return new Recipe(
            "MigrateCdi",
            "Migrates javax.inject.* and javax.enterprise.* packages to jakarta.*",
            "javax.inject/javax.enterprise → jakarta.inject/jakarta.enterprise",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JAXB from javax to jakarta.
     */
    public static Recipe jaxbRecipe() {
        return new Recipe(
            "MigrateJaxb",
            "Migrates javax.xml.bind.* packages to jakarta.xml.bind.*",
            "javax.xml.bind → jakarta.xml.bind",
            SafetyLevel.MEDIUM,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating Bean Validation from javax to jakarta.
     */
    public static Recipe validatorRecipe() {
        return new Recipe(
            "MigrateValidator",
            "Migrates javax.validation.* packages to jakarta.validation.*",
            "javax.validation → jakarta.validation",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating EJB from javax to jakarta.
     */
    public static Recipe ejbRecipe() {
        return new Recipe(
            "MigrateEjb",
            "Migrates javax.ejb.* packages to jakarta.ejb.*",
            "javax.ejb → jakarta.ejb",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JMS from javax to jakarta.
     */
    public static Recipe jmsRecipe() {
        return new Recipe(
            "MigrateJms",
            "Migrates javax.jms.* packages to jakarta.jms.*",
            "javax.jms → jakarta.jms",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JAX-RS from javax to jakarta.
     */
    public static Recipe jaxrsRecipe() {
        return new Recipe(
            "MigrateJaxrs",
            "Migrates javax.ws.rs.* packages to jakarta.ws.rs.*",
            "javax.ws.rs → jakarta.ws.rs",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JAX-WS from javax to jakarta.
     */
    public static Recipe jaxwsRecipe() {
        return new Recipe(
            "MigrateJaxws",
            "Migrates javax.xml.ws.* packages to jakarta.xml.ws.*",
            "javax.xml.ws → jakarta.xml.ws",
            SafetyLevel.MEDIUM,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JTA from javax to jakarta.
     */
    public static Recipe jtaRecipe() {
        return new Recipe(
            "MigrateJta",
            "Migrates javax.transaction.* packages to jakarta.transaction.*",
            "javax.transaction → jakarta.transaction",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JavaMail from javax to jakarta.
     */
    public static Recipe javaMailRecipe() {
        return new Recipe(
            "MigrateJavaMail",
            "Migrates javax.mail.* packages to jakarta.mail.*",
            "javax.mail → jakarta.mail",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating WebSocket from javax to jakarta.
     */
    public static Recipe websocketRecipe() {
        return new Recipe(
            "MigrateWebsocket",
            "Migrates javax.websocket.* packages to jakarta.websocket.*",
            "javax.websocket → jakarta.websocket",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JSON-B from javax to jakarta.
     */
    public static Recipe jsonbRecipe() {
        return new Recipe(
            "MigrateJsonb",
            "Migrates javax.json.bind.* packages to jakarta.json.bind.*",
            "javax.json.bind → jakarta.json.bind",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JSON-P from javax to jakarta.
     */
    public static Recipe jsonpRecipe() {
        return new Recipe(
            "MigrateJsonp",
            "Migrates javax.json.* packages to jakarta.json.*",
            "javax.json → jakarta.json",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating Activation API from javax to jakarta.
     */
    public static Recipe activationRecipe() {
        return new Recipe(
            "MigrateActivation",
            "Migrates javax.activation.* packages to jakarta.activation.*",
            "javax.activation → jakarta.activation",
            SafetyLevel.MEDIUM,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating SOAP from javax to jakarta.
     */
    public static Recipe soapRecipe() {
        return new Recipe(
            "MigrateSoap",
            "Migrates javax.xml.soap.* packages to jakarta.xml.soap.*",
            "javax.xml.soap → jakarta.xml.soap",
            SafetyLevel.MEDIUM,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating SAAJ from javax to jakarta.
     */
    public static Recipe saajRecipe() {
        return new Recipe(
            "MigrateSaaj",
            "Migrates javax.xml.soap.SAAJ to jakarta.xml.soap.SAAJ",
            "javax.xml.soap.SAAJ → jakarta.xml.soap.SAAJ",
            SafetyLevel.MEDIUM,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating Authorization from javax to jakarta.
     */
    public static Recipe authorizationRecipe() {
        return new Recipe(
            "MigrateAuthorization",
            "Migrates javax.security.* to jakarta.security.*",
            "javax.security → jakarta.security",
            SafetyLevel.MEDIUM,
            true
        );
    }
}
