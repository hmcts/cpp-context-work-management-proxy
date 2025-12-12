package uk.gov.moj.cpp.workmanagement.proxy.api.mapper;

import java.util.List;

import org.camunda.bpm.engine.task.IdentityLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface IdentityLinkMapper {
    IdentityLinkMapper IDENTITY_LINK_MAPPER = Mappers.getMapper(IdentityLinkMapper.class);

    uk.gov.moj.cpp.workmanagement.proxy.api.model.IdentityLink identityLinkToIdentityLinkDto(IdentityLink identityLink);
    List<uk.gov.moj.cpp.workmanagement.proxy.api.model.IdentityLink> identityLinksToIdentityLinksDto(List<IdentityLink> identityLinks);

    @Mapping(source = "processDefinitionId", target = "processDefId")
    uk.gov.moj.cpp.workmanagement.proxy.api.model.IdentityLink historicIdentityLinkLogToIdentityLinkDto(org.camunda.bpm.engine.history.HistoricIdentityLinkLog identityLink);

    List<uk.gov.moj.cpp.workmanagement.proxy.api.model.IdentityLink> historicIdentityLinksLogToIdentityLinksDto(List<org.camunda.bpm.engine.history.HistoricIdentityLinkLog> identityLinks);


}
