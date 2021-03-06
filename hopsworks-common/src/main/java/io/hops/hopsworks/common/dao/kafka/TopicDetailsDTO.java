package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TopicDetailsDTO implements Serializable {

  private String name;

  private List<PartitionDetailsDTO> parrtitionDetails;

  public TopicDetailsDTO() {
  }

  public TopicDetailsDTO(String name) {
    this.name = name;
  }

  public TopicDetailsDTO(String topic,
          List<PartitionDetailsDTO> partitionDetails) {
    this.name = topic;
    this.parrtitionDetails = partitionDetails;
  }

  public String getName() {
    return name;
  }

  public List<PartitionDetailsDTO> getPartition() {
    return parrtitionDetails;
  }

  public void setName(String topic) {
    this.name = topic;
  }

  public void setPartition(List<PartitionDetailsDTO> partitionReplicas) {
    this.parrtitionDetails = partitionReplicas;
  }

}
