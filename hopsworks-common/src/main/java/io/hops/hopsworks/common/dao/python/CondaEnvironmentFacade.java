/*
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
 */
package io.hops.hopsworks.common.dao.python;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class CondaEnvironmentFacade {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public CondaEnvironment findByTfAndPythonVersion(String pythonVersion, String tfVersion) {
    TypedQuery<CondaEnvironment> query = em.createNamedQuery("CondaEnvironment.findByTfAndPythonVersion",
        CondaEnvironment.class);
    query.setParameter("pythonVersion", pythonVersion);
    query.setParameter("tfVersion", tfVersion);
    CondaEnvironment condaEnv = null;
    try {
      condaEnv = query.getSingleResult();
    } catch (NoResultException ex) {
      condaEnv = new CondaEnvironment();
      condaEnv.setPythonVersion(pythonVersion);
      condaEnv.setTfVersion(tfVersion);
      em.flush();
    }
    return condaEnv;
  }
}