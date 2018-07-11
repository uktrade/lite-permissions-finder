package controllers.admin;

import actions.BasicAuthAction;
import com.google.inject.Inject;
import components.services.PingService;
import models.admin.PingAuditResult;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

/**
 * Actions for system administrators
 */
public class AdminController extends Controller {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AdminController.class);
  private final PingService pingService;

  private final String PING_XML_TEMPLATE = "<pingdom_http_custom_check><status>%s</status><detail>%s</detail></pingdom_http_custom_check>";

  @Inject
  public AdminController(PingService pingService) {
    this.pingService = pingService;
  }

  @With(BasicAuthAction.class)
  public Result buildInfo() {
    return ok(buildinfo.BuildInfo$.MODULE$.toJson()).as("application/json");
  }

  public Result ping() {
    LOGGER.info("ping");
    PingAuditResult result = pingService.pingAudit();


    String status = String.format(PING_XML_TEMPLATE, result.getStatus(), result.getDetail());

    return ok(String.format(PING_XML_TEMPLATE, result.getStatus(), result.getDetail())).as("application/xml");
  }


}
