package controllers.search;

import static play.mvc.Results.ok;

import com.google.common.base.Enums;
import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.common.journey.StandardEvents;
import components.persistence.PermissionsFinderDao;
import components.services.search.search.SearchServiceClient;
import components.services.search.search.SearchServiceResult;
import exceptions.FormStateException;
import journey.Events;
import journey.helpers.ControlCodeSubJourneyHelper;
import models.controlcode.BackType;
import models.controlcode.ControlCodeSubJourney;
import models.controlcode.ControlCodeVariant;
import models.search.SearchResultsDisplay;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.search.searchResults;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class SearchResultsController {

  public static final int PAGINATION_SIZE = 5;

  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final SearchServiceClient searchServiceClient;
  private final HttpExecutionContext httpExecutionContext;
  private final PermissionsFinderDao permissionsFinderDao;

  public enum SearchResultAction{
    NONE_MATCHED,
    SHOW_MORE,
    EDIT_DESCRIPTION,
    CONTINUE;

    public static Optional<SearchResultAction> getMatched(String name) {
      return Enums.getIfPresent(SearchResultAction.class, name)
          .transform(java.util.Optional::of)
          .or(java.util.Optional.empty());
    }
  }

  @Inject
  public SearchResultsController(JourneyManager journeyManager,
                                 FormFactory formFactory,
                                 SearchServiceClient searchServiceClient,
                                 HttpExecutionContext httpExecutionContext,
                                 PermissionsFinderDao permissionsFinderDao) {
    this.journeyManager = journeyManager;
    this.formFactory = formFactory;
    this.searchServiceClient = searchServiceClient;
    this.httpExecutionContext = httpExecutionContext;
    this.permissionsFinderDao = permissionsFinderDao;
  }

  public CompletionStage<Result> renderForm(String goodsTypeText) {
    ControlCodeSubJourney controlCodeSubJourney = ControlCodeSubJourneyHelper.resolveUrlToSubJourneyAndUpdateContext(ControlCodeVariant.SEARCH.urlString(), goodsTypeText);
    return renderFormInternal(controlCodeSubJourney);
  }

  private CompletionStage<Result> renderFormInternal(ControlCodeSubJourney controlCodeSubJourney) {
    return physicalGoodsSearch(controlCodeSubJourney)
        .thenApplyAsync(result -> {
          int displayCount = Math.min(result.results.size(), PAGINATION_SIZE);
          Optional<Integer> optionalDisplayCount = permissionsFinderDao.getPhysicalGoodSearchPaginationDisplayCount(controlCodeSubJourney);
          if (optionalDisplayCount.isPresent()) {
            displayCount = Math.min(result.results.size(), optionalDisplayCount.get());
          }
          else {
            permissionsFinderDao.savePhysicalGoodSearchPaginationDisplayCount(controlCodeSubJourney, displayCount);
          }
          String lastChosenControlCode = permissionsFinderDao.getPhysicalGoodSearchLastChosenControlCode(controlCodeSubJourney);
          SearchResultsDisplay display = new SearchResultsDisplay(controlCodeSubJourney, formFactory.form(SearchResultsForm.class),
              result.results, displayCount, lastChosenControlCode);
          return ok(searchResults.render(display));
        }, httpExecutionContext.current());
  }

  public CompletionStage<Result> handleSubmit() {
    ControlCodeSubJourney controlCodeSubJourney = ControlCodeSubJourneyHelper.resolveContextToSubJourney();
    return handleSubmitInternal(controlCodeSubJourney);
  }

  private CompletionStage<Result> handleSubmitInternal(ControlCodeSubJourney controlCodeSubJourney) {
    Form<SearchResultsForm> form = formFactory.form(SearchResultsForm.class).bindFromRequest();

    if (form.hasErrors()) {
      return physicalGoodsSearch(controlCodeSubJourney)
          .thenApplyAsync(result -> {
            int displayCount = Integer.parseInt(form.field("resultsDisplayCount").value());
            int newDisplayCount = Math.min(displayCount, result.results.size());
            if (displayCount != newDisplayCount) {
              permissionsFinderDao.savePhysicalGoodSearchPaginationDisplayCount(controlCodeSubJourney, newDisplayCount);
            }
            SearchResultsDisplay display = new SearchResultsDisplay(controlCodeSubJourney, form, result.results, newDisplayCount);
            return ok(searchResults.render(display));
          }, httpExecutionContext.current());
    }

    Optional<SearchResultAction> action = SearchResultAction.getMatched(form.get().action);
    if (action.isPresent()){
      switch (action.get()) {
        case NONE_MATCHED:
          return journeyManager.performTransition(Events.NONE_MATCHED);
        case SHOW_MORE:
          return physicalGoodsSearch(controlCodeSubJourney)
              .thenApplyAsync(result -> {
                int displayCount = Integer.parseInt(form.get().resultsDisplayCount);
                int newDisplayCount = Math.min(displayCount + PAGINATION_SIZE, result.results.size());
                if (displayCount != newDisplayCount) {
                  permissionsFinderDao.savePhysicalGoodSearchPaginationDisplayCount(controlCodeSubJourney, newDisplayCount);
                }
                SearchResultsDisplay display = new SearchResultsDisplay(controlCodeSubJourney, form, result.results, newDisplayCount);
                return ok(searchResults.render(display));
              }, httpExecutionContext.current());
        case EDIT_DESCRIPTION:
          return journeyManager.performTransition(Events.BACK, BackType.SEARCH);
        case CONTINUE:
          return journeyManager.performTransition(StandardEvents.NEXT);
      }
    }

    Optional<String> result = getResult(form.get());
    if (result.isPresent()) {
      int displayCount = Integer.parseInt(form.get().resultsDisplayCount);
      permissionsFinderDao.clearAndUpdateControlCodeSubJourneyDaoFieldsIfChanged(controlCodeSubJourney, result.get());
      permissionsFinderDao.savePhysicalGoodSearchPaginationDisplayCount(controlCodeSubJourney, displayCount);
      permissionsFinderDao.savePhysicalGoodSearchLastChosenControlCode(controlCodeSubJourney, result.get());
      return journeyManager.performTransition(Events.CONTROL_CODE_SELECTED);
    }

    throw new FormStateException("Unhandled form state");
  }

  public CompletionStage<SearchServiceResult> physicalGoodsSearch(ControlCodeSubJourney controlCodeSubJourney) {
    String searchTerms = SearchController.getSearchTerms(permissionsFinderDao.getPhysicalGoodsSearchForm(controlCodeSubJourney).get());
    return searchServiceClient.get(searchTerms);
  }

  public Optional<String> getResult(SearchResultsForm form) {
    return !(form.result == null || form.result.isEmpty()) ? Optional.of(form.result) : Optional.empty();
  }

  public static class SearchResultsForm {

    public String result;

    public String action;

    public String controlCodeSubJourney;

    public String resultsDisplayCount;

    public String paginationSize;
  }

}