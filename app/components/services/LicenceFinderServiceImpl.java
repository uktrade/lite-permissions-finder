package components.services;

import com.google.inject.Inject;
import components.client.CustomerService;
import components.client.PermissionRegistrationClient;
import components.common.auth.SpireAuthManager;
import components.persistence.LicenceFinderDao;
import play.Logger;
import uk.gov.bis.lite.customer.api.view.CustomerView;
import uk.gov.bis.lite.customer.api.view.SiteView;
import uk.gov.bis.lite.permissions.api.view.CallbackView;

import java.util.List;
import java.util.Optional;

public class LicenceFinderServiceImpl implements LicenceFinderService {

  private final LicenceFinderDao licenceFinderDao;
  private final CustomerService customerService;
  private final SpireAuthManager authManager;
  private final PermissionRegistrationClient permissionsService;
  private final String permissionsFinderUrl;

  @Inject
  public LicenceFinderServiceImpl(LicenceFinderDao licenceFinderDao, CustomerService customerService,
                                  PermissionRegistrationClient permissionsService, SpireAuthManager authManager,
                                  @com.google.inject.name.Named("permissionsFinderUrl") String permissionsFinderUrl) {
    this.licenceFinderDao = licenceFinderDao;
    this.permissionsService = permissionsService;
    this.customerService = customerService;
    this.authManager = authManager;
    this.permissionsFinderUrl = permissionsFinderUrl;
  }

  private void registrationResponseReceived(String transactionId, PermissionRegistrationClient.RegistrationResponse response) {
    Logger.info("Response: " + response.isSuccess());
    Logger.info("RequestId: " + response.getRequestId());
  }

  public void registerOgel() {
    String transactionId = "transactionId";
    String userId = getUserId();
    String customerId = licenceFinderDao.getCustomerId();
    String siteId = licenceFinderDao.getSiteId();
    String ogelId = licenceFinderDao.getOgelId();
    String callbackUrl = permissionsFinderUrl + "/licencefinder/registration-callback";

    permissionsService.registerOgel(userId, customerId, siteId, ogelId, callbackUrl)
        .thenAcceptAsync(response -> registrationResponseReceived(transactionId, response));

  }

  public void handleCallback(String transactionId, CallbackView callbackView) {
    String storedRequestId = licenceFinderDao.getSubmissionRequestId();
    Logger.info("handleCallback: " + storedRequestId);
  }

  public void persistCustomerAndSiteData() {
    String userId = getUserId();
    Optional<String> optCustomerId = getCustomerId(userId);
    if(optCustomerId.isPresent()) {
      String customerId = optCustomerId.get();
      licenceFinderDao.saveCustomerId(customerId); // persist customerId
      Logger.info("CustomerId persisted: " + customerId);
      Optional<String> optSiteId = getSiteId(userId, optCustomerId.get());
      if(optSiteId.isPresent()) {
        String siteId = optSiteId.get();
        licenceFinderDao.saveSiteId(siteId); // persist siteId
        Logger.info("SiteId persisted: " + siteId);
      } else {
        Logger.warn("Not a single Site associated with user/customer: " + userId + "/" + customerId);
      }
    } else {
      Logger.warn("Not a single Customer associated with user: " + userId);
    }
  }

  /**
   * We only return a CustomerId if there is only one Customer associated with the user
   */
  private Optional<String> getCustomerId(String userId) {
    Optional<List<CustomerView>> optCustomers = customerService.getCustomersByUserId(userId);
    if (optCustomers.isPresent()) {
      List<CustomerView> customers = optCustomers.get();

      // Check for single customer only TODO when we have single Customer user
      if (customers.size() > 0) {
        return Optional.of(customers.get(0).getCustomerId());
      }
    }
    return Optional.empty();
  }

  /**
   * We only return a SiteId if there is only one Site associated with the user/customer
   */
  private Optional<String> getSiteId(String userId, String customerId) {
    Optional<List<SiteView>> optSites = customerService.getSitesByCustomerIdUserId(customerId, userId);
    if (optSites.isPresent()) {
      List<SiteView> sites = optSites.get();
      // Check for single site only TODO when we have single Site user
      if (sites.size() > 0) {
        return Optional.of(sites.get(0).getSiteId());
      }
    }
    return Optional.empty();
  }

  private String getUserId() {
    return authManager.getAuthInfoFromContext().getId();
  }

}