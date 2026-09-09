package com.BeSpoke.service;

import com.BeSpoke.dto.CreateStaffTaskRequest;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.StaffTask;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.repository.StaffTaskRepository;
import com.BeSpoke.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Assigning work is a tenant boundary: the assignee id arrives from the browser and
 * nothing in the URL says whose team they are on. These cover the guard and the
 * notification, which is the only thing that makes a task different from a private note.
 */
class StaffTaskServiceTest {

    private final StaffTaskRepository tasks = mock(StaffTaskRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MailService mail = mock(MailService.class);

    private final StaffTaskService service = new StaffTaskService(tasks, users, mail,
            mock(com.BeSpoke.repository.TaskCommentRepository.class), mock(CryptoService.class), mock(NotificationService.class));

    private final Company studio = company(1L, "Studio Nine");
    private final Company rival = company(2L, "Another Studio");

    private final User director = staff(10L, "Asha Rao", Role.DIRECTOR, studio);
    private final User designer = staff(11L, "Ravi Kumar", Role.DESIGNER, studio);
    private final User outsider = staff(99L, "Someone Else", Role.DESIGNER, rival);

    private static Company company(Long id, String name) {
        Company company = new Company(name, name.toLowerCase().replace(' ', '-'));
        company.setId(id);
        return company;
    }

    private static User staff(Long id, String name, Role role, Company company) {
        User user = new User(name, name.replace(' ', '.') + "@example.in", "hash", role);
        user.setId(id);
        user.setCompany(company);
        return user;
    }

    private CreateStaffTaskRequest request(Long assigneeId) {
        return new CreateStaffTaskRequest("Call the Mehra site", "Before Friday",
                assigneeId, null);
    }

    @Test
    void taggingAColleagueSavesTheTaskAndMailsThem() {
        when(users.findById(11L)).thenReturn(Optional.of(designer));
        when(tasks.save(any())).thenAnswer(call -> call.getArgument(0));

        service.create(director, request(11L));

        ArgumentCaptor<StaffTask> saved = ArgumentCaptor.forClass(StaffTask.class);
        verify(tasks).save(saved.capture());
        assertEquals(designer, saved.getValue().getAssignee());
        assertEquals(director, saved.getValue().getCreatedBy());
        assertEquals(studio, saved.getValue().getCompany());
        assertEquals(StaffTask.Status.OPEN, saved.getValue().getStatus());
        verify(mail).taskAssigned(any());
    }

    /** The whole point of the guard: an id from another tenant must not be assignable. */
    @Test
    void somebodyFromAnotherCompanyCannotBeTagged() {
        when(users.findById(99L)).thenReturn(Optional.of(outsider));

        assertThrows(BadRequestException.class, () -> service.create(director, request(99L)));

        verify(tasks, never()).save(any());
        verify(mail, never()).taskAssigned(any());
    }

    @Test
    void aDeactivatedColleagueCannotBeTagged() {
        designer.setActive(false);
        when(users.findById(11L)).thenReturn(Optional.of(designer));

        assertThrows(BadRequestException.class, () -> service.create(director, request(11L)));

        verify(tasks, never()).save(any());
    }

    /** A note to self is legitimate; mailing yourself about it is noise. */
    @Test
    void assigningToYourselfSkipsTheNotification() {
        when(users.findById(10L)).thenReturn(Optional.of(director));
        when(tasks.save(any())).thenAnswer(call -> call.getArgument(0));

        service.create(director, request(10L));

        verify(tasks).save(any());
        verify(mail, never()).taskAssigned(any());
    }
}
