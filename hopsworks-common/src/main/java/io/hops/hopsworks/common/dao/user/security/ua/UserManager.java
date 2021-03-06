package io.hops.hopsworks.common.dao.user.security.ua;

import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.ProjectGenericUserCerts;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.user.security.Address;
import io.hops.hopsworks.common.dao.user.security.Organization;
import io.hops.hopsworks.common.dao.user.security.PeopleGroup;
import io.hops.hopsworks.common.dao.user.security.PeopleGroupPK;
import io.hops.hopsworks.common.dao.user.security.Yubikey;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAudit;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAudit;
import io.hops.hopsworks.common.metadata.exception.ApplicationException;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.persistence.TransactionRequiredException;

@Stateless
public class UserManager {

  private static final Logger logger = Logger.getLogger(UserManager.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @EJB
  private CertsFacade userCertsFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  
  /**
   * Register a new group for user.
   *
   * @param uid
   * @param gidNumber
   * @return
   */
  public void registerGroup(Users uid, int gidNumber) {
    BbcGroup bbcGroup = em.find(BbcGroup.class, gidNumber);
    uid.getBbcGroupCollection().add(bbcGroup);
    em.merge(uid);
  }

  /**
   * Register an address for new mobile users.
   *
   * @param user
   * @return
   */
  public void registerAddress(Users user) {
    Address add = new Address();
    add.setUid(user);
    add.setAddress1("-");
    add.setAddress2("-");
    add.setAddress3("-");
    add.setState("-");
    add.setCity("-");
    add.setCountry("-");
    add.setPostalcode("-");
    em.persist(add);

  }

  public void increaseLockNum(int id, int val) {
    Users p = (Users) em.find(Users.class, id);
    if (p != null) {
      p.setFalseLogin(val);
      em.merge(p);
    }

  }

  public void setOnline(int id, int val) {
    Users p = (Users) em.find(Users.class, id);
    p.setIsonline(val);
    em.merge(p);

  }

  public void resetLock(int id) {
    Users p = (Users) em.find(Users.class, id);
    p.setFalseLogin(0);
    em.merge(p);

  }

  public void changeAccountStatus(int id, String note, PeopleAccountStatus status) {
    Users p = (Users) em.find(Users.class, id);
    if (p != null) {
      p.setNotes(note);
      p.setStatus(status);
      em.merge(p);
    }

  }

  public void resetKey(int id) {
    Users p = (Users) em.find(Users.class, id);
    p.setValidationKey(SecurityUtils.getRandomPassword(64));
    em.merge(p);

  }

  public void resetPassword(Users p, String pass) throws Exception {
    //For every project, change the certificate secret in the database
    //Get cert password by decrypting it with old password

    List<Project> projects = projectFacade.findAllMemberStudies(p);
    //In case of failure, keep a list of old certs 
    List<UserCerts> oldCerts = userCertsFacade.findUserCertsByUid(p.getUsername());
    List<ProjectGenericUserCerts> pguCerts = null;
    try {
      for (Project project : projects) {
        UserCerts userCert = userCertsFacade.findUserCert(project.getName(), p.getUsername());
        String masterEncryptionPassword = certificatesMgmService.getMasterEncryptionPassword();
        String certPassword = HopsUtils.decrypt(p.getPassword(), userCert.getUserKeyPwd(), masterEncryptionPassword);
        //Encrypt it with new password and store it in the db
        String newSecret = HopsUtils.encrypt(pass, certPassword, masterEncryptionPassword);
        userCert.setUserKeyPwd(newSecret);
        userCertsFacade.persist(userCert);

        //If user is owner of the project, update projectgenericuser certs as well
        if (project.getOwner().equals(p)) {
          if (pguCerts == null) {
            pguCerts = new ArrayList<>();
          }
          ProjectGenericUserCerts pguCert = userCertsFacade.findProjectGenericUserCerts(project.getName()
              + Settings.PROJECT_GENERIC_USER_SUFFIX);
          pguCerts.add(userCertsFacade.findProjectGenericUserCerts(project.getName()
              + Settings.PROJECT_GENERIC_USER_SUFFIX));
          String pguCertPassword = HopsUtils.decrypt(p.getPassword(), pguCert.getCertificatePassword(),
              masterEncryptionPassword);
          //Encrypt it with new password and store it in the db
          String newPguSecret = HopsUtils.encrypt(pass, pguCertPassword, masterEncryptionPassword);
          pguCert.setCertificatePassword(newPguSecret);
          userCertsFacade.persistPGUCert(pguCert);
        }
      }
      p.setPassword(pass);
      p.setPasswordChanged(new Timestamp(new Date().getTime()));
      em.merge(p);
    } catch (Exception ex) {
      Logger.getLogger(UserManager.class.getName()).log(Level.SEVERE, null, ex);
      //Persist old certs
      for (UserCerts oldCert : oldCerts) {
        userCertsFacade.persist(oldCert);
      }
      if (pguCerts != null) {
        for (ProjectGenericUserCerts pguCert : pguCerts) {
          userCertsFacade.persistPGUCert(pguCert);
        }
      }
      throw new Exception(ex);
    }

  }

  public void resetSecQuestion(int id, SecurityQuestion question, String ans) {
    Users p = (Users) em.find(Users.class, id);
    p.setSecurityQuestion(question);
    p.setSecurityAnswer(ans);
    em.merge(p);

  }

  public void updateStatus(Users id, PeopleAccountStatus stat) throws ApplicationException {
    id.setStatus(stat);
    try {
      em.merge(id);
    } catch (TransactionRequiredException ex) {
      throw new ApplicationException("Need a transaction to update the user status");
    }
  }

  public void updateSecret(int id, String sec) {
    Users p = (Users) em.find(Users.class, id);
    p.setSecret(sec);
    em.merge(p);
  }

  public void increaseNumCreatedProjects(int id){
    Users u = (Users) em.find(Users.class, id);
    u.setNumCreatedProjects(u.getNumCreatedProjects() + 1);
    em.merge(u);
  }
  /**
   * Find a user through email.
   *
   * @param username
   * @return
   */
  public Users getUserByEmail(String username) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByEmail",
            Users.class);
    query.setParameter("email", username);
    List<Users> list = query.getResultList();

    if (list == null || list.isEmpty()) {
      return null;
    }

    return list.get(0);
  }

  public Users getUserByUsername(String username) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByUsername",
            Users.class);
    query.setParameter("username", username);
    List<Users> list = query.getResultList();

    if (list == null || list.isEmpty()) {
      return null;
    }

    return list.get(0);
  }

  public boolean isUsernameTaken(String username) {

    return (getUserByEmail(username) != null);
  }

  public List<Users> findMobileRequests() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByStatusAndMode", Users.class);
    query.setParameter("status", PeopleAccountStatus.VERIFIED_ACCOUNT);
    query.setParameter("mode", PeopleAccountType.M_ACCOUNT_TYPE);
    List<Users> res = query.getResultList();
    
    query = em.createNamedQuery("Users.findByStatusAndMode", Users.class);
    query.setParameter("status", PeopleAccountStatus.NEW_MOBILE_ACCOUNT);
    query.setParameter("mode", PeopleAccountType.M_ACCOUNT_TYPE);
    
    res.addAll(query.getResultList());
    return res;
  }

  public List<Users> findYubikeyRequests() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByStatusAndMode", Users.class);
    query.setParameter("status", PeopleAccountStatus.VERIFIED_ACCOUNT);
    query.setParameter("mode", PeopleAccountType.Y_ACCOUNT_TYPE);
    List<Users> res = query.getResultList();

    query = em.createNamedQuery("Users.findByStatusAndMode", Users.class);
    query.setParameter("status", PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT);
    query.setParameter("mode", PeopleAccountType.Y_ACCOUNT_TYPE);

    res.addAll(query.getResultList());
    return res;
  }

  public Yubikey findYubikey(int uid) {
    TypedQuery<Yubikey> query = em.createNamedQuery("Yubikey.findByUid",
            Yubikey.class);
    query.setParameter("uid", uid);
    return query.getSingleResult();

  }

  /**
   * Get all users except spam accounts.
   *
   * @return
   */
  public List<Users> findAllUsers() {
    List<Users> query = em.createQuery("SELECT p FROM Users p WHERE p.status != :status", Users.class).
        setParameter("status", PeopleAccountStatus.SPAM_ACCOUNT).getResultList();

    return query;
  }

  public List<Users> findAllByStatus(PeopleAccountStatus status) {
    TypedQuery<Users> query = em.
            createNamedQuery("Users.findByStatus", Users.class);
    query.setParameter("status", status);
    return query.getResultList();
  }

  /**
   * Get a list of accounts that are not validated or marked as spam.
   *
   * @return
   */
  public List<Users> findSPAMAccounts() {
    return findAllByStatus(PeopleAccountStatus.SPAM_ACCOUNT);
  }

  public Users findByEmail(String email) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByEmail",
            Users.class);
    query.setParameter("email", email);
    return query.getSingleResult();
  }

  public Address findAddress(int uid) {
    TypedQuery<Address> query = em.createNamedQuery("Address.findByUid",
            Address.class);
    query.setParameter("uid", uid);
    return query.getSingleResult();
  }

  public List<String> findGroups(int uid) {
    String sql
            = "SELECT group_name FROM hopsworks.bbc_group INNER JOIN hopsworks.people_group "
            + "ON (hopsworks.people_group.gid = hopsworks.bbc_group.gid "
            + "AND hopsworks.people_group.uid = " + uid + " )";
    List existing = em.createNativeQuery(sql).getResultList();
    return existing;
  }

  public List<String> findGroups(String userName) {
    Users user = getUserByUsername(userName);
    String sql
            = "SELECT group_name FROM hopsworks.bbc_group INNER JOIN hopsworks.people_group "
            + "ON (hopsworks.people_group.gid = hopsworks.bbc_group.gid "
            + "AND hopsworks.people_group.uid = " + user.getUid() + " )";
    List existing = em.createNativeQuery(sql).getResultList();
    return existing;
  }
  
  /**
   * Remove user's group based on uid/gid.
   *
   * @param user
   * @param gid
   */
  public void removeGroup(Users user, int gid) {
    PeopleGroup p = em.find(PeopleGroup.class, new PeopleGroup(
            new PeopleGroupPK(user.getUid(), gid)).getPeopleGroupPK());
    em.remove(p);
  }

  /**
   * Study authorization methods
   *
   * @param name of the study
   * @return List of User objects for the study, if found
   */
  public List<Users> filterUsersBasedOnStudy(String name) {

    Query query = em.createNativeQuery(
            "SELECT * FROM hopsworks.users WHERE email NOT IN (SELECT team_member "
            + "FROM hopsworks.project_team WHERE name=?)",
            Users.class).setParameter(1, name);
    return query.getResultList();
  }

  /**
   * Check the value and if empty set as '-'.
   *
   * @param var
   * @return
   */
  public String checkDefaultValue(String var) {
    if (var != null && !var.isEmpty()) {
      return var;
    }
    return "-";
  }

  /**
   * Return the max uid in the table.
   *
   * @return
   */
  public int lastUserID() {
    Query query = em.createNativeQuery(
            "SELECT MAX(p.uid) FROM hopsworks.users p");
    Object obj = query.getSingleResult();

    if (obj == null) {
      return AuthenticationConstants.STARTING_USER;
    }
    return (Integer) obj;
  }

  public void persist(Users user) {
    em.persist(user);
  }

  public void updatePeople(Users user) {
    em.merge(user);

  }

  public void updateYubikey(Yubikey yubi) {
    em.merge(yubi);
  }

  public void updateAddress(Address add) {
    em.merge(add);

  }

  /**
   * Delete user account requests.
   *
   * @param u
   * @return
   */
  public void deleteUserRequest(Users u) {
    if (u != null) {

      TypedQuery<RolesAudit> query4 = em.createNamedQuery(
              "RolesAudit.findByInitiator", RolesAudit.class);
      query4.setParameter("initiator", u);

      List<RolesAudit> results1 = query4.getResultList();

      for (Iterator<RolesAudit> iterator = results1.iterator(); iterator.
              hasNext();) {
        RolesAudit next = iterator.next();
        em.remove(next);
      }

      TypedQuery<AccountAudit> query5 = em.createNamedQuery(
              "AccountAudit.findByInitiator", AccountAudit.class);
      query5.setParameter("initiator", u);

      List<AccountAudit> aa = query5.getResultList();

      for (Iterator<AccountAudit> iterator = aa.iterator(); iterator.hasNext();) {
        AccountAudit next = iterator.next();
        em.remove(next);
      }
      u = em.merge(u);
      em.remove(u);
    }
  }

  public void registerOrg(Users uid, String org, String department) {

    Organization organization = new Organization();
    organization.setUid(uid);
    organization.setOrgName(checkDefaultValue(org));
    organization.setDepartment(checkDefaultValue(department));
    organization.setContactEmail(checkDefaultValue(""));
    organization.setContactPerson(checkDefaultValue(""));
    organization.setWebsite(checkDefaultValue(""));
    organization.setFax(checkDefaultValue(""));
    organization.setPhone(checkDefaultValue(""));
    em.persist(organization);

  }

  public void updateOrganization(Organization org) {
    em.merge(org);

  }

  /**
   * Get all the users that are not in the given project.
   *
   * @param project The project on which to search.
   * @return List of User objects that are not in the project.
   */
  public List<Users> filterUsersBasedOnProject(Project project) {
    TypedQuery<Users> query = em.createQuery(
            "SELECT u FROM hopsworks.users u WHERE u NOT IN (SELECT DISTINCT st.user"
            + " FROM hopsworks.project_team st WHERE st.project = :project)",
            Users.class);
    query.setParameter("project", project);
    return query.getResultList();
  }

  public void updateMaxNumProjs(Users id, int maxNumProjs) {
    id.setMaxNumProjects(maxNumProjs);
    em.merge(id);

  }

}
