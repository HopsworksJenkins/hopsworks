package io.hops.hopsworks.api.hopssite;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.hopssite.dto.CategoryDTO;
import io.hops.hopsworks.api.hopssite.dto.DatasetIssueReqDTO;
import io.hops.hopsworks.api.hopssite.dto.HopsSiteServiceInfoDTO;
import io.hops.hopsworks.api.hopssite.dto.LocalDatasetDTO;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.common.UserDTO;
import io.hops.hopsworks.dela.dto.hopssite.DatasetDTO;
import io.hops.hopsworks.dela.dto.hopssite.HopsSiteDatasetDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.dela.old_hopssite_dto.DatasetIssueDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/hopssite/")
@Stateless
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Hopssite Service",
  description = "Hopssite Service")
public class HopssiteService {

  private final static Logger LOGGER = Logger.getLogger(HopssiteService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private HopssiteController hopsSite;
  @EJB
  private Settings settings;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private InodeFacade inodes;
  @Inject
  private CommentService commentService;
  @Inject
  private RatingService ratingService;

  @GET
  @Path("services/{service}")
  public Response getServiceInfo(@PathParam("service") String service) {
    boolean delaEnabled = settings.isDelaEnabled();
    HopsSiteServiceInfoDTO serviceInfo;
    if (delaEnabled) {
      serviceInfo = new HopsSiteServiceInfoDTO("Dela", 1, "Dela enabled.");
    } else {
      serviceInfo = new HopsSiteServiceInfoDTO("Dela", 0, "Dela disabled.");
    }

    LOGGER.log(Settings.DELA_DEBUG, "Get service info for service: {0}, {1}", new Object[]{service, serviceInfo});
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(serviceInfo).build();
  }

  @GET
  @Path("clusterId")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getClusterId() throws ThirdPartyException {
    String clusterId = settings.getDELA_CLUSTER_ID();
    LOGGER.log(Level.INFO, "Cluster id on hops-site: {0}", clusterId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(clusterId).build();
  }

  @GET
  @Path("userId")
  public Response getUserId(@Context SecurityContext sc) throws ThirdPartyException {
    String id = String.valueOf(hopsSite.getUserId(sc.getUserPrincipal().getName()));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(id).build();
  }
  
  public static enum CategoriesFilter {
    ALL,
    TOP_RATED,
    NEW
  }
  @GET
  @Path("datasets")
  public Response getAllDatasets(
    @ApiParam(required = true) @QueryParam("filter") CategoriesFilter filter) 
    throws ThirdPartyException {
    List<HopsSiteDatasetDTO> datasets;
    switch(filter) {
      case ALL : datasets = hopsSite.getAll(); break;
      case TOP_RATED : datasets = hopsSite.getAll(); break;
      case NEW : datasets = hopsSite.getAll(); break;
      default: throw new IllegalArgumentException("unknown filter:" + filter);
    }
    
    markLocalDatasets(datasets);
    GenericEntity<List<HopsSiteDatasetDTO>> datasetsJson = new GenericEntity<List<HopsSiteDatasetDTO>>(datasets) {
    };
    LOGGER.log(Settings.DELA_DEBUG, "Get all datasets");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasetsJson).build();
  }

  @GET
  @Path("datasets/{publicDSId}")
  public Response getDataset(@PathParam("publicDSId") String publicDSId) throws ThirdPartyException {
    DatasetDTO.Complete datasets = hopsSite.getDataset(publicDSId);
    LOGGER.log(Settings.DELA_DEBUG, "Get a dataset");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasets).build();
  }

  @GET
  @Path("datasets/{publicDSId}/local")
  public Response getLocalDataset(@PathParam("publicDSId") String publicDSId) {
    Optional<Dataset> datasets = datasetFacade.findByPublicDsId(publicDSId);
    if (!datasets.isPresent()) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.BAD_REQUEST).build();
    }
    Dataset ds = datasets.get();
    Inode parent = inodes.findParent(ds.getInode()); // to get the real parent project
    LocalDatasetDTO datasetDTO = new LocalDatasetDTO(ds.getInodeId(), ds.getName(), ds.getDescription(), parent.
            getInodePK().getName());
    LOGGER.log(Settings.DELA_DEBUG, "Get a local dataset by public id.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasetDTO).build();
  }

  @GET
  @Path("categories")
  public Response getDisplayCategories() {
    CategoryDTO categoryAll = new CategoryDTO(CategoriesFilter.ALL.name(), "All", false);
    CategoryDTO categoryNew = new CategoryDTO(CategoriesFilter.NEW.name(), "Recently added", false);
    CategoryDTO categoryTopRated = new CategoryDTO(CategoriesFilter.TOP_RATED.name(), "Top Rated", false);
    List<CategoryDTO> categories = Arrays.asList(categoryAll, categoryNew, categoryTopRated);
    GenericEntity<List<CategoryDTO>> categoriesEntity = new GenericEntity<List<CategoryDTO>>(categories) {
    };
    LOGGER.log(Settings.DELA_DEBUG, "Get all display categories.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(categoriesEntity).build();
  }

  @POST
  @Path("datasets/{publicDSId}/issue")
  public Response addDatasetIssue(@PathParam("publicDSId") String publicDSId, DatasetIssueReqDTO datasetIssueReq,
          @Context SecurityContext sc) throws ThirdPartyException {
    if (datasetIssueReq == null) {
      throw new IllegalArgumentException("Dataset issue not set.");
    }
    UserDTO.Complete user = hopsSite.getUser(sc.getUserPrincipal().getName());
    DatasetIssueDTO datasetIssue = new DatasetIssueDTO(publicDSId, user, datasetIssueReq.getType(),
            datasetIssueReq.getMsg());
    boolean added = hopsSite.addDatasetIssue(datasetIssue);
    if (added) {
      LOGGER.log(Settings.DELA_DEBUG, "Added issue for dataset {0}", publicDSId);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).build();
  }

  @Path("datasets/{publicDSId}/comments")
  public CommentService getComments(@PathParam("publicDSId") String publicDSId) {
    this.commentService.setPublicDSId(publicDSId);
    return this.commentService;
  }

  @Path("datasets/{publicDSId}/rating")
  public RatingService getRating(@PathParam("publicDSId") String publicDSId) {
    this.ratingService.setPublicDSId(publicDSId);
    return this.ratingService;
  }

  private void markLocalDatasets(List<HopsSiteDatasetDTO> datasets) {
    List<Dataset> publicDatasets = datasetFacade.findAllPublicDatasets();
    for (HopsSiteDatasetDTO publicDs : datasets) {
      for (Dataset localDs : publicDatasets) {
        if (publicDs.getPublicId().equals(localDs.getPublicDsId())) {
          publicDs.setLocalDataset(true);
          break;
        }
      }
    }
  }

}
