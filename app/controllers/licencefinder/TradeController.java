package controllers.licencefinder;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.inject.Inject;
import components.auth.SamlAuthorizer;
import components.common.auth.SpireAuthManager;
import components.common.auth.SpireSAML2Client;
import components.persistence.LicenceFinderDao;
import exceptions.FormStateException;
import models.TradeType;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Secure(clients = SpireSAML2Client.CLIENT_NAME, authorizers = SamlAuthorizer.AUTHORIZER_NAME)
public class TradeController extends Controller {

  private final FormFactory formFactory;
  private final LicenceFinderDao licenceFinderDao;
  private final views.html.licencefinder.trade trade;
  private final SpireAuthManager authManager;

  private static final String UNITED_KINGDOM = "CTRY0";

  @Inject
  public TradeController(FormFactory formFactory, LicenceFinderDao licenceFinderDao, views.html.licencefinder.trade trade,
                         SpireAuthManager authManager) {
    this.formFactory = formFactory;
    this.licenceFinderDao = licenceFinderDao;
    this.trade = trade;
    this.authManager = authManager;
  }

  /**
   * Licence finder flow entry point
   */
  public CompletionStage<Result> entry(String controlCode) {
    String sessionId = UUID.randomUUID().toString();
    licenceFinderDao.saveControlCode(sessionId, controlCode);
    licenceFinderDao.saveUserId(sessionId, authManager.getAuthInfoFromContext().getId());
    return renderTradeForm(sessionId);
  }

  /**
   * renderTradeForm
   */
  public CompletionStage<Result> renderTradeForm(String sessionId) {
    TradeTypeForm form = new TradeTypeForm();
    licenceFinderDao.getTradeType(sessionId).ifPresent((e) -> form.tradeType = e.toString());
    return completedFuture(ok(trade.render(formFactory.form(TradeTypeForm.class).fill(form), licenceFinderDao.getControlCode(sessionId), sessionId)));
  }

  /**
   * handleTradeSubmit
   */
  public CompletionStage<Result> handleTradeSubmit(String sessionId) {
    Form<TradeTypeForm> form = formFactory.form(TradeTypeForm.class).bindFromRequest();
    String controlCode = licenceFinderDao.getControlCode(sessionId);
    if (form.hasErrors()) {
      return completedFuture(ok(trade.render(form, controlCode, sessionId)));
    }

    TradeType tradeType = TradeType.valueOf(form.get().tradeType);
    licenceFinderDao.saveTradeType(sessionId, tradeType);

    switch (tradeType) {
      case EXPORT:
        licenceFinderDao.saveSourceCountry(sessionId, UNITED_KINGDOM);
        return completedFuture(redirect(routes.DestinationController.renderDestinationForm(sessionId)));
      case TRANSSHIPMENT:
        return completedFuture(redirect(controllers.routes.StaticContentController.renderTranshipment()));
      case BROKERING:
        return completedFuture(redirect(controllers.routes.StaticContentController.renderBrokering()));
      default:
        throw new FormStateException("Unknown trade type " + tradeType);
    }
  }

  public static class TradeTypeForm {
    @Required(message = "Select where your items are going")
    public String tradeType;
  }

}

