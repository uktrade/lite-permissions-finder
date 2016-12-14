package controllers.importcontent;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.inject.Inject;
import components.common.client.CountryServiceClient;
import components.common.journey.JourneyManager;
import components.persistence.ImportJourneyDao;
import exceptions.FormStateException;
import exceptions.ServiceResponseException;
import importcontent.ImportEvents;
import importcontent.models.ImportCountry;
import importcontent.models.ImportWhere;
import models.common.Country;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.importcontent.importCountry;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.inject.Named;

public class ImportWhereController extends Controller {

  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final ImportJourneyDao importJourneyDao;
  private final HttpExecutionContext httpExecutionContext;
  private final CountryServiceClient countryServiceExport;
  private final CountryServiceClient countryServiceEu;

  public static final String COUNTRY_FIELD_NAME = "importCountry";

  @Inject
  public ImportWhereController(JourneyManager journeyManager, FormFactory formFactory, ImportJourneyDao importJourneyDao,
                               HttpExecutionContext httpExecutionContext,
                               @Named("countryServiceExport") CountryServiceClient countryServiceExport,
                               @Named("countryServiceEu") CountryServiceClient countryServiceEu) {
    this.journeyManager = journeyManager;
    this.formFactory = formFactory;
    this.importJourneyDao = importJourneyDao;
    this.httpExecutionContext = httpExecutionContext;
    this.countryServiceExport = countryServiceExport;
    this.countryServiceEu = countryServiceEu;
  }

  public CompletionStage<Result> renderForm() {
    return countryServiceExport.getCountries()
        .thenApplyAsync(response -> {
          if (response.getStatus() == CountryServiceClient.Status.SUCCESS && !response.getCountries().isEmpty()) {
            return ok(importCountry.render(formFactory.form(), response.getCountries()));
          } else {
            throw new ServiceResponseException("Country service supplied an invalid response");
          }
        }, httpExecutionContext.current());
  }

  public CompletionStage<Result> handleSubmit() {
    Form<ImportCountryForm> form = formFactory.form(ImportCountryForm.class).bindFromRequest();
    return countryServiceExport.getCountries()
        .thenApplyAsync(response -> {
          if (response.getStatus() == CountryServiceClient.Status.SUCCESS && !response.getCountries().isEmpty()) {
            if (form.hasErrors()) {
              return completedFuture(ok(importCountry.render(formFactory.form(), response.getCountries())));
            }
            String importCountrySelectedCode = form.get().importCountry;
            Optional<Country> optCountry = response.getCountries().stream()
                .filter(country -> importCountrySelectedCode.equals(country.getCountryRef())).findFirst();
            if (optCountry.isPresent()) {
              importJourneyDao.saveImportCountrySelected(importCountrySelectedCode);
              return journeyManager.performTransition(ImportEvents.IMPORT_WHERE_SELECTED, getImportWhereRoute(optCountry.get().getCountryRef()));
            } else {
              throw new FormStateException("Country not recognised: " + importCountrySelectedCode);
            }
          } else {
            throw new FormStateException("An issue occurred while processing your request, please try again later.");
          }
        }, httpExecutionContext.current()).thenCompose(Function.identity());
  }

  /**
   * Returns appropriate ImportWhere enum from selected Spire country code
   */
  private ImportWhere getImportWhereRoute(String spireCode) {
    if (ImportController.MILITARY_COUNTRY_SPIRE_CODES.contains(spireCode)) {
      return ImportWhere.MILITARY_COUNTRIES;
    } else if (spireCode.equals(ImportController.SOMALIA_SPIRE_CODE)) {
      return ImportWhere.CHARCOAL_COUNTRIES;
    } else if (spireCode.equals(ImportController.SYRIA_SPIRE_CODE)) {
      return ImportWhere.SYRIA_COUNTRY;
    } else {
      if (isEuCountry(spireCode)) {
        return ImportWhere.EU_COUNTRIES;
      }
    }
    return ImportWhere.OTHER_COUNTRIES;
  }

  private boolean isEuCountry(String spireCountryCode) {
    return ImportCountry.getMatched(spireCountryCode).map(ImportCountry::isEu).orElse(false);
  }

  /**
   * ImportCountryForm
   */
  public static class ImportCountryForm {
    public String importCountry;
  }
}



