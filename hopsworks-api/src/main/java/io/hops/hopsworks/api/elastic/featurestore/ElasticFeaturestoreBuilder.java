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
package io.hops.hopsworks.api.elastic.featurestore;

import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.elastic.FeaturestoreDocType;
import io.hops.hopsworks.common.elastic.FeaturestoreElasticHit;
import io.hops.hopsworks.common.provenance.core.ProvXAttrs;
import io.hops.hopsworks.common.util.HopsworksJAXBContext;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static io.hops.hopsworks.common.provenance.core.ProvXAttrs.Featurestore;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ElasticFeaturestoreBuilder {
  @EJB
  private ElasticController elasticCtrl;
  @Inject
  private DataAccessController dataAccessCtrl;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HopsworksJAXBContext converter;
  
  public ElasticFeaturestoreDTO build(Users user, ElasticFeaturestoreRequest req, Integer projectId)
    throws ElasticException, ServiceException, GenericException {
    Project project = projectFacade.find(projectId);
    Map<FeaturestoreDocType, Set<Integer>> searchProjects
      = dataAccessCtrl.featurestoreSearchContext(project, req.getDocType());
    Map<FeaturestoreDocType, SearchResponse> response
      = elasticCtrl.featurestoreSearch(req.getTerm(), searchProjects, req.getFrom(), req.getSize());
    ElasticFeaturestoreDTO result = parseResult(response, accessFromSharedProjects(user));
    result.setFeaturegroupsFrom(req.getFrom());
    result.setTrainingdatasetsFrom(req.getFrom());
    result.setFeaturesFrom(req.getFrom());
    return result;
  }
  
  public ElasticFeaturestoreDTO build(Users user,ElasticFeaturestoreRequest req)
    throws ElasticException, ServiceException, GenericException {
    Map<FeaturestoreDocType, SearchResponse> response
      = elasticCtrl.featurestoreSearch(req.getDocType(), req.getTerm(), req.getFrom(), req.getSize());
    ElasticFeaturestoreDTO result = parseResult(response, accessFromSharedProjects(user));
    result.setFeaturegroupsFrom(req.getFrom());
    result.setTrainingdatasetsFrom(req.getFrom());
    result.setFeaturesFrom(req.getFrom());
    return result;
  }
  
  private ElasticFeaturestoreDTO parseResult(Map<FeaturestoreDocType, SearchResponse> resp,
    ProjectAccessCtrl accessCtrl)
    throws ElasticException, GenericException {
    ElasticFeaturestoreDTO result = new ElasticFeaturestoreDTO();
    for(Map.Entry<FeaturestoreDocType, SearchResponse> e : resp.entrySet()) {
      switch(e.getKey()) {
        case FEATUREGROUP: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            FeaturestoreElasticHit hit = FeaturestoreElasticHit.instance(hitAux);
            ElasticFeaturestoreItemDTO.Base item = ElasticFeaturestoreItemDTO.fromFeaturegroup(hit, converter);
            item.setHighlights(getHighlights(hitAux.getHighlightFields()));
            accessCtrl.accept(item, hit);
            result.addFeaturegroup(item);
          }
          result.setFeaturegroupsTotal(e.getValue().getHits().getTotalHits().value);
        } break;
        case TRAININGDATASET: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            FeaturestoreElasticHit hit = FeaturestoreElasticHit.instance(hitAux);
            ElasticFeaturestoreItemDTO.Base item = ElasticFeaturestoreItemDTO.fromTrainingDataset(hit, converter);
            item.setHighlights(getHighlights(hitAux.getHighlightFields()));
            accessCtrl.accept(item, hit);
            result.addTrainingdataset(item);
          }
          result.setTrainingdatasetsTotal(e.getValue().getHits().getTotalHits().value);
        } break;
        case FEATURE: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            FeaturestoreElasticHit hit = FeaturestoreElasticHit.instance(hitAux);
            ElasticFeaturestoreItemDTO.Base fgParent = ElasticFeaturestoreItemDTO.fromFeaturegroup(hit, converter);
            Map<String, HighlightField> highlightFields = hitAux.getHighlightFields();
            String featureField = Featurestore.getFeaturestoreElasticKey(Featurestore.FG_FEATURES);
            HighlightField hf = highlightFields.get(featureField + ".keyword");
            if (hf != null) {
              for (Text ee : hf.fragments()) {
                String feature = removeHighlightTags(ee.toString());
                ElasticFeaturestoreItemDTO.Feature item = ElasticFeaturestoreItemDTO.fromFeature(feature, fgParent);
                ElasticFeaturestoreItemDTO.Highlights highlights = new ElasticFeaturestoreItemDTO.Highlights();
                highlights.setName(ee.toString());
                item.setHighlights(highlights);
                accessCtrl.accept(item, hit);
                result.addFeature(item);
              }
            }
          }
          result.setFeaturesTotal(e.getValue().getHits().getTotalHits().value);
        } break;
      }
    }
    return result;
  }
  
  private ElasticFeaturestoreItemDTO.Highlights getHighlights(Map<String, HighlightField> map) {
    ElasticFeaturestoreItemDTO.Highlights highlights = new ElasticFeaturestoreItemDTO.Highlights();
    for(Map.Entry<String, HighlightField> e : map.entrySet()) {
      if(e.getKey().endsWith(".keyword")) {
        continue;
      }
      if(e.getKey().equals(Featurestore.NAME)
        || e.getKey().equals(Featurestore.getFeaturestoreElasticKey(Featurestore.NAME))) {
        highlights.setName(e.getValue().fragments()[0].toString());
        continue;
      }
      if(e.getKey().equals(Featurestore.getFeaturestoreElasticKey(Featurestore.DESCRIPTION))) {
        highlights.setDescription(e.getValue().fragments()[0].toString());
        continue;
      }
      if(e.getKey().equals(Featurestore.getFeaturestoreElasticKey(Featurestore.FG_FEATURES))) {
        for(Text t : e.getValue().fragments()) {
          highlights.addFeature(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(Featurestore.getFeaturestoreElasticKey(Featurestore.TD_FEATURES))) {
        for(Text t : e.getValue().fragments()) {
          highlights.addFeature(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(Featurestore.getTagsElasticKey())) {
        for(Text t : e.getValue().fragments()) {
          highlights.addTagKey(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(Featurestore.getTagsElasticValue())) {
        for(Text t : e.getValue().fragments()) {
          highlights.addTagValue(t.toString());
        }
        continue;
      }
      
      if(e.getKey().startsWith(ProvXAttrs.ELASTIC_XATTR + ".")) {
        for(Text t : e.getValue().fragments()) {
          highlights.addOtherXAttr(e.getKey(), t.toString());
        }
      }
    }
    return highlights;
  }
  
  private String removeHighlightTags(String field) {
    field = field.replace("<em>", "");
    field = field.replace("</em>", "");
    return field;
  }
  
  private ProjectAccessCtrl accessFromSharedProjects(Users user) {
    DataAccessController.ShortLivedCache cache = dataAccessCtrl.newCache();
    return (item, elasticHit) -> dataAccessCtrl.addAccessProjects(user, accessMapper(item, elasticHit), cache);
  }
  
  private interface ProjectAccessCtrl extends BiConsumer<ElasticFeaturestoreItemDTO.Base, FeaturestoreElasticHit> {
  }
  
  private DataAccessController.AccessI accessMapper(ElasticFeaturestoreItemDTO.Base item, FeaturestoreElasticHit hit) {
    return new DataAccessController.AccessI() {
  
      @Override
      public Integer getParentProjectId() {
        return hit.getProjectId();
      }
  
      @Override
      public Long getParentDatasetIId() {
        return hit.getDatasetIId();
      }
  
      @Override
      public void addAccessProject(Project project) {
        item.addAccessProject(project.getId(), project.getName());
      }
  
      @Override
      public String toString() {
        return hit.toString();
      }
    };
  }
}
