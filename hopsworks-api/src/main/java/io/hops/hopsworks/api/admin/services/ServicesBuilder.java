/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.api.admin.services;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ServicesBuilder {
  
  @EJB
  private HostServicesFacade hostServicesFacade;
  
  private ServiceDTO uri(ServiceDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getBaseUriBuilder().build());
    return dto;
  }
  
  private ServiceDTO uri(ServiceDTO dto, UriInfo uriInfo, HostServices service) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.SERVICES.toString())
      .path(Long.toString(service.getId()))
      .build());
    return dto;
  }
  
  private ServiceDTO expand(ServiceDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.SERVICES)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public ServiceDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest) {
    ServiceDTO dto = new ServiceDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = hostServicesFacade.findAll(resourceRequest.getOffset(),
        resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort());
      dto.setCount(collectionInfo.getCount());
      collectionInfo.getItems().forEach((service) -> dto.addItem(build(uriInfo, resourceRequest, (HostServices)
        service)));
    }
    return dto;
  }
  
  public ServiceDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, HostServices service) {
    ServiceDTO dto = new ServiceDTO();
    uri(dto, uriInfo, service);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(service.getId());
      dto.setHostId(service.getHost().getId());
      dto.setPid(service.getPid());
      dto.setGroup(service.getGroup());
      dto.setName(service.getName());
      dto.setStatus(service.getStatus());
      dto.setUptime(service.getUptime());
      dto.setStartTime(service.getStartTime());
      dto.setStopTime(service.getStopTime());
    }
    return dto;
  }
}
