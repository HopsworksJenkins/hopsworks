/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.kagent;

import io.hops.hopsworks.common.agent.AgentController;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.host.ServiceStatus;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.InvalidQueryException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.common.util.WebCommunication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.persistence.NonUniqueResultException;
import javax.ws.rs.core.Response;

@Stateless
public class HostServicesFacade extends AbstractFacade<HostServices> {

  @EJB
  private WebCommunication web;
  @EJB
  private HostsFacade hostEJB;

  private static final Logger LOGGER = Logger.getLogger(HostServicesFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public HostServicesFacade() {
    super(HostServices.class);
  }
  
  public HostServices find(String hostname, String group, String name) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.find", HostServices.class)
      .setParameter("hostname", hostname).setParameter("group", group)
      .setParameter("name", name);
    List results = query.getResultList();
    if (results.isEmpty()) {
      return null;
    } else if (results.size() == 1) {
      return (HostServices) results.get(0);
    }
    throw new NonUniqueResultException();
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public List<HostServices> findGroupServices(String group) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findBy-Group", HostServices.class)
      .setParameter("group", group);
    return query.getResultList();
  }

  public List<String> findGroups() {
    return em.createNamedQuery("HostServices.findGroups", String.class)
      .getResultList();
  }

  public List<HostServices> findHostServiceByHostname(String hostname) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findBy-Hostname", HostServices.class)
        .setParameter("hostname", hostname);
    return query.getResultList();
  }

  public List<HostServices> findServices(String name) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findBy-Service", HostServices.class)
        .setParameter("name", name);
    return query.getResultList();
  }

  public Long count(String group, String name) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.Count", Long.class)
      .setParameter("group", group)
      .setParameter("name", name);
    return query.getSingleResult();
  }

  public Long countServices(String group) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.Count-services", Long.class)
        .setParameter("group", group);
    return query.getSingleResult();
  }

  public void persist(HostServices hostService) {
    em.persist(hostService);
  }

  public void store(HostServices service) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.find", HostServices.class)
      .setParameter("hostname", service.getHost().getHostname())
      .setParameter("group", service.getGroup())
      .setParameter("name", service.getName());
    List<HostServices> s = query.getResultList();

    if (s.size() > 0) {
      service.setId(s.get(0).getId());
      em.merge(service);
    } else {
      em.persist(service);
    }
  }

  public void deleteServicesByHostname(String hostname) {
    em.createNamedQuery("HostServices.DeleteBy-Hostname")
      .setParameter("hostname", hostname)
      .executeUpdate();
  }

  public String groupOp(String group, Action action) throws GenericException {
    return webOp(action, findGroupServices(group));
  }

  public String serviceOp(String service, Action action) throws GenericException {
    return webOp(action, findServices(service));
  }

  public String serviceOnHostOp(String group, String serviceName, String hostname,
      Action action) throws GenericException {
    return webOp(action, Collections.singletonList(find(hostname, group, serviceName)));
  }

  private String webOp(Action operation, List<HostServices> services) throws GenericException {
    if (operation == null) {
      throw new IllegalArgumentException("The action is not valid, valid action are " + Arrays.toString(
              Action.values()));
    }
    if (services == null || services.isEmpty()) {
      throw new IllegalArgumentException("service was not provided.");
    }
    String result = "";
    boolean success = false;
    int exception = Response.Status.BAD_REQUEST.getStatusCode();
    for (HostServices service : services) {
      Hosts h = service.getHost();
      if (h != null) {
        String ip = h.getPublicOrPrivateIp();
        String agentPassword = h.getAgentPassword();
        try {
          result += service.toString() + " " + web.serviceOp(operation.value(), ip, agentPassword,
              service.getGroup(), service.getName());
          success = true;
        } catch (GenericException ex) {
          if (services.size() == 1) {
            throw ex;
          } else {
            exception = ex.getErrorCode().getRespStatus().getStatusCode();
            result += service.toString() + " " + ex.getErrorCode().getRespStatus() + " " + ex.getMessage();
          }
        }
      } else {
        result += service.toString() + " " + "host not found: " + service.getHost();
      }
      result += "\n";
    }
    if (!success) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE,
        "webOp error, exception: " + exception + ", " + "result: " + result);
    }
    return result;
  }

  public List<HostServices> updateHostServices(AgentController.AgentHeartbeatDTO heartbeat) throws ServiceException {
    Hosts host = hostEJB.findByHostname(heartbeat.getHostId());
    if (host == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_NOT_FOUND, Level.WARNING,
        "hostId: " + heartbeat.getHostId());
    }
    final List<HostServices> hostServices = new ArrayList<>(heartbeat.getServices().size());
    for (final AgentController.AgentServiceDTO service : heartbeat.getServices()) {
      final String name = service.getService();
      final String group = service.getGroup();
      HostServices hostService = null;
      try {
        hostService = find(heartbeat.getHostId(), group, name);
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Could not find service for " + heartbeat.getHostId() + "/" + group + "/" + name);
        continue;
      }
      
      if (hostService == null) {
        hostService = new HostServices();
        hostService.setHost(host);
        hostService.setGroup(group);
        hostService.setName(name);
        hostService.setStartTime(heartbeat.getAgentTime());
      }
  
      final Integer pid = service.getPid() != null ? service.getPid(): -1;
      hostService.setPid(pid);
      if (service.getStatus() != null) {
        if ((hostService.getStatus() == null || !hostService.getStatus().equals(ServiceStatus.STARTED))
            && service.getStatus().equals(ServiceStatus.STARTED)) {
          hostService.setStartTime(heartbeat.getAgentTime());
        }
        hostService.setStatus(service.getStatus());
      } else {
        hostService.setStatus(ServiceStatus.NONE);
      }
  
      if (service.getStatus().equals(ServiceStatus.STARTED)) {
        hostService.setStopTime(heartbeat.getAgentTime());
      }
      final Long startTime = hostService.getStartTime();
      final Long stopTime = hostService.getStopTime();
      if (startTime != null && stopTime != null) {
        hostService.setUptime(stopTime - startTime);
      } else {
        hostService.setUptime(0L);
      }
      
      store(hostService);
      hostServices.add(hostService);
    }
    return hostServices;
  }
  
  public CollectionInfo findAll(Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Set<? extends SortBy> sort) {
    String queryStr = buildQuery("SELECT DISTINCT h FROM HostServices h ", filter, sort, "");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT h.id) FROM HostServices h ", filter, sort, "");
    Query query = em.createQuery(queryStr, HostServices.class);
    Query queryCount = em.createQuery(queryCountStr, HostServices.class);
    setFilter(filter, query);
    setFilter(filter, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q);
    }
  }
  
  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case ID:
        q.setParameter(filterBy.getField(), getLongValue(filterBy.getField(), filterBy.getParam()));
        break;
      case HOST_ID:
      case PID:
        q.setParameter(filterBy.getField(), getIntValue(filterBy));
        break;
      case NAME:
      case GROUP_NAME:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      case STATUS:
        q.setParameter(filterBy.getField(), getStatusValue(filterBy.getField(), filterBy.getParam()));
        break;
      default:
        break;
    }
  }
  
  private ServiceStatus getStatusValue(String field, String value) {
    if (value == null || value.isEmpty()) {
      throw new InvalidQueryException("Filter value for " + field + " needs to set an Integer or a valid " + field
        + ", but found: " + value);
    }
    ServiceStatus val;
    try {
      int v = Integer.parseInt(value);
      val = ServiceStatus.fromValue(v);
    } catch (IllegalArgumentException e) {
      try {
        val = ServiceStatus.valueOf(value);
      } catch (IllegalArgumentException ie) {
        throw new InvalidQueryException("Filter value for " + field + " needs to set an Integer or a valid " + field
          + ", but found: " + value);
      }
    }
    return val;
  }
  
  public enum Sorts {
    ID("ID", "h.id", "ASC"),
    HOST_ID("HOST_ID", "h.host_id", "ASC"),
    PID("PID", "h.pid", "ASC"),
    NAME("NAME", "LOWER(h.name)", "ASC"),
    GROUP_NAME("GROUP_NAME", "LOWER(h.group_name)", "ASC"),
    STATUS("STATUS", "h.status", "ASC"),
    UPTIME("UPTIME", "h.uptime", "ASC"),
    START_TIME("START_TIME", "h.startTime", "ASC"),
    STOP_TIME("STOP_TIME", "h.stopTime", "ASC");
    
    private final String value;
    private final String sql;
    private final String defaultParam;
    
    private Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }
    
    public String getValue() {
      return value;
    }
    
    public String getSql() {
      return sql;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getJoin() {
      return null;
    }
    
    @Override
    public String toString() {
      return value;
    }
    
  }
  
  public enum Filters {
    ID("ID", "h.id = :id", "id", "0"),
    HOST_ID("HOST_ID", "h.host_id = :hostId", "host_id", "0"),
    PID("PID", "h.pid = :pid", "pid", "0"),
    NAME("NAME", "h.name = :name", "name", ""),
    GROUP_NAME("GROUP_NAME", "h.groupName = :groupName", "group_name", ""),
    STATUS("STATUS", "h.status = :status", "status", "0");
    
    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    private Filters(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }
    
    public String getValue() {
      return value;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getSql() {
      return sql;
    }
    
    public String getField() {
      return field;
    }
    
    @Override
    public String toString() {
      return value;
    }
    
  }
}
