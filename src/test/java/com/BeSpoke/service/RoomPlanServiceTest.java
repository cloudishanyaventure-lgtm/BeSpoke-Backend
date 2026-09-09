package com.BeSpoke.service;
import com.BeSpoke.entity.*;
import com.BeSpoke.dto.*;
import com.BeSpoke.exception.*;
import com.BeSpoke.repository.RoomPlanRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
class RoomPlanServiceTest {
    final RoomPlanRepository plans = mock(RoomPlanRepository.class);
    final RequirementService requirements = mock(RequirementService.class);
    final LeadService leads = mock(LeadService.class);
    final RoomPlanService service = new RoomPlanService(plans, requirements, leads);
    final User customer = new User("Customer", "customer@example.com", "hash", Role.CUSTOMER);
    RoomPlanRequest request(Long version) { return new RoomPlanRequest("Living room", 0, 1, 2, 4.8, 4.0, "open", true, version); }
    @Test void creationBindsToAuthenticatedCustomerAndTheirLead() {
        Lead lead = mock(Lead.class); when(lead.getId()).thenReturn(9L);
        when(requirements.myLead(customer)).thenReturn(lead);
        when(plans.saveAndFlush(any())).thenAnswer(c -> c.getArgument(0));
        service.save(customer, null, request(null));
        var captor = org.mockito.ArgumentCaptor.forClass(RoomPlan.class);
        verify(plans).saveAndFlush(captor.capture());
        assertSame(customer, captor.getValue().getOwner()); assertSame(lead, captor.getValue().getLead());
    }
    @Test void anotherCustomersRoomIsNotFound() {
        when(plans.findByIdAndOwner(4L, customer)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.save(customer, 4L, request(0L)));
        verify(plans, never()).saveAndFlush(any());
    }
    @Test void staleVersionDoesNotOverwriteRoom() {
        RoomPlan plan = new RoomPlan(); plan.setVersion(3L); plan.setName("Newest room");
        when(plans.findByIdAndOwner(4L, customer)).thenReturn(Optional.of(plan));
        assertThrows(ConflictException.class, () -> service.save(customer, 4L, request(2L)));
        assertEquals("Newest room", plan.getName()); verify(plans, never()).saveAndFlush(any());
    }
    @Test void staffCannotSaveCustomerRooms() {
        User staff = new User("Staff", "staff@example.com", "hash", Role.DESIGNER);
        assertThrows(ForbiddenException.class, () -> service.save(staff, null, request(null)));
    }
    @Test void staffReadsGoThroughLeadScopeBeforeQuerying() {
        User staff = new User("Staff", "staff@example.com", "hash", Role.DESIGNER);
        when(leads.scopedLead(staff, 7L)).thenThrow(new NotFoundException("Lead not found"));
        assertThrows(NotFoundException.class, () -> service.forLead(staff, 7L));
        verify(plans, never()).findByLeadOrderByUpdatedAtDesc(any());
    }
    @Test void invalidDimensionsAndMissingVersionsAreRejected() {
        RoomPlan plan = new RoomPlan(); plan.setVersion(0L);
        when(plans.findByIdAndOwner(4L, customer)).thenReturn(Optional.of(plan));
        assertThrows(ConflictException.class, () -> service.save(customer, 4L, request(null)));
        assertThrows(BadRequestException.class, () -> service.save(customer, 4L,
                new RoomPlanRequest("Room", 0, 0, 0, Double.NaN, 4.0, "open", true, 0L)));
    }
}
