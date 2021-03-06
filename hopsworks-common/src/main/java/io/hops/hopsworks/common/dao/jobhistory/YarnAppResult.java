package io.hops.hopsworks.common.dao.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "yarn_app_result",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnAppResult.findAll",
          query = "SELECT y FROM YarnAppResult y"),
  @NamedQuery(name = "YarnAppResult.findById",
          query = "SELECT y FROM YarnAppResult y WHERE y.id = :id"),
  @NamedQuery(name = "YarnAppResult.findByUsername",
          query = "SELECT y FROM YarnAppResult y WHERE y.username = :username"),
  @NamedQuery(name = "YarnAppResult.findByStartTime",
          query = "SELECT y FROM YarnAppResult y WHERE y.startTime = :startTime"),
  @NamedQuery(name = "YarnAppResult.findByFinishTime",
          query
          = "SELECT y FROM YarnAppResult y WHERE y.finishTime = :finishTime"),
  @NamedQuery(name = "YarnAppResult.findByJobType",
          query = "SELECT y FROM YarnAppResult y WHERE y.jobType = :jobType"),
  @NamedQuery(name = "YarnAppResult.findBySeverity",
          query = "SELECT y FROM YarnAppResult y WHERE y.severity = :severity"),
  @NamedQuery(name = "YarnAppResult.findByScore",
          query = "SELECT y FROM YarnAppResult y WHERE y.score = :score"),
  @NamedQuery(name = "YarnAppResult.findByWorkflowDepth",
          query
          = "SELECT y FROM YarnAppResult y WHERE y.workflowDepth = :workflowDepth"),
  @NamedQuery(name = "YarnAppResult.findByFlowExecId",
          query
          = "SELECT y FROM YarnAppResult y WHERE y.flowExecId = :flowExecId"),
  @NamedQuery(name = "YarnAppResult.findByJobDefId",
          query = "SELECT y FROM YarnAppResult y WHERE y.jobDefId = :jobDefId"),
  @NamedQuery(name = "YarnAppResult.findByFlowDefId",
          query = "SELECT y FROM YarnAppResult y WHERE y.flowDefId = :flowDefId")})
public class YarnAppResult implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 50)
  @Column(name = "id")
  private String id;
  @Lob
  @Size(max = 65535)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 50)
  @Column(name = "username")
  private String username;
  @Lob
  @Size(max = 65535)
  @Column(name = "queue_name")
  private String queueName;
  @Basic(optional = false)
  @NotNull
  @Column(name = "start_time")
  private long startTime;
  @Basic(optional = false)
  @NotNull
  @Column(name = "finish_time")
  private long finishTime;
  @Lob
  @Size(max = 65535)
  @Column(name = "tracking_url")
  private String trackingUrl;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 50)
  @Column(name = "job_type")
  private String jobType;
  @Basic(optional = false)
  @NotNull
  @Column(name = "severity")
  private short severity;
  @Column(name = "score")
  private Integer score;
  @Column(name = "workflow_depth")
  private Short workflowDepth;
  @Lob
  @Size(max = 65535)
  @Column(name = "scheduler")
  private String scheduler;
  @Lob
  @Size(max = 65535)
  @Column(name = "job_name")
  private String jobName;
  @Lob
  @Size(max = 65535)
  @Column(name = "job_exec_id")
  private String jobExecId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "flow_exec_id")
  private String flowExecId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "job_def_id")
  private String jobDefId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "flow_def_id")
  private String flowDefId;
  @Lob
  @Size(max = 65535)
  @Column(name = "job_exec_url")
  private String jobExecUrl;
  @Lob
  @Size(max = 65535)
  @Column(name = "flow_exec_url")
  private String flowExecUrl;
  @Lob
  @Size(max = 65535)
  @Column(name = "job_def_url")
  private String jobDefUrl;
  @Lob
  @Size(max = 65535)
  @Column(name = "flow_def_url")
  private String flowDefUrl;

  public YarnAppResult() {
  }

  public YarnAppResult(String id) {
    this.id = id;
  }

  public YarnAppResult(String id, String username, long startTime,
          long finishTime, String jobType, short severity, String flowExecId,
          String jobDefId, String flowDefId) {
    this.id = id;
    this.username = username;
    this.startTime = startTime;
    this.finishTime = finishTime;
    this.jobType = jobType;
    this.severity = severity;
    this.flowExecId = flowExecId;
    this.jobDefId = jobDefId;
    this.flowDefId = flowDefId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getFinishTime() {
    return finishTime;
  }

  public void setFinishTime(long finishTime) {
    this.finishTime = finishTime;
  }

  public String getTrackingUrl() {
    return trackingUrl;
  }

  public void setTrackingUrl(String trackingUrl) {
    this.trackingUrl = trackingUrl;
  }

  public String getJobType() {
    return jobType;
  }

  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  public short getSeverity() {
    return severity;
  }

  public void setSeverity(short severity) {
    this.severity = severity;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public Short getWorkflowDepth() {
    return workflowDepth;
  }

  public void setWorkflowDepth(Short workflowDepth) {
    this.workflowDepth = workflowDepth;
  }

  public String getScheduler() {
    return scheduler;
  }

  public void setScheduler(String scheduler) {
    this.scheduler = scheduler;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getJobExecId() {
    return jobExecId;
  }

  public void setJobExecId(String jobExecId) {
    this.jobExecId = jobExecId;
  }

  public String getFlowExecId() {
    return flowExecId;
  }

  public void setFlowExecId(String flowExecId) {
    this.flowExecId = flowExecId;
  }

  public String getJobDefId() {
    return jobDefId;
  }

  public void setJobDefId(String jobDefId) {
    this.jobDefId = jobDefId;
  }

  public String getFlowDefId() {
    return flowDefId;
  }

  public void setFlowDefId(String flowDefId) {
    this.flowDefId = flowDefId;
  }

  public String getJobExecUrl() {
    return jobExecUrl;
  }

  public void setJobExecUrl(String jobExecUrl) {
    this.jobExecUrl = jobExecUrl;
  }

  public String getFlowExecUrl() {
    return flowExecUrl;
  }

  public void setFlowExecUrl(String flowExecUrl) {
    this.flowExecUrl = flowExecUrl;
  }

  public String getJobDefUrl() {
    return jobDefUrl;
  }

  public void setJobDefUrl(String jobDefUrl) {
    this.jobDefUrl = jobDefUrl;
  }

  public String getFlowDefUrl() {
    return flowDefUrl;
  }

  public void setFlowDefUrl(String flowDefUrl) {
    this.flowDefUrl = flowDefUrl;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnAppResult)) {
      return false;
    }
    YarnAppResult other = (YarnAppResult) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.jobhistory.YarnAppResult[ id=" + id + " ]";
  }

}
