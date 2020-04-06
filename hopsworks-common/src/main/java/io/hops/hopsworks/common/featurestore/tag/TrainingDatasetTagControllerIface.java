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

package io.hops.hopsworks.common.featurestore.tag;

import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;


public interface TrainingDatasetTagControllerIface{

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  default public Map<String, String> get(Project project, Users user, Featurestore featurestore, int trainingdatasetid)
      throws FeaturestoreException, DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  default public boolean createOrUpdate(Project project, Users user, Featurestore featurestore,
                                        int trainingdatasetid, String tagsJson)
      throws FeaturestoreException, DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  default public boolean delete(Project project, Users user, Featurestore featurestore, int trainingdatasetid)
      throws FeaturestoreException, DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

}
