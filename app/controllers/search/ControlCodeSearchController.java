package controllers.search;

import controllers.ErrorController;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;

import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ControlCodeSearchController {

  protected final FormFactory formFactory;

  protected final ControlCodeSearchClient controlCodeSearchClient;

  protected final ErrorController errorController;

  public ControlCodeSearchController(FormFactory formFactory, ControlCodeSearchClient controlCodeSearchClient,
                                     ErrorController errorController) {
    this.formFactory = formFactory;
    this.controlCodeSearchClient = controlCodeSearchClient;
    this.errorController = errorController;
  }

  public CompletionStage<ControlCodeSearchResponse> physicalGoodsSearch(Form<ControlCodeSearchForm> form) {
    return controlCodeSearchClient.search(getSearchTerms(form));
  }

  public Form<ControlCodeSearchForm> bindForm(){
    return formFactory.form(ControlCodeSearchForm.class).bindFromRequest();
  }

  public String getSearchTerms(Form<ControlCodeSearchForm> form){
    return Stream.of(ControlCodeSearchForm.class.getFields())
        .map(f -> form.field(f.getName()).value())
        .filter(fv -> !fv.isEmpty())
        .collect(Collectors.joining(", "));
  }

  public static class ControlCodeSearchForm {

    @Required(message = "You must enter a description of your goods")
    public String description;

    public String component;

    public String brand;

    public String partNumber;

    public String hsCode;
  }
}
