package components.services;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.cms.dao.SessionOutcomeDao;
import components.common.client.userservice.UserServiceClientJwt;
import components.services.notification.PermissionsFinderNotificationClient;
import controllers.routes;
import models.enums.OutcomeType;
import models.view.AnswerView;
import models.view.BreadcrumbItemView;
import models.view.BreadcrumbView;
import play.twirl.api.Html;
import triage.config.ControlEntryConfig;
import triage.config.JourneyConfigService;
import triage.config.StageConfig;
import triage.session.SessionOutcome;
import uk.gov.bis.lite.customer.api.view.CustomerView;
import uk.gov.bis.lite.customer.api.view.SiteView;
import uk.gov.bis.lite.user.api.view.UserDetailsView;
import views.html.triage.decontrolBreadcrumb;
import views.html.triage.itemNotFoundBreadcrumb;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SessionOutcomeServiceImpl implements SessionOutcomeService {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMMM uuuu");

  private final String ecjuEmailAddress;
  private final UserServiceClientJwt userService;
  private final CustomerService customerService;
  private final BreadcrumbViewService breadcrumbViewService;
  private final AnswerViewService answerViewService;
  private final JourneyConfigService journeyConfigService;
  private final SessionOutcomeDao sessionOutcomeDao;
  private final PermissionsFinderNotificationClient permissionsFinderNotificationClient;
  private final views.html.nlr.nlrLetter nlrLetter;

  @Inject
  public SessionOutcomeServiceImpl(@Named("ecjuEmailAddress") String ecjuEmailAddress, UserServiceClientJwt userService,
                                   CustomerService customerService, BreadcrumbViewService breadcrumbViewService,
                                   AnswerViewService answerViewService, JourneyConfigService journeyConfigService,
                                   SessionOutcomeDao sessionOutcomeDao,
                                   PermissionsFinderNotificationClient permissionsFinderNotificationClient,
                                   views.html.nlr.nlrLetter nlrLetter) {
    this.ecjuEmailAddress = ecjuEmailAddress;
    this.userService = userService;
    this.customerService = customerService;
    this.breadcrumbViewService = breadcrumbViewService;
    this.answerViewService = answerViewService;
    this.journeyConfigService = journeyConfigService;
    this.sessionOutcomeDao = sessionOutcomeDao;
    this.permissionsFinderNotificationClient = permissionsFinderNotificationClient;
    this.nlrLetter = nlrLetter;
  }

  @Override
  public String generateNotFoundNlrLetter(String userId, String sessionId, String controlEntryId, String resumeCode) {
    ControlEntryConfig controlEntryConfig = journeyConfigService.getControlEntryConfigById(controlEntryId);
    List<BreadcrumbItemView> breadcrumbItemViews = breadcrumbViewService.createBreadcrumbItemViews(sessionId, controlEntryConfig);
    Html nlrBreadcrumb = itemNotFoundBreadcrumb.render(breadcrumbItemViews);

    return generateLetter(userId, sessionId, resumeCode, OutcomeType.NLR_NOT_FOUND, nlrBreadcrumb);
  }

  @Override
  public String generateDecontrolNlrLetter(String userId, String sessionId, String stageId, String resumeCode) {
    StageConfig stageConfig = journeyConfigService.getStageConfigById(stageId);
    List<AnswerView> answerViews = answerViewService.createAnswerViews(stageConfig, true);
    BreadcrumbView breadcrumbView = breadcrumbViewService.createBreadcrumbView(stageId, sessionId);
    Html nlrBreadcrumb = decontrolBreadcrumb.render(breadcrumbView, answerViews);

    return generateLetter(userId, sessionId, resumeCode, OutcomeType.NLR_DECONTROL, nlrBreadcrumb);
  }

  private String generateLetter(String userId, String sessionId, String resumeCode, OutcomeType outcomeType,
                                Html nlrBreadcrumb) {
    CustomerView customerView = getCustomerId(userId);
    String customerId = customerView.getCustomerId();
    SiteView siteView = getSite(customerId, userId);
    UserDetailsView userDetailsView = getUserDetailsView(userId);
    SiteView.SiteViewAddress address = siteView.getAddress();
    String todayDate = DATE_TIME_FORMATTER.format(LocalDate.now());

    Html html = nlrLetter.render(resumeCode, userDetailsView, todayDate, address, nlrBreadcrumb);
    String id = createOutcomeId();
    SessionOutcome sessionOutcome = new SessionOutcome(id, sessionId, userId, customerId, siteView.getSiteId(), outcomeType, html.toString());
    sessionOutcomeDao.insert(sessionOutcome);
    String url = routes.NlrController.renderOutcome(id).toString();
    permissionsFinderNotificationClient.sendNlrDocumentToUserEmail(userDetailsView.getContactEmailAddress(), userDetailsView.getFullName(), url);
    permissionsFinderNotificationClient.sendNlrDocumentToEcjuEmail(ecjuEmailAddress, userDetailsView.getFullName(), url, resumeCode, customerView.getCompanyName(), address.getPlainText());
    return id;
  }

  private String createOutcomeId() {
    return "out_" + UUID.randomUUID().toString().replace("-", "");
  }

  private UserDetailsView getUserDetailsView(String userId) {
    try {
      return userService.getUserDetailsView(userId).toCompletableFuture().get();
    } catch (InterruptedException | ExecutionException exception) {
      throw new RuntimeException("Unable to get userDetailsView for userId " + userId, exception);
    }
  }

  private SiteView getSite(String customerId, String userId) {
    Optional<List<SiteView>> optSites = customerService.getSitesByCustomerIdUserId(customerId, userId);
    if (optSites.isPresent()) {
      List<SiteView> sites = optSites.get();
      if (sites.size() == 1) {
        return sites.get(0);
      } else {
        throw new RuntimeException("Expected user [" + userId + "] to only have 1 associated Site but found: " + sites.size());
      }
    }
    throw new RuntimeException("Not a single Site associated with user/customer: " + userId + "/" + customerId);
  }

  private CustomerView getCustomerId(String userId) {
    Optional<List<CustomerView>> optCustomers = customerService.getCustomersByUserId(userId);
    if (optCustomers.isPresent()) {
      List<CustomerView> customers = optCustomers.get();
      if (customers.size() == 1) {
        return customers.get(0);
      } else {
        throw new RuntimeException("Expected user [" + userId + "] to only have 1 associated Customer but found: " + customers.size());
      }
    }
    throw new RuntimeException("Not a single Customer associated with user: " + userId);
  }

}
