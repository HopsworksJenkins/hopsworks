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
package io.hops.hopsworks.common.admin.services;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.ServiceStatus;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.common.util.WebCommunication;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HostServicesController {
  
  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private WebCommunication web;
  @EJB
  private HostsController hostsController;
  
  
  public HostServices find(Long serviceId) throws ServiceException {
    HostServices service = hostServicesFacade.find(serviceId);
    if (service == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.WARNING,
        "id: " + serviceId);
    }
    return service;
  }
  
  public HostServices findByName(String name, String hostname) throws ServiceException {
    Optional<HostServices> service = hostServicesFacade.findByServiceName(name, hostname);
    if (!service.isPresent()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.WARNING,
        "name: " + name);
    }
    return service.get();
  }
  
  public Response updateService(String hostname, String serviceName, ServiceStatus status) throws ServiceException,
    GenericException {
    if (status != ServiceStatus.STARTED && status != ServiceStatus.FAILED) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.OPERATION_NOT_SUPPORTED, Level.WARNING,
        "status: " + status);
    }
    String operation = status == ServiceStatus.STARTED ? "startService" : "stopService";
    Hosts host = hostsController.findByHostname(hostname);
    HostServices service = findByName(serviceName, hostname);
    String ip = host.getPublicOrPrivateIp();
    String agentPassword = host.getAgentPassword();
    String response = web.serviceOp(operation ,ip, agentPassword, service.getGroup(), service.getName());
    return Response.ok().entity(response).build();
  }
}
