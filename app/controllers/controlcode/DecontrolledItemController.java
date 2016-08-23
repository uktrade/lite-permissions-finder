package controllers.controlcode;

import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.frontend.FrontendServiceResult;
import model.ExportCategory;
import play.mvc.Result;
import views.html.controlcode.decontrolledItem;

public class DecontrolledItemController {

  private final PermissionsFinderDao dao;

  @Inject
  public DecontrolledItemController(PermissionsFinderDao dao) {
    this.dao = dao;
  }

  public Result render(FrontendServiceResult frontendServiceResult) {
    boolean showFirearmsAndMilitary =  dao.getExportCategory() == ExportCategory.MILITARY;
    return ok(decontrolledItem.render(frontendServiceResult, showFirearmsAndMilitary));
  }
}