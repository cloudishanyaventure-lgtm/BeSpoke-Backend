package com.BeSpoke.config;

import com.BeSpoke.security.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.boot.test.util.TestPropertyValues;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityResponseTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthFilter.class, Endpoints.class})
    static class Config {
        @Bean JwtService jwtService() { return mock(JwtService.class); }
        @Bean AppUserDetailsService userDetails() { return mock(AppUserDetailsService.class); }
        @Bean com.BeSpoke.repository.UserRepository users() { return mock(com.BeSpoke.repository.UserRepository.class); }
    }
    @RestController
    static class Endpoints {
        @GetMapping("/api/my/journey") String journey() { return "customer"; }
        @GetMapping("/api/public/options") String options() { return "public"; }
    }
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertyValues.of("app.cors.allowed-origins=http://localhost:*").applyTo(context);
        context.register(Config.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }
    @AfterEach void close() { context.close(); }
    @Test void missingAndExpiredTokensReturn401Json() throws Exception {
        mvc.perform(get("/api/my/journey")).andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"));
        when(context.getBean(JwtService.class).parse("expired")).thenThrow(new JwtException("expired"));
        mvc.perform(get("/api/my/journey").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized());
    }
    @Test void wrongRoleReturns403AndValidCustomerCanEnter() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("person@example.com");
        when(claims.get("role", String.class)).thenReturn("DESIGNER", "CUSTOMER");
        when(context.getBean(JwtService.class).parse("valid")).thenReturn(claims);
        var company = new com.BeSpoke.entity.Company("Studio", "studio"); company.setId(1L);
        var staff = new com.BeSpoke.entity.User("Staff", "person@example.com", "hash", com.BeSpoke.entity.Role.DESIGNER);
        staff.setCompany(company);
        var customer = new com.BeSpoke.entity.User("Customer", "person@example.com", "hash", com.BeSpoke.entity.Role.CUSTOMER);
        when(context.getBean(com.BeSpoke.repository.UserRepository.class).findByEmail("person@example.com"))
                .thenReturn(java.util.Optional.of(staff), java.util.Optional.of(customer));
        mvc.perform(get("/api/my/journey").header("Authorization", "Bearer valid"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/my/journey").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
    }
    @Test void expiredTokenDoesNotBreakPublicBrowsing() throws Exception {
        when(context.getBean(JwtService.class).parse("expired")).thenThrow(new JwtException("expired"));
        mvc.perform(get("/api/public/options").header("Authorization", "Bearer expired"))
                .andExpect(status().isOk());
    }
}
