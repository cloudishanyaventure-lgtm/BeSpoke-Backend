package com.BeSpoke.config;

import com.BeSpoke.security.AppUserDetailsService;
import com.BeSpoke.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Everyone with a workspace login: platform admins + all company staff roles. */
    private static final String[] STAFF_ROLES = {
            "SUPER_ADMIN", "ADMIN",
            "DIRECTOR", "ACCOUNT_MANAGER", "PRINCIPAL_ARCHITECT", "DESIGN_MANAGER", "DESIGNER",
            "PROJECT_MANAGER", "SALES_MANAGER", "CUSTOMER_CONSULTANT", "SALES_EXECUTIVE",
            "PRODUCT_MANAGER", "PRODUCT_SME"};

    /** Vendor workspace: vendor-company roles + platform. */
    private static final String[] VENDOR_ROLES = {
            "SUPER_ADMIN", "ADMIN",
            "DIRECTOR", "ACCOUNT_MANAGER", "SALES_MANAGER", "CUSTOMER_CONSULTANT",
            "PRODUCT_MANAGER", "PRODUCT_SME"};

    private final JwtAuthFilter jwtAuthFilter;
    private final AppUserDetailsService userDetailsService;
    private final String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          AppUserDetailsService userDetailsService,
                          @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: auth, enquiry form, catalog, marketing cards, shop, uploads, health.
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/enquiries").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/shop/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        // Customer portal + shop checkout.
                        .requestMatchers("/api/my/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                        // Vendor workspace (service layer 404s non-vendor companies).
                        .requestMatchers("/api/vendor/**").hasAnyRole(VENDOR_ROLES)
                        // Financials: invoices for finance roles, quotes also for the funnel roles.
                        .requestMatchers("/api/invoices/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "ACCOUNT_MANAGER")
                        .requestMatchers("/api/quotes/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "ACCOUNT_MANAGER",
                                "DESIGN_MANAGER", "SALES_MANAGER", "CUSTOMER_CONSULTANT")
                        // Team: directory readable by all staff, management for platform/director.
                        .requestMatchers(HttpMethod.GET, "/api/team").hasAnyRole(STAFF_ROLES)
                        .requestMatchers("/api/team/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        // Internal team chat: any staff role; the service requires a company.
                        .requestMatchers("/api/team-chat/**").hasAnyRole(STAFF_ROLES)
                        // Companies: own profile/org readable by staff, onboarding + KYC platform-side.
                        .requestMatchers(HttpMethod.GET, "/api/companies/mine").hasAnyRole(STAFF_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/companies/*/org").hasAnyRole(STAFF_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/companies/*/kyc-status")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/companies").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/companies").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers("/api/companies/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        // Platform org chart.
                        .requestMatchers("/api/hierarchy").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        // Staff workspace (company/role scoping enforced in the service layer;
                        // finer restrictions via @PreAuthorize).
                        .requestMatchers("/api/leads/**", "/api/projects/**", "/api/clients/**",
                                "/api/messages/**", "/api/dashboard", "/api/audit")
                        .hasAnyRole(STAFF_ROLES)
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Origin *patterns* so any localhost port works (Next dev may use 3000, 3001, …),
        // while still allowing credentials. Configure via app.cors.allowed-origins.
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new org.springframework.security.authentication.ProviderManager(authenticationProvider());
    }
}
