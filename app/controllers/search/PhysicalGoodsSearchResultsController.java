package controllers.search;

import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.FrontendServiceClient;
import components.services.search.SearchServiceClient;
import components.services.search.SearchServiceResult;
import controllers.ErrorController;
import controllers.controlcode.ControlCodeController;
import exceptions.FormStateException;
import journey.Events;
import journey.helpers.ControlCodeJourneyHelper;
import journey.helpers.SoftTechJourneyHelper;
import models.GoodsType;
import models.controlcode.ControlCodeJourney;
import models.search.SearchResultsBaseDisplay;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.search.physicalGoodsSearchResults;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class PhysicalGoodsSearchResultsController extends SearchResultsController {

  private final HttpExecutionContext httpExecutionContext;
  private final PermissionsFinderDao permissionsFinderDao;
  private final SoftTechJourneyHelper softTechJourneyHelper;

  @Inject
  public PhysicalGoodsSearchResultsController(JourneyManager journeyManager,
                                              FormFactory formFactory,
                                              SearchServiceClient searchServiceClient,
                                              FrontendServiceClient frontendServiceClient,
                                              ControlCodeController controlCodeController,
                                              ErrorController errorController,
                                              HttpExecutionContext httpExecutionContext,
                                              PermissionsFinderDao permissionsFinderDao,
                                              SoftTechJourneyHelper softTechJourneyHelper) {
    super(journeyManager, formFactory, searchServiceClient, frontendServiceClient, controlCodeController, errorController);
    this.httpExecutionContext = httpExecutionContext;
    this.permissionsFinderDao = permissionsFinderDao;
    this.softTechJourneyHelper = softTechJourneyHelper;
  }

  private CompletionStage<Result> renderForm(ControlCodeJourney controlCodeJourney) {
    return physicalGoodsSearch(controlCodeJourney)
        .thenApplyAsync(result -> {
          int displayCount = Math.min(result.results.size(), PAGINATION_SIZE);
          Optional<Integer> optionalDisplayCount = permissionsFinderDao.getPhysicalGoodSearchPaginationDisplayCount(controlCodeJourney);
          if (optionalDisplayCount.isPresent()) {
            displayCount = Math.min(result.results.size(), optionalDisplayCount.get());
          }
          else {
            permissionsFinderDao.savePhysicalGoodSearchPaginationDisplayCount(controlCodeJourney, displayCount);
          }
          String lastChosenControlCode = permissionsFinderDao.getPhysicalGoodSearchLastChosenControlCode(controlCodeJourney);
          SearchResultsBaseDisplay display = new SearchResultsBaseDisplay(controlCodeJourney, searchResultsForm(), GoodsType.PHYSICAL,
              result.results, displayCount, lastChosenControlCode);
          return ok(physicalGoodsSearchResults.render(display));
        }, httpExecutionContext.current());
  }

  public CompletionStage<Result> renderSearchForm() {
    return renderForm(ControlCodeJourney.PHYSICAL_GOODS_SEARCH);
  }

  public CompletionStage<Result> renderSearchRelatedToForm(String goodsTypeText) {
    return ControlCodeJourneyHelper.getSearchRelatedToPhysicalGoodsResult(goodsTypeText, this::renderForm);
  }

  private CompletionStage<Result> handleSubmit(ControlCodeJourney controlCodeJourney) {
    Form<ControlCodeSearchResultsForm> form = bindSearchResultsForm();

    if (form.hasErrors()) {
      return physicalGoodsSearch(controlCodeJourney)
          .thenApplyAsync(result -> {
            int displayCount = Integer.parseInt(form.field("resultsDisplayCount").value());
            int newDisplayCount = Math.min(displayCount, result.results.size());
            if (displayCount != newDisplayCount) {
              permissionsFinderDao.savePhysicalGoodSearchPaginationDisplayCount(controlCodeJourney, newDisplayCount);
            }
            SearchResultsBaseDisplay display = new SearchResultsBaseDisplay(controlCodeJourney, form, GoodsType.PHYSICAL,
                result.results, newDisplayCount);
            return ok(physicalGoodsSearchResults.render(display));
          }, httpExecutionContext.current());
    }

    Optional<SearchResultAction> action = getAction(form.get());
    if (action.isPresent()){
      switch (action.get()) {
        case NONE_MATCHED:
          return noneMatched(controlCodeJourney);
        case SHORE_MORE:
          return physicalGoodsSearch(controlCodeJourney)
              .thenApplyAsync(result -> {
                int displayCount = Integer.parseInt(form.get().resultsDisplayCount);
                int newDisplayCount = Math.min(displayCount + PAGINATION_SIZE, result.results.size());
                if (displayCount != newDisplayCount) {
                  permissionsFinderDao.savePhysicalGoodSearchPaginationDisplayCount(controlCodeJourney, newDisplayCount);
                }
                SearchResultsBaseDisplay display = new SearchResultsBaseDisplay(controlCodeJourney, form, GoodsType.PHYSICAL, result.results, newDisplayCount);
                return ok(physicalGoodsSearchResults.render(display));
              }, httpExecutionContext.current());
        case EDIT_DESCRIPTION:
          return editDescription(controlCodeJourney);
        case CONTINUE:
          return continueWithService(controlCodeJourney);
      }
    }

    Optional<String> result = getResult(form.get());
    if (result.isPresent()) {
      int displayCount = Integer.parseInt(form.get().resultsDisplayCount);
      permissionsFinderDao.clearAndUpdateControlCodeJourneyDaoFieldsIfChanged(controlCodeJourney, result.get());
      permissionsFinderDao.savePhysicalGoodSearchPaginationDisplayCount(controlCodeJourney, displayCount);
      permissionsFinderDao.savePhysicalGoodSearchLastChosenControlCode(controlCodeJourney, result.get());
      return journeyManager.performTransition(Events.CONTROL_CODE_SELECTED);
    }

    throw new FormStateException("Unhandled form state");
  }

  public CompletionStage<Result> handleSearchSubmit() {
    return handleSubmit(ControlCodeJourney.PHYSICAL_GOODS_SEARCH);
  }

  public CompletionStage<Result> handleSearchRelatedToSubmit(String goodsTypeText) {
    return ControlCodeJourneyHelper.getSearchRelatedToPhysicalGoodsResult(goodsTypeText, this::handleSubmit);
  }

  public CompletionStage<SearchServiceResult> physicalGoodsSearch(ControlCodeJourney controlCodeJourney) {
    String searchTerms = PhysicalGoodsSearchController.getSearchTerms(permissionsFinderDao.getPhysicalGoodsSearchForm(controlCodeJourney).get());
    return searchServiceClient.get(searchTerms);
  }

  private CompletionStage<Result> noneMatched(ControlCodeJourney controlCodeJourney){
      if (controlCodeJourney == ControlCodeJourney.PHYSICAL_GOODS_SEARCH ||
          controlCodeJourney == ControlCodeJourney.SOFTWARE_CONTROLS ||
          controlCodeJourney == ControlCodeJourney.TECHNOLOGY_CONTROLS) {
      return journeyManager.performTransition(Events.NONE_MATCHED);
    }
    else if (controlCodeJourney == ControlCodeJourney.PHYSICAL_GOODS_SEARCH_RELATED_TO_SOFTWARE ||
          controlCodeJourney == ControlCodeJourney.PHYSICAL_GOODS_SEARCH_RELATED_TO_TECHNOLOGY) {
      return continueWithService(controlCodeJourney);
    }
    else {
      throw new RuntimeException(String.format("Unexpected member of ControlCodeJourney enum: \"%s\""
          , controlCodeJourney.toString()));
    }
  }

  private CompletionStage<Result> editDescription(ControlCodeJourney controlCodeJourney) {
    if (controlCodeJourney == ControlCodeJourney.PHYSICAL_GOODS_SEARCH_RELATED_TO_SOFTWARE ||
        controlCodeJourney == ControlCodeJourney.PHYSICAL_GOODS_SEARCH_RELATED_TO_TECHNOLOGY) {
      return journeyManager.performTransition(Events.EDIT_SEARCH_DESCRIPTION);
    }
    else {
      throw new RuntimeException(String.format("Unexpected member of ControlCodeJourney enum: \"%s\""
          , controlCodeJourney.toString()));
    }
  }

  private CompletionStage<Result> continueWithService(ControlCodeJourney controlCodeJourney) {
    if (controlCodeJourney == ControlCodeJourney.PHYSICAL_GOODS_SEARCH_RELATED_TO_SOFTWARE) {
      return softTechJourneyHelper.performCatchallSoftTechControlsTransition(GoodsType.SOFTWARE);
    }
    else if (controlCodeJourney == ControlCodeJourney.PHYSICAL_GOODS_SEARCH_RELATED_TO_TECHNOLOGY) {
      return softTechJourneyHelper.performCatchallSoftTechControlsTransition(GoodsType.TECHNOLOGY);
    }
    else {
      throw new RuntimeException(String.format("Unexpected member of ControlCodeJourney enum: \"%s\""
          , controlCodeJourney.toString()));
    }
  }

}