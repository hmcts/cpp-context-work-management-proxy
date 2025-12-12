package uk.gov.moj.cpp.workmanagement.proxy.api.util;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskWithVariables;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

public class SortingUtils {

    public static final String HEARING_DATE = "hearingDate";
    public static final String URGENT = "isUrgent";

    private SortingUtils() {
    }

    public static Date getDueDate(final TaskWithVariables taskWithVariables) {
        return taskWithVariables.getTask().getDueDate();
    }

    public static Date getHearingDate(final TaskWithVariables taskWithVariables) {
        final Optional<VariableInstance> date = filterVariableInstanceByValue(taskWithVariables, HEARING_DATE);
        return date.map(variableInstance -> (Date) variableInstance.getValue()).orElse(null);
    }

    public static boolean isUrgent(final TaskWithVariables taskWithVariables) {
        final Optional<VariableInstance> urgent = filterVariableInstanceByValue(taskWithVariables, URGENT);
        return urgent.map(variableInstance -> {
            Object varVal = variableInstance.getValue();
            if (varVal instanceof Boolean) {
                return (Boolean) varVal;
            } else {
                return Boolean.parseBoolean((String) varVal);
            }
        }).orElse(false);
    }

    private static Optional<VariableInstance> filterVariableInstanceByValue(final TaskWithVariables taskWithVariables, final String filterValue) {
        return taskWithVariables.getVariableList().stream().filter(taskVariable -> taskVariable.getName().equals(filterValue)).findAny();
    }

    public static boolean isOverDue(final TaskWithVariables taskWithVariables) {
        final Date taskDueDate = taskWithVariables.getTask().getDueDate();
        if (nonNull(taskDueDate)) {
            final ZonedDateTime dueDate = taskDueDate.toInstant().atZone(ZoneId.systemDefault());
            if (dueDate.isBefore(ZonedDateTime.now(ZoneId.systemDefault()))) {
                return true;
            }
        }
        return false;
    }

    public static Date convertToDate(final Object value) {
        if (nonNull(value)) {
            if (value instanceof Date) {
                return (Date) value;
            } else if ((value instanceof String) && value.toString().length() >= 10) {
                final LocalDateTime localDateTime = convertToLocalDateTime(value);
                if (nonNull(localDateTime)) {
                    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                }
            }
        }
        return null;
    }

    public static LocalDate convertToLocalDate(final Object value) {
        if (nonNull(value)) {
            if (value instanceof Date) {
                return ((Date) (value)).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            } else if ((value instanceof String) && value.toString().length() >= 10) {
                if (value.toString().length() > 10) {
                    return ZonedDateTime.parse(value.toString(), DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
                } else {
                    return LocalDate.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
                }
            }
        }
        return null;
    }

    public static LocalDateTime convertToLocalDateTime(final Object value) {
        if (nonNull(value)) {
            if (value instanceof Date) {
                return ((Date) (value)).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            } else if ((value instanceof String) && value.toString().length() >= 10) {
                if (value.toString().length() > 10) {
                    return ZonedDateTime.parse(value.toString(), DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
                } else {
                    return LocalDate.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                }
            }
        }
        return null;
    }
}
