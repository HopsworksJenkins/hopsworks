package io.hops.kafka;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import kafka.admin.AdminUtils;
import kafka.common.TopicAlreadyMarkedForDeletionException;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.util.Settings;

@Singleton
public class ZookeeprTopicCleanerTimer {

    private final static Logger LOGGER = Logger.getLogger(KafkaFacade.class.
            getName());

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @EJB
    Settings settings;

    @EJB
    KafkaFacade kafkaFacade;

    @Schedule(persistent = false, second = "*/40", minute = "*", hour = "*")
    public void execute(Timer timer) {

//        Set<String> zkTopics = new HashSet<>();
//
//        try {
//            zkTopics = kafkaFacade.getTopicList();
//            if (zkTopics != null) {
//                LOGGER.log(Level.SEVERE, "Retrieved topics list from Zookeeper. {0}", new Object[]{zkTopics.toString()});
//            }
//        } catch (Exception e) {
//            LOGGER.log(Level.SEVERE, "Cannot retrieve topic list from Zookeeper");
//        }
//
//        List<ProjectTopics> dbProjectTopics = em.createNamedQuery(
//                "ProjectTopics.findAll").getResultList();
//
//        Set<String> dbTopics = new HashSet<>();
//
//        for (ProjectTopics pt : dbProjectTopics) {
//            try {
//                dbTopics.add(pt.getProjectTopicsPK().getTopicName());
//            } catch (UnsupportedOperationException e) {
//                LOGGER.log(Level.SEVERE, e.toString());
//            }
//        }
//
////        Set<String> zkTopicsTemp = zkTopics;
////        zkTopics.removeAll(dbTopics);
////
////        //remove topics from database which do not exist in zookeeper
////        if (!zkTopics.isEmpty()) {
////            for (String topicName : zkTopics) {
////                ProjectTopics removeTopic = em.createNamedQuery(
////                        "ProjectTopics.findByTopicName", ProjectTopics.class)
////                        .setParameter("topicName", topicName).getSingleResult();
////                em.remove(removeTopic);
////                LOGGER.log(Level.SEVERE, "************************** "
////                        + "{0} is being removed from database", new Object[]{topicName});
////            }
////        }
//
//        /*
//        To remove topics from zookeeper which do not exist in database 
//        1. get all topics from zookeeper
//        2. get the topics which exist in zookeeper, but not in database
//            zkTopics.removeAll(dbTopics);
//        3. remove those topics
//         */
//        zkTopics.removeAll(dbTopics);
//
//        if (!zkTopics.isEmpty()) {
//            for (String topicName : zkTopics) {
//                ZkClient zkClient = null;
//                try {
//                    zkClient = new ZkClient(kafkaFacade.getIp(settings.getZkIp()).getHostName(),
//                            10 * 1000, 29 * 1000, ZKStringSerializer$.MODULE$);
//                } catch (AppException ex) {
//                    LOGGER.log(Level.SEVERE, "Unable to get zookeeper ip address ");
//                }
//                ZkConnection zkConnection = new ZkConnection(settings.getZkIp());
//                ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);
//
//                try {
//                    AdminUtils.deleteTopic(zkUtils, topicName);
//                    LOGGER.log(Level.SEVERE, "************************** "
//                            + "{0} is being removed from Zookeeper", new Object[]{topicName});
//                } catch (TopicAlreadyMarkedForDeletionException ex) {
//                    LOGGER.log(Level.SEVERE, "************************** "
//                            + "{0} is already marked for deletion", new Object[]{topicName});
//                } finally {
//                    if (zkClient != null) {
//                        zkClient.close();
//                    }
//                }
//            }
//        }
    }
}