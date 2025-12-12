package uk.gov.moj.cpp.workmanagement.proxy.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.workmanagement.proxy.api.model.Task;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskWithVariables;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SortingUtilsTest {

    @Mock
    private TaskWithVariables taskWithVariables;

    @Mock
    private Task task;

    @Mock
    private VariableInstance variableInstance;

    @BeforeEach
    public void setUp() {
        when(taskWithVariables.getTask()).thenReturn(task);
    }

    @Test
    public void testGetDueDate() {
        Date dueDate = new Date();
        when(task.getDueDate()).thenReturn(dueDate);

        Date result = SortingUtils.getDueDate(taskWithVariables);
        assertEquals(dueDate, result);
    }

    @Test
    public void testGetHearingDate() {
        Date hearingDate = new Date();
        when(variableInstance.getName()).thenReturn(SortingUtils.HEARING_DATE);
        when(variableInstance.getValue()).thenReturn(hearingDate);
        when(taskWithVariables.getVariableList()).thenReturn(Arrays.asList(variableInstance));

        Date result = SortingUtils.getHearingDate(taskWithVariables);
        assertEquals(hearingDate, result);
    }

    @Test
    public void testGetHearingDateNotFound() {
        when(taskWithVariables.getVariableList()).thenReturn(Collections.emptyList());

        Date result = SortingUtils.getHearingDate(taskWithVariables);
        assertNull(result);
    }

    @Test
    public void testIsUrgent() {
        when(variableInstance.getName()).thenReturn(SortingUtils.URGENT);
        when(variableInstance.getValue()).thenReturn(true);
        when(taskWithVariables.getVariableList()).thenReturn(Arrays.asList(variableInstance));

        boolean result = SortingUtils.isUrgent(taskWithVariables);
        assertTrue(result);
    }

    @Test
    public void testIsUrgentWithString() {
        when(variableInstance.getName()).thenReturn(SortingUtils.URGENT);
        when(variableInstance.getValue()).thenReturn("true");
        when(taskWithVariables.getVariableList()).thenReturn(Arrays.asList(variableInstance));

        boolean result = SortingUtils.isUrgent(taskWithVariables);
        assertTrue(result);
    }

    @Test
    public void testIsUrgentNotFound() {
        when(taskWithVariables.getVariableList()).thenReturn(Collections.emptyList());

        boolean result = SortingUtils.isUrgent(taskWithVariables);
        assertFalse(result);
    }

    @Test
    public void testIsOverDue() {
        Date pastDueDate = Date.from(ZonedDateTime.now(ZoneId.systemDefault()).minusDays(1).toInstant());
        when(task.getDueDate()).thenReturn(pastDueDate);

        boolean result = SortingUtils.isOverDue(taskWithVariables);
        assertTrue(result);
    }

    @Test
    public void testIsNotOverDue() {
        Date futureDueDate = Date.from(ZonedDateTime.now(ZoneId.systemDefault()).plusDays(1).toInstant());
        when(task.getDueDate()).thenReturn(futureDueDate);

        boolean result = SortingUtils.isOverDue(taskWithVariables);
        assertFalse(result);
    }

    @Test
    public void testIsOverDueNoDueDate() {
        when(task.getDueDate()).thenReturn(null);

        boolean result = SortingUtils.isOverDue(taskWithVariables);
        assertFalse(result);
    }

    @Test
    public void testConvertToDateFromDate() {
        Date date = new Date();
        Date result = SortingUtils.convertToDate(date);
        assertEquals(date, result);
    }

    @Test
    public void testConvertToDateFromString() {
        String dateString = "2024-05-17T10:00:00.000Z";
        Date result = SortingUtils.convertToDate(dateString);

        LocalDateTime expectedDateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        Date expectedDate = Date.from(expectedDateTime.atZone(ZoneId.systemDefault()).toInstant());

        assertEquals(expectedDate, result);
    }

    @Test
    public void testConvertToDateInvalidString() {
        String invalidDateString = "invalid";
        Date result = SortingUtils.convertToDate(invalidDateString);
        assertNull(result);
    }

    @Test
    public void testConvertToLocalDateFromDate() {
        Date date = new Date();
        LocalDate result = SortingUtils.convertToLocalDate(date);

        LocalDate expectedDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        assertEquals(expectedDate, result);
    }

    @Test
    public void testConvertToLocalDateFromString() {
        String dateString = "2024-05-17";
        LocalDate result = SortingUtils.convertToLocalDate(dateString);

        LocalDate expectedDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        assertEquals(expectedDate, result);
    }

    @Test
    public void testConvertToLocalDateTimeFromDate() {
        Date date = new Date();
        LocalDateTime result = SortingUtils.convertToLocalDateTime(date);

        LocalDateTime expectedDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        assertEquals(expectedDateTime, result);
    }

    @Test
    public void testConvertToLocalDateTimeFromString() {
        String dateTimeString = "2024-07-26T09:00:00.000Z";
        LocalDateTime result = SortingUtils.convertToLocalDateTime(dateTimeString);

        LocalDateTime expectedDateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expectedDateTime, result);
    }

    @Test
    public void testConvertToLocalDateInvalidString() {
        String invalidDateString = "invalid";
        LocalDate result = SortingUtils.convertToLocalDate(invalidDateString);
        assertNull(result);
    }

    @Test
    public void testConvertToLocalDateTimeInvalidString() {
        String invalidDateTimeString = "invalid";
        LocalDateTime result = SortingUtils.convertToLocalDateTime(invalidDateTimeString);
        assertNull(result);
    }

    @Test
    public void testConvertToLocalDateFromStringWithTime() {
        String dateTimeString = "2024-07-26T09:00:00.000Z";
        LocalDate result = SortingUtils.convertToLocalDate(dateTimeString);

        LocalDate expectedDate = LocalDate.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expectedDate, result);
    }

    @Test
    public void testConvertToLocalDateTimeFromStringWithDateOnly() {
        String dateString = "2024-05-17";
        LocalDateTime result = SortingUtils.convertToLocalDateTime(dateString);

        LocalDateTime expectedDateTime = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        assertEquals(expectedDateTime, result);
    }
}