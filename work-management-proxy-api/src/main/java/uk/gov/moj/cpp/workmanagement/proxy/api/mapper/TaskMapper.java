package uk.gov.moj.cpp.workmanagement.proxy.api.mapper;

import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.task.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskMapper {
    TaskMapper TASK_MAPPER = Mappers.getMapper(TaskMapper.class);
    uk.gov.moj.cpp.workmanagement.proxy.api.model.Task taskToTaskDto(Task task);

    @Mapping(source = "startTime", target = "createTime")
    uk.gov.moj.cpp.workmanagement.proxy.api.model.Task historicTaskInstanceToTaskDto(HistoricTaskInstance historicTaskInstance);
}
