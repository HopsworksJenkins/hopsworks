/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.kafka;

import io.hops.hopsworks.common.dao.kafka.schemas.CompatibilityDto;
import io.hops.hopsworks.common.dao.kafka.schemas.CompatibilityLevelDto;
import io.hops.hopsworks.common.dao.kafka.schemas.SchemaCompatibility;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectsFacade;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectsCompatibility;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectsCompatibilityFacade;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectsCompatibilityPK;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SubjectsCompatibilityController {
  
  private final static Logger LOGGER = Logger.getLogger(SubjectsCompatibilityController.class.getName());
  
  @EJB
  private SubjectsCompatibilityFacade subjectsCompatibilityFacade;
  @EJB
  private SubjectsFacade subjectsFacade;
  
  public CompatibilityLevelDto getProjectCompatibilityLevel(Project project) throws SchemaException {
    SchemaCompatibility sc = subjectsCompatibilityFacade.getProjectCompatibility(project)
      .orElseThrow(() ->
        new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.FINE,
          "Project compatibility not found for project " + project.getName()))
      .getCompatibility();
    return new CompatibilityLevelDto(sc);
  }
  
  public CompatibilityDto getProjectCompatibility(Project project) throws SchemaException {
    SchemaCompatibility sc = subjectsCompatibilityFacade.getProjectCompatibility(project)
      .orElseThrow(() ->
        new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.FINE,
          "Project compatibility not found for project " + project.getName()))
      .getCompatibility();
    return new CompatibilityDto(sc);
  }
  
  public CompatibilityDto setProjectCompatibility(Project project, CompatibilityDto dto) throws
    SchemaException {
    if (dto == null || dto.getCompatibility() == null) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INVALID_COMPATIBILITY, Level.WARNING,
        "Compatibility cannot be null");
    }
    subjectsCompatibilityFacade.setProjectCompatibility(project, dto.getCompatibility());
    return getProjectCompatibility(project);
  }
  
  public CompatibilityDto setProjectCompatibility(Project project, SchemaCompatibility sc) throws
    SchemaException {
    if (sc == null) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INVALID_COMPATIBILITY, Level.WARNING,
        "Compatibility cannot be null");
    }
    subjectsCompatibilityFacade.setProjectCompatibility(project, sc);
    return getProjectCompatibility(project);
  }
  
  public CompatibilityLevelDto getSubjectCompatibility(Project project, String subject) throws SchemaException {
    if (subject == null) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.WARNING,
        "Subject cannot be null");
    }
    
    Optional<SubjectsCompatibility> sc = Optional.ofNullable(
      subjectsCompatibilityFacade.find(new SubjectsCompatibilityPK(subject, project.getId())));
    
    if (!sc.isPresent()) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.WARNING,
        "subject=" + subject);
    }
    
    return new CompatibilityLevelDto(sc.get().getCompatibility());
  }
  
  public CompatibilityDto setSubjectCompatibility(Project project, String subject, CompatibilityDto dto)
    throws SchemaException {
    return setSubjectCompatibility(project, subject, dto.getCompatibility());
  }
  
  public CompatibilityDto setSubjectCompatibility(Project project, String subject, SchemaCompatibility sc)
    throws SchemaException {
    if (sc == null) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INVALID_COMPATIBILITY, Level.WARNING,
        "Compatibility cannot be null");
    }
  
    if (subject == null || subject.equals(Settings.PROJECT_COMPATIBILITY_SUBJECT) ||
      subjectsFacade.findSubjectByName(project, subject).isEmpty()) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.WARNING,
        "Incorrect subject");
    }
  
    subjectsCompatibilityFacade.updateSubjectCompatibility(project, subject, sc);
    CompatibilityLevelDto levelDto = getSubjectCompatibility(project, subject);
    return new CompatibilityDto(levelDto.getCompatibilityLevel());
  }
}
