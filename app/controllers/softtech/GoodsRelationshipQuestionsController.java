package controllers.softtech;

import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.common.journey.StandardEvents;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.controls.relationships.GoodsRelationship;
import components.services.controlcode.controls.relationships.GoodsRelationshipsServiceClient;
import exceptions.FormStateException;
import models.GoodsType;
import models.softtech.GoodsRelationshipQuestionsDisplay;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.softtech.goodsRelationshipQuestions;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class GoodsRelationshipQuestionsController {

  private final FormFactory formFactory;
  private final JourneyManager journeyManager;
  private final PermissionsFinderDao permissionsFinderDao;
  private final GoodsRelationshipsServiceClient goodsRelationshipsServiceClient;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public GoodsRelationshipQuestionsController(FormFactory formFactory,
                                              JourneyManager journeyManager,
                                              PermissionsFinderDao permissionsFinderDao,
                                              GoodsRelationshipsServiceClient goodsRelationshipsServiceClient,
                                              HttpExecutionContext httpExecutionContext) {
    this.formFactory = formFactory;
    this.journeyManager = journeyManager;
    this.permissionsFinderDao = permissionsFinderDao;
    this.goodsRelationshipsServiceClient = goodsRelationshipsServiceClient;
    this.httpExecutionContext = httpExecutionContext;
  }


  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public CompletionStage<Result> renderForm(String goodsTypeText, String relatedToGoodsTypeText) {
    GoodsType goodsType = GoodsType.getMatchedByUrlString(goodsTypeText).get();
    GoodsType relatedToGoodsType = GoodsType.getMatchedByUrlString(relatedToGoodsTypeText).get();
    return renderFormInternal(goodsType, relatedToGoodsType);
  }

  private CompletionStage<Result> renderFormInternal(GoodsType goodsType, GoodsType relatedToGoodsType) {
    int currentQuestionIndex;
    Optional<Integer> currentQuestionIndexOptional = permissionsFinderDao.getGoodsRelationshipQuestionCurrentIndex(goodsType, relatedToGoodsType);
    GoodsRelationshipQuestionsForm templateForm = new GoodsRelationshipQuestionsForm();
    if (currentQuestionIndexOptional.isPresent()) {
      currentQuestionIndex = currentQuestionIndexOptional.get();
    } else {
      // Initialise currentQuestionIndex into DAO
      currentQuestionIndex = 0;
      permissionsFinderDao.saveGoodsRelationshipQuestionCurrentIndex(goodsType, relatedToGoodsType, currentQuestionIndex);
    }
    Optional<Boolean> questionAnswerOptional = permissionsFinderDao.getGoodsRelationshipQuestionAnswer(goodsType, relatedToGoodsType, currentQuestionIndex);
    templateForm.questionAnswer = questionAnswerOptional.isPresent() ? questionAnswerOptional.get().toString() : "";
    templateForm.currentQuestionIndex = currentQuestionIndex;
    return renderWithForm(goodsType, relatedToGoodsType, formFactory.form(GoodsRelationshipQuestionsForm.class).fill(templateForm), currentQuestionIndex);
  }

  private CompletionStage<Result> renderWithForm(GoodsType goodsType, GoodsType relatedToGoodsType, Form<GoodsRelationshipQuestionsForm> form, int currentQuestionIndex){
    return goodsRelationshipsServiceClient.get(goodsType, relatedToGoodsType)
        .thenApplyAsync(result -> {
          GoodsRelationship relationship = result.getRelationship(currentQuestionIndex);
          GoodsRelationshipQuestionsDisplay display = new GoodsRelationshipQuestionsDisplay(goodsType, relatedToGoodsType, relationship);
          return ok(goodsRelationshipQuestions.apply(form, display));
        }, httpExecutionContext.current());
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public CompletionStage<Result> handleSubmit(String goodsTypeText, String relatedToGoodsTypeText) {
    GoodsType goodsType = GoodsType.getMatchedByUrlString(goodsTypeText).get();
    GoodsType relatedToGoodsType = GoodsType.getMatchedByUrlString(relatedToGoodsTypeText).get();
    return handleSubmitInternal(goodsType, relatedToGoodsType);
  }

  private CompletionStage<Result> handleSubmitInternal(GoodsType goodsType, GoodsType relatedToGoodsType) {

    Form<GoodsRelationshipQuestionsForm> form = formFactory.form(GoodsRelationshipQuestionsForm.class).bindFromRequest();

    if (form.hasErrors()) {
      // TODO Decide, should this be DAO or form state driven?
      int currentQuestionIndex = Integer.parseInt(form.field("currentQuestionIndex").value());
      return renderWithForm(goodsType, relatedToGoodsType, form, currentQuestionIndex);
    }

    String questionAnswer = form.get().questionAnswer;

    int currentQuestionIndex = form.get().currentQuestionIndex;

    if ("true".equals(questionAnswer)) {
      return goodsRelationshipsServiceClient.get(goodsType, relatedToGoodsType)
          .thenComposeAsync(result -> {
            if (result.isValidRelationshipIndex(currentQuestionIndex)) {
              permissionsFinderDao.saveGoodsRelationshipQuestionAnswer(goodsType, relatedToGoodsType, currentQuestionIndex, true);
              permissionsFinderDao.saveGoodsRelationshipQuestionCurrentIndex(goodsType, relatedToGoodsType, currentQuestionIndex);
              return journeyManager.performTransition(StandardEvents.YES);
            }
            else {
              throw new RuntimeException(String.format("Invalid value for currentQuestionIndex: %d", currentQuestionIndex));
            }
          }, httpExecutionContext.current());
    }
    else if ("false".equals(questionAnswer)) {
      // Belt and braces check
      return goodsRelationshipsServiceClient.get(goodsType, relatedToGoodsType)
          .thenComposeAsync(result -> {
            if (result.isValidRelationshipIndex(currentQuestionIndex)) {
              permissionsFinderDao.saveGoodsRelationshipQuestionAnswer(goodsType, relatedToGoodsType, currentQuestionIndex, false);
              permissionsFinderDao.saveGoodsRelationshipQuestionCurrentIndex(goodsType, relatedToGoodsType, currentQuestionIndex);

              if (result.hasNextRelationship(currentQuestionIndex)) {
                // Increment and save new index
                int newCurrentQuestionIndex = currentQuestionIndex + 1;
                permissionsFinderDao.saveGoodsRelationshipQuestionCurrentIndex(goodsType, relatedToGoodsType, newCurrentQuestionIndex);
                GoodsRelationshipQuestionsForm templateForm = new GoodsRelationshipQuestionsForm();
                Optional<Boolean> questionAnswerOptional = permissionsFinderDao.getGoodsRelationshipQuestionAnswer(goodsType, relatedToGoodsType, newCurrentQuestionIndex);
                templateForm.questionAnswer = questionAnswerOptional.isPresent() ? questionAnswerOptional.get().toString() : "";
                templateForm.currentQuestionIndex = newCurrentQuestionIndex;
                return renderWithForm(goodsType, relatedToGoodsType, formFactory.form(GoodsRelationshipQuestionsForm.class).fill(templateForm), newCurrentQuestionIndex);
              }
              else {
                return journeyManager.performTransition(StandardEvents.NO);
              }
            }
            else {
              throw new RuntimeException(String.format("Invalid value for currentQuestionIndex: %d", currentQuestionIndex));
            }
          }, httpExecutionContext.current());
    }
    else {
      throw new FormStateException(String.format("Invalid value for questionAnswer: %s", questionAnswer));
    }
  }

  public static class GoodsRelationshipQuestionsForm {

    public int currentQuestionIndex;

    @Required(message = "You must answer this question")
    public String questionAnswer;

  }
}
