package uk.gov.moj.cpp.workmanagement.proxy.api.mapper;

import org.camunda.bpm.engine.task.DelegationState;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DelegationStateMapper {
    DelegationStateMapper INSTANCE = Mappers.getMapper(DelegationStateMapper.class);
    uk.gov.moj.cpp.workmanagement.proxy.api.model.DelegationState delegationStateToDelegationStateDto(DelegationState delegationState);
}
