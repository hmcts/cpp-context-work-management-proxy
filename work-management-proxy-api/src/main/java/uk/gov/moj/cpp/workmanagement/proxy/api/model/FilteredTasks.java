package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskWithVariables;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilteredTasks {
    private List<TaskWithVariables> taskWithVariables;
    private long totalTaskCount;
}
