package models.softtech;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import controllers.softtech.routes;

public class ExemptionsDisplay {

  public enum ExemptionDisplayType {
    Q1,
    Q2,
    Q3
  }

  public final String formAction;
  public final String pageTitle;
  public final String questionLabel;
  public final List<String> exemptions;

  public ExemptionsDisplay(ExemptionDisplayType exemptionDisplayType) {
    if (exemptionDisplayType == ExemptionDisplayType.Q1) {
      this.pageTitle = "Public domain software";
      this.questionLabel = "Is your software in the public domain, for example available as a free download that does not require permission or payment to use?";
      this.formAction = routes.ExemptionsController.handleSubmitQ1().url();
      this.exemptions = Collections.emptyList();
    }
    else if (exemptionDisplayType == ExemptionDisplayType.Q2) {
      this.pageTitle = "Dummy";
      this.questionLabel = "Is software for information security?";
      this.formAction = routes.ExemptionsController.handleSubmitQ2().url();
      this.exemptions = Arrays.asList("Is your software for information security?");
    }
    else {
      this.pageTitle = "Dummy";
      this.questionLabel = "Do software exemptions apply?";
      this.formAction = routes.ExemptionsController.handleSubmitQ3().url();
      this.exemptions = Arrays.asList("Software that the user can install themselves, without your help",
          "Compiled source code that is the minimum needed to install, operate, maintain or repair exported items");
    }

  }
}
