package uk.gov.moj.cpp.workmanagement.proxy.api.mapper;

import java.util.List;

import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VariableInstanceMapper {

    VariableInstanceMapper VARIABLE_INSTANCE_MAPPER = Mappers.getMapper(VariableInstanceMapper.class);
    uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance variableInstanceToVariableInstanceDto(VariableInstance variableInstance);

    List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableInstancesToVariableInstancesDto(List<VariableInstance> variableInstances);

    uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance historicVariableInstanceToVariableInstanceDto(HistoricVariableInstance historicVariableInstance);

    List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> historicVariableInstancesToVariableInstancesDto(List<HistoricVariableInstance> historicVariableInstances);
}
