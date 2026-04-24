package adrianmikula.jakartamigration.pdfreporting.snippet;

import lombok.extern.slf4j.Slf4j;

/**
 * Code examples snippet showing common migration patterns.
 * Provides before/after code samples for typical Jakarta EE migrations.
 */
@Slf4j
public class CodeExamplesSnippet extends BaseHtmlSnippet {
    
    @Override
    public String generate() throws SnippetGenerationException {
        return safelyFormat("""
            <div class="section">
                <h2>Common Migration Patterns</h2>
                <p>Detailed code examples showing how to migrate common Java EE patterns to Jakarta EE.</p>
                
                %s
                %s
                %s
                %s
                %s
                %s
            </div>
            """,
            generateImportExamples(),
            generateServletExamples(),
            generateJpaExamples(),
            generateEjbExamples(),
            generateCdiExamples(),
            generateValidationExamples()
        );
    }
    
    private String generateImportExamples() {
        return """
            <div class="code-example-section">
                <h3>📦 Package Import Migration</h3>
                <p>The most common migration task is updating package imports from javax.* to jakarta.*.</p>
                
                <div class="code-comparison">
                    <div class="before-code">
                        <h4>Before (Java EE)</h4>
                        <pre class="code-block"><code>import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.persistence.EntityManager;
import javax.persistence.Entity;
import javax.ejb.Stateless;
import javax.inject.Inject;</code></pre>
                    </div>
                    <div class="after-code">
                        <h4>After (Jakarta EE)</h4>
                        <pre class="code-block"><code>import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Entity;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;</code></pre>
                    </div>
                </div>
                
                <div class="migration-note">
                    <h4>📝 Migration Notes</h4>
                    <ul>
                        <li>Most packages follow the pattern: javax.* → jakarta.*</li>
                        <li>Some packages have slight name changes (e.g., javax.persistence → jakarta.persistence)</li>
                        <li>IDE search/replace functionality can automate most import updates</li>
                        <li>Verify all imports after automated changes</li>
                    </ul>
                </div>
            </div>
            """;
    }
    
    private String generateServletExamples() {
        return """
            <div class="code-example-section">
                <h3>🌐 Servlet API Migration</h3>
                <p>Servlet API changes are primarily package name updates with minor API improvements.</p>
                
                <div class="code-comparison">
                    <div class="before-code">
                        <h4>Before (Java EE Servlet)</h4>
                        <pre class="code-block"><code>@WebServlet("/api/users")
public class UserServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, 
                          HttpServletResponse response) 
            throws ServletException, IOException {
        
        String userId = request.getParameter("id");
        User user = userService.findById(userId);
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(user));
    }
    
    @Override
    protected void doPost(HttpServletRequest request, 
                           HttpServletResponse response) 
            throws ServletException, IOException {
        
        User user = gson.fromJson(request.getReader(), User.class);
        userService.save(user);
        
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.getWriter().write("User created successfully");
    }
}</code></pre>
                    </div>
                    <div class="after-code">
                        <h4>After (Jakarta EE Servlet)</h4>
                        <pre class="code-block"><code>@WebServlet("/api/users")
public class UserServlet extends HttpServlet {
    
    @Override
    protected void doGet(jakarta.servlet.http.HttpServletRequest request, 
                          jakarta.servlet.http.HttpServletResponse response) 
            throws jakarta.servlet.ServletException, java.io.IOException {
        
        String userId = request.getParameter("id");
        User user = userService.findById(userId);
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(user));
    }
    
    @Override
    protected void doPost(jakarta.servlet.http.HttpServletRequest request, 
                           jakarta.servlet.http.HttpServletResponse response) 
            throws jakarta.servlet.ServletException, java.io.IOException {
        
        User user = gson.fromJson(request.getReader(), User.class);
        userService.save(user);
        
        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_CREATED);
        response.getWriter().write("User created successfully");
    }
}</code></pre>
                    </div>
                </div>
                
                <div class="migration-note">
                    <h4>📝 Migration Notes</h4>
                    <ul>
                        <li>Update all javax.servlet imports to jakarta.servlet</li>
                        <li>ServletException moves to jakarta.servlet.ServletException</li>
                        <li>HTTP status constants remain the same</li>
                        <li>No changes to servlet lifecycle methods</li>
                    </ul>
                </div>
            </div>
            """;
    }
    
    private String generateJpaExamples() {
        return """
            <div class="code-example-section">
                <h3>🗄️ JPA/Hibernate Migration</h3>
                <p>JPA migration involves package updates and some annotation changes.</p>
                
                <div class="code-comparison">
                    <div class="before-code">
                        <h4>Before (Java EE JPA)</h4>
                        <pre class="code-block"><code>@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;
    
    @ManyToMany
    @JoinTable(name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;
}</code></pre>
                    </div>
                    <div class="after-code">
                        <h4>After (Jakarta EE JPA)</h4>
                        <pre class="code-block"><code>@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "users")
public class User {
    
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    
    @jakarta.persistence.Column(nullable = false, unique = true)
    private String email;
    
    @jakarta.persistence.Column(name = "created_at")
    @jakarta.persistence.Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @jakarta.persistence.OneToMany(mappedBy = "user", cascade = jakarta.persistence.CascadeType.ALL)
    private List<Order> orders;
    
    @jakarta.persistence.ManyToMany
    @jakarta.persistence.JoinTable(name = "user_roles",
               joinColumns = @jakarta.persistence.JoinColumn(name = "user_id"),
               inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "role_id"))
    private Set<Role> roles;
}</code></pre>
                    </div>
                </div>
                
                <div class="migration-note">
                    <h4>📝 Migration Notes</h4>
                    <ul>
                        <li>All javax.persistence annotations move to jakarta.persistence</li>
                        <li>Annotation names and attributes remain the same</li>
                        <li>Entity classes work identically after migration</li>
                        <li>Update persistence.xml configuration file</li>
                        <li>Test with your specific JPA provider (Hibernate, EclipseLink, etc.)</li>
                    </ul>
                </div>
            </div>
            """;
    }
    
    private String generateEjbExamples() {
        return """
            <div class="code-example-section">
                <h3>☕ EJB Migration</h3>
                <p>Enterprise Java Beans migration focuses on package name changes.</p>
                
                <div class="code-comparison">
                    <div class="before-code">
                        <h4>Before (Java EE EJB)</h4>
                        <pre class="code-block"><code>@Stateless
public class UserService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Inject
    private EmailService emailService;
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setName(userDTO.getName());
        
        entityManager.persist(user);
        
        emailService.sendWelcomeEmail(user);
        return user;
    }
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findById(Long id) {
        return entityManager.find(User.class, id);
    }
}</code></pre>
                    </div>
                    <div class="after-code">
                        <h4>After (Jakarta EE EJB)</h4>
                        <pre class="code-block"><code>@jakarta.ejb.Stateless
public class UserService {
    
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    
    @jakarta.inject.Inject
    private EmailService emailService;
    
    @jakarta.ejb.TransactionAttribute(jakarta.ejb.TransactionAttributeType.REQUIRED)
    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setName(userDTO.getName());
        
        entityManager.persist(user);
        
        emailService.sendWelcomeEmail(user);
        return user;
    }
    
    @jakarta.ejb.TransactionAttribute(jakarta.ejb.TransactionAttributeType.SUPPORTS)
    public User findById(Long id) {
        return entityManager.find(User.class, id);
    }
}</code></pre>
                    </div>
                </div>
                
                <div class="migration-note">
                    <h4>📝 Migration Notes</h4>
                    <ul>
                        <li>javax.ejb annotations move to jakarta.ejb</li>
                        <li>javax.persistence annotations move to jakarta.persistence</li>
                        <li>javax.inject annotations move to jakarta.inject</li>
                        <li>EJB behavior and lifecycle remain unchanged</li>
                        <li>Update ejb-jar.xml configuration if used</li>
                    </ul>
                </div>
            </div>
            """;
    }
    
    private String generateCdiExamples() {
        return """
            <div class="code-example-section">
                <h3>🔧 CDI Migration</h3>
                <p>Contexts and Dependency Injection migration involves package updates.</p>
                
                <div class="code-comparison">
                    <div class="before-code">
                        <h4>Before (Java EE CDI)</h4>
                        <pre class="code-block"><code>@ApplicationScoped
public class OrderService {
    
    @Inject
    private UserRepository userRepository;
    
    @Inject
    private PaymentService paymentService;
    
    @Inject
    private Event<OrderCreatedEvent> orderCreatedEvent;
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        
        order = userRepository.save(order);
        
        paymentService.processPayment(order);
        orderCreatedEvent.fire(new OrderCreatedEvent(order));
        
        return order;
    }
    
    @Produces
    @RequestScoped
    public OrderValidator createOrderValidator() {
        return new OrderValidator();
    }
}</code></pre>
                    </div>
                    <div class="after-code">
                        <h4>After (Jakarta EE CDI)</h4>
                        <pre class="code-block"><code>@jakarta.enterprise.context.ApplicationScoped
public class OrderService {
    
    @jakarta.inject.Inject
    private UserRepository userRepository;
    
    @jakarta.inject.Inject
    private PaymentService paymentService;
    
    @jakarta.inject.Inject
    private jakarta.enterprise.event.Event<OrderCreatedEvent> orderCreatedEvent;
    
    @jakarta.transaction.Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        
        order = userRepository.save(order);
        
        paymentService.processPayment(order);
        orderCreatedEvent.fire(new OrderCreatedEvent(order));
        
        return order;
    }
    
    @jakarta.enterprise.inject.Produces
    @jakarta.enterprise.context.RequestScoped
    public OrderValidator createOrderValidator() {
        return new OrderValidator();
    }
}</code></pre>
                    </div>
                </div>
                
                <div class="migration-note">
                    <h4>📝 Migration Notes</h4>
                    <ul>
                        <li>javax.enterprise.context annotations move to jakarta.enterprise.context</li>
                        <li>javax.inject annotations move to jakarta.inject</li>
                        <li>javax.enterprise.inject.Produces moves to jakarta.enterprise.inject.Produces</li>
                        <li>javax.enterprise.event.Event moves to jakarta.enterprise.event.Event</li>
                        <li>CDI behavior and scopes remain unchanged</li>
                    </ul>
                </div>
            </div>
            """;
    }
    
    private String generateValidationExamples() {
        return """
            <div class="code-example-section">
                <h3>✅ Bean Validation Migration</h3>
                <p>Bean validation migration involves package name updates.</p>
                
                <div class="code-comparison">
                    <div class="before-code">
                        <h4>Before (Java EE Validation)</h4>
                        <pre class="code-block"><code>public class UserRegistrationForm {
    
    @NotNull(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @NotNull(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotNull(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", 
              message = "Password must contain at least one letter and one number")
    private String password;
    
    @Min(value = 18, message = "You must be at least 18 years old")
    private Integer age;
}</code></pre>
                    </div>
                    <div class="after-code">
                        <h4>After (Jakarta EE Validation)</h4>
                        <pre class="code-block"><code>public class UserRegistrationForm {
    
    @jakarta.validation.constraints.NotNull(message = "Name is required")
    @jakarta.validation.constraints.Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @jakarta.validation.constraints.NotNull(message = "Email is required")
    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;
    
    @jakarta.validation.constraints.NotNull(message = "Password is required")
    @jakarta.validation.constraints.Size(min = 8, message = "Password must be at least 8 characters")
    @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", 
              message = "Password must contain at least one letter and one number")
    private String password;
    
    @jakarta.validation.constraints.Min(value = 18, message = "You must be at least 18 years old")
    private Integer age;
}</code></pre>
                    </div>
                </div>
                
                <div class="migration-note">
                    <h4>📝 Migration Notes</h4>
                    <ul>
                        <li>javax.validation.constraints annotations move to jakarta.validation.constraints</li>
                        <li>All validation annotations and attributes remain the same</li>
                        <li>Validation behavior is identical after migration</li>
                        <li>Update validation.xml configuration if used</li>
                        <li>Test validation messages and constraints</li>
                    </ul>
                </div>
            </div>
            """;
    }
    
    @Override
    public boolean isApplicable() {
        return true; // Always show code examples
    }
    
    @Override
    public int getOrder() {
        return 50; // Show after dependency analysis
    }
}
