package io.hops.hopsworks.api.admin.llap;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.admin.llap.LlapClusterFacade;
import io.hops.hopsworks.common.admin.llap.LlapClusterLifecycle;
import io.hops.hopsworks.common.admin.llap.LlapClusterStatus;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.exception.AppException;
import io.swagger.annotations.Api;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin/llap")
@RolesAllowed({"HOPS_ADMIN"})
@Api(value = "Admin")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LlapAdmin {

  @EJB
  private LlapClusterFacade llapClusterFacade;
  @EJB
  private LlapClusterLifecycle llapClusterLifecycle;
  @EJB
  private NoCacheResponse noCacheResponse;

  /**
   * Return the state of the llap cluster and other information
   * such as the appId and the hosts on which the cluster is running
   * @return
   * @throws AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response clusterStatus() throws AppException {
    LlapClusterStatus status = llapClusterFacade.getClusterStatus();
    GenericEntity<LlapClusterStatus> statusEntity =
        new GenericEntity<LlapClusterStatus>(status) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(statusEntity).build();
  }

  /**
   * Update the state of the cluster based on the ingested JSON
   * @param llapClusterRequest
   * @return
   * @throws AppException
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response changeClusterStatus(LlapClusterStatus llapClusterRequest) throws AppException {
    LlapClusterStatus oldClusterStatus = llapClusterFacade.getClusterStatus();
    LlapClusterStatus.Status desiredStatus = llapClusterRequest.getClusterStatus();

    switch (desiredStatus) {
      case UP:
        if (oldClusterStatus.getClusterStatus() == desiredStatus) {
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.LLAP_CLUSTER_ALREADY_UP);
        }
        llapClusterLifecycle.startCluster(llapClusterRequest.getInstanceNumber(),
            llapClusterRequest.getExecutorsMemory(),
            llapClusterRequest.getCacheMemory(),
            llapClusterRequest.getExecutorsPerInstance(),
            llapClusterRequest.getIOThreadsPerInstance());
        break;
      case DOWN:
        if (oldClusterStatus.getClusterStatus() == desiredStatus) {
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.LLAP_CLUSTER_ALREADY_DOWN);
        }
        llapClusterLifecycle.stopCluster();
        break;
      default:
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            ResponseMessages.LLAP_STATUS_INVALID);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).build();
  }
}
