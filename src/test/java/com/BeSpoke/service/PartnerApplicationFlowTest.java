package com.BeSpoke.service;

import com.BeSpoke.dto.ApprovePartnerRequest;
import com.BeSpoke.dto.PartnerApplicationRequest;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.KycStatus;
import com.BeSpoke.entity.PartnerApplication;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.PartnerApplicationRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The partner front door: applying writes an application and nothing else, and only an
 * admin's approval turns it into a company, a director account and the welcome mail.
 */
class PartnerApplicationFlowTest {

    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final StaffProfileRepository profiles = mock(StaffProfileRepository.class);
    private final PartnerApplicationRepository applications =
            mock(PartnerApplicationRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditService audit = mock(AuditService.class);
    private final MailService mail = mock(MailService.class);

    private final CompanyService service = new CompanyService(companies, users, profiles,
            null, null, applications, encoder, audit, mail);

    private final User admin = new User("Platform Admin", "admin@bespoke.in", "hash",
            Role.SUPER_ADMIN);

    /** The whole form: director, business, city, email, phone. */
    private PartnerApplicationRequest form() {
        return new PartnerApplicationRequest("Asha Rao", "Studio Nine", "Pune",
                "Asha@StudioNine.in", "9876500000");
    }

    private PartnerApplication pending() {
        PartnerApplication application = new PartnerApplication();
        application.setCompanyName("Studio Nine");
        application.setCity("Pune");
        application.setContactName("Asha Rao");
        application.setContactEmail("asha@studionine.in");
        application.setContactPhone("9876500000");
        when(applications.findById(7L)).thenReturn(Optional.of(application));
        when(applications.save(any())).thenAnswer(call -> call.getArgument(0));
        return application;
    }

    @Test
    void applyingWritesTheApplicationAndAcknowledgesIt() {
        when(applications.save(any())).thenAnswer(call -> call.getArgument(0));
        service.applyAsPartner(form());

        ArgumentCaptor<PartnerApplication> saved =
                ArgumentCaptor.forClass(PartnerApplication.class);
        verify(applications).save(saved.capture());
        // Lower-cased, so it matches the account it will become on approval.
        assertEquals("asha@studionine.in", saved.getValue().getContactEmail());
        assertEquals(PartnerApplication.Status.PENDING, saved.getValue().getStatus());
        verify(companies, never()).save(any());
        verify(users, never()).save(any());
        verify(mail).partnerApplicationReceived(any());
        // And BeSpoke hears about it too — nobody sits refreshing the admin queue.
        verify(mail).partnerApplicationInternal(any());
    }

    @Test
    void anApplicantWhoAlreadyHasAnAccountIsTurnedAway() {
        when(users.existsByEmail("asha@studionine.in")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.applyAsPartner(form()));
        verify(applications, never()).save(any());
        verify(mail, never()).partnerApplicationReceived(any());
        verify(mail, never()).partnerApplicationInternal(any());
    }

    /** Caught at the form, not at approval — the admin must never inherit the clash. */
    @Test
    void aPhoneAlreadyInUseIsTurnedAwayAtTheForm() {
        when(users.existsByPhone("9876500000")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.applyAsPartner(form()));
        verify(applications, never()).save(any());
    }

    @Test
    void aNumberAlreadyInTheQueueIsTurnedAway() {
        when(applications.existsByContactPhoneAndStatus(
                "9876500000", PartnerApplication.Status.PENDING)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.applyAsPartner(form()));
        verify(applications, never()).save(any());
    }

    @Test
    void approvingOnboardsTheCompanyAndMailsTheCredentials() {
        PartnerApplication application = pending();
        when(companies.save(any())).thenAnswer(call -> call.getArgument(0));
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        when(encoder.encode("opensesame")).thenReturn("bcrypted");

        service.approveApplication(admin, 7L,
                new ApprovePartnerRequest("opensesame", "VENDOR", null,
                        List.of("Glass", "Electricals"), "27AAAAA0000A1Z5", "AAAAA0000A",
                        null, "Studio Nine Interiors LLP", "12 MG Road, Pune", null, true, null));

        ArgumentCaptor<Company> company = ArgumentCaptor.forClass(Company.class);
        verify(companies).save(company.capture());
        assertEquals("Pune", company.getValue().getHeadquartersCity());
        // The one city they gave us is also their first operational city.
        assertEquals("Pune", company.getValue().getOperationalCities().get(0));
        // The form never asked what they are — the admin's choice is what counts.
        assertEquals(CompanyType.VENDOR, company.getValue().getType());
        // The single email and phone from the form stand in as the company's.
        assertEquals("asha@studionine.in", company.getValue().getEmail());
        assertEquals("9876500000", company.getValue().getPhone());
        // Never VERIFIED on approval — the director still has to pass KYC.
        assertEquals(KycStatus.PENDING, company.getValue().getKycStatus());
        // A vendor is classified by what it actually supplies.
        assertEquals(List.of("Glass", "Electricals"), company.getValue().getVendorCategories());
        assertEquals("27AAAAA0000A1Z5", company.getValue().getGstin());
        // "GST address same as office" copies it across rather than leaving a null the
        // KYC check would then report as a missing registered address.
        assertEquals("12 MG Road, Pune", company.getValue().getOfficeAddress());
        assertEquals("12 MG Road, Pune", company.getValue().getRegisteredAddress());

        ArgumentCaptor<User> director = ArgumentCaptor.forClass(User.class);
        verify(users).save(director.capture());
        assertEquals(Role.DIRECTOR, director.getValue().getRole());
        assertEquals("asha@studionine.in", director.getValue().getEmail());
        assertEquals("bcrypted", director.getValue().getPasswordHash());

        verify(mail).partnerApproved(any(), any(), eq("opensesame"));
        assertEquals(PartnerApplication.Status.APPROVED, application.getStatus());
        assertNotNull(application.getDecidedAt());
        assertEquals(admin, application.getDecidedBy());
    }

    @Test
    void aDecidedApplicationCannotBeDecidedAgain() {
        PartnerApplication application = pending();
        application.setStatus(PartnerApplication.Status.REJECTED);
        assertThrows(BadRequestException.class, () -> service.approveApplication(
                admin, 7L, new ApprovePartnerRequest("opensesame", "DESIGN", null, null,
                        null, null, null, null, null, null, null, null)));
        verify(companies, never()).save(any());
        verify(mail, never()).partnerApproved(any(), any(), any());
    }
}
