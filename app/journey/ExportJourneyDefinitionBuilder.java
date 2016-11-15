package journey;

import com.google.inject.Inject;
import components.common.journey.BackLink;
import components.common.journey.JourneyDefinitionBuilder;
import components.common.journey.JourneyStage;
import components.common.journey.StandardEvents;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.category.controls.CategoryControlsServiceClient;
import controllers.routes;
import models.ArtsCulturalGoodsType;
import models.ControlCodeFlowStage;
import models.ExportCategory;
import models.GoodsType;
import models.LifeType;
import models.NonMilitaryFirearmExportBySelfType;
import models.VirtualEUOgelStage;
import models.software.ApplicableSoftwareControls;
import models.software.SoftwareCategory;
import models.software.SoftwareExemptionsFlow;

public class ExportJourneyDefinitionBuilder extends JourneyDefinitionBuilder {

  private final JourneyStage exportCategory = defineStage("exportCategory", "What are you exporting?",
      controllers.categories.routes.ExportCategoryController.renderForm());
  private final JourneyStage goodsType = defineStage("goodsType", "Are you exporting goods, software or technical information?",
      routes.GoodsTypeController.renderForm());
  private final JourneyStage physicalGoodsSearch = defineStage("physicalGoodsSearch", "Describe your items",
      controllers.search.routes.PhysicalGoodsSearchController.renderForm());
  private final JourneyStage destinationCountries = defineStage("destinationCountries", "Countries and territories",
      routes.DestinationCountryController.renderForm());
  private final JourneyStage ogelQuestions = defineStage("ogelQuestions", "Refining your licence results",
      controllers.ogel.routes.OgelQuestionsController.renderForm());
  private final JourneyStage notImplemented = defineStage("notImplemented", "This section is currently under development",
      routes.StaticContentController.renderNotImplemented());
  private final JourneyStage notApplicable = defineStage("notApplicable", "You cannot use this service to get an export licence",
      routes.StaticContentController.renderNotApplicable());
  private final JourneyStage softwareExemptions = defineStage("softwareExemptions", "Some types of software do not need a licence",
      controllers.software.routes.ExemptionsController.renderForm());

  private final PermissionsFinderDao permissionsFinderDao;
  private final CategoryControlsServiceClient categoryControlsServiceClient;

  @Inject
  public ExportJourneyDefinitionBuilder(PermissionsFinderDao permissionsFinderDao, CategoryControlsServiceClient categoryControlsServiceClient) {
    this.permissionsFinderDao = permissionsFinderDao;
    this.categoryControlsServiceClient = categoryControlsServiceClient;
  }

  @Override
  protected void journeys() {
    // *** Stages/transitions ***

    goodsCategoryStages();

    atStage(goodsType)
        .onEvent(Events.GOODS_TYPE_SELECTED)
        .branch()
        .when(GoodsType.PHYSICAL, moveTo(physicalGoodsSearch))
        .when(GoodsType.SOFTWARE, moveTo(softwareExemptions))
        .when(GoodsType.TECHNOLOGY, moveTo(notImplemented));

    physicalGoodsStages();

    softwareStages();

    // *** Journeys ***

    defineJourney(JourneyDefinitionNames.EXPORT, exportCategory, BackLink.to(routes.TradeTypeController.renderForm(),
        "Where are your items going?"));

    defineJourney(JourneyDefinitionNames.CHANGE_CONTROL_CODE, physicalGoodsSearch,
        BackLink.to(routes.SummaryController.renderForm(), "Summary"));
    defineJourney(JourneyDefinitionNames.CHANGE_DESTINATION_COUNTRIES, destinationCountries,
        BackLink.to(routes.SummaryController.renderForm(), "Summary"));
    defineJourney(JourneyDefinitionNames.CHANGE_OGEL_TYPE, ogelQuestions,
        BackLink.to(routes.SummaryController.renderForm(), "Summary"));
  }

  private void goodsCategoryStages() {

    JourneyStage categoryArtsCultural = defineStage("categoryArtsCultural", "Arts and cultural goods",
        controllers.categories.routes.ArtsCulturalController.renderForm());

    JourneyStage categoryArtsCulturalHistoric = defineStage("categoryArtsCulturalHistoric",
        "You may need an Arts Council licence",
        routes.StaticContentController.renderCategoryArtsCulturalHistoric());

    JourneyStage categoryArtsCulturalNonHistoric = defineStage("categoryArtsCulturalNonHistoric",
        "You need an Arts Council licence to export specific items",
        routes.StaticContentController.renderCategoryArtsCulturalNonHistoric());

    JourneyStage categoryArtsCulturalFirearmHistoric = defineStage("categoryArtsCulturalFirearmHistoric",
        "You may need an Arts Council licence, and an export licence",
        controllers.categories.routes.ArtsCulturalFirearmHistoricController.renderForm());

    JourneyStage categoryChemicalsCosmetics = defineStage("categoryChemicalsCosmetics",
        "Cosmetics, chemicals and pesticides", controllers.categories.routes.ChemicalsCosmeticsController.renderForm());

    JourneyStage categoryDualUse = defineStage("categoryDualUse", "Do your items have a dual use?",
        controllers.categories.routes.DualUseController.renderForm());

    JourneyStage categoryFinancialTechnicalAssistance = defineStage("categoryFinancialTechnicalAssistance",
        "You should contact the Export Control Organisation to find out if you need a licence",
        controllers.categories.routes.FinancialTechnicalAssistanceController.renderForm());

    JourneyStage categoryFoodStatic = defineStage("categoryFood", "You need to check the rules for your export destination",
        routes.StaticContentController.renderCategoryFood());

    JourneyStage categoryMedicinesDrugs = defineStage("categoryMedicinesDrugs", "Medicines and drugs",
        controllers.categories.routes.MedicinesDrugsController.renderForm());

    JourneyStage categoryNonMilitary = defineStage("categoryNonMilitary",
        "Will you be taking the firearms or ammunition out of the UK yourself?",
        controllers.categories.routes.NonMilitaryController.renderForm());

    JourneyStage categoryNonMilitaryCheckDestination = defineStage("categoryNonMilitaryCheckDestination",
        "You need to check the rules for your destination country",
        routes.StaticContentController.renderCategoryNonMilitaryFirearmsCheckDestination());

    JourneyStage categoryNonMilitaryNeedExportLicence = defineStage("categoryNonMilitaryNeedExportLicence",
        "You need an export licence", routes.StaticContentController.renderCategoryNonMilitaryFirearmsNeedExportLicence());

    JourneyStage categoryPlantsAnimals = defineStage("categoryPlantsAnimals", "Plants and animals",
        controllers.categories.routes.PlantsAnimalsController.renderForm());

    JourneyStage categoryEndangeredAnimalStatic = defineStage("categoryEndangeredAnimal", "You may need a CITES permit",
        routes.StaticContentController.renderCategoryEndangeredAnimals());

    JourneyStage categoryNonEndangeredAnimalStatic = defineStage("categoryNonEndangeredAnimal",
        "You may need approval from the destination country",
        routes.StaticContentController.renderCategoryNonEndangeredAnimals());

    JourneyStage categoryPlantStatic = defineStage("categoryPlant", "You may need approval from the destination country",
        routes.StaticContentController.renderCategoryPlants());

    JourneyStage categoryMedicinesDrugsStatic = defineStage("categoryMedicinesDrugsStatic",
        "You need a licence to export most drugs and medicines",
        routes.StaticContentController.renderCategoryMedicinesDrugs());

    JourneyStage categoryTortureRestraint = defineStage("categoryTortureRestraint",
        "You may not be allowed to export your goods", controllers.categories.routes.TortureRestraintController.renderForm());

    JourneyStage categoryRadioactive = defineStage("categoryRadioactive",
        "You need a licence to export radioactive materials above certain activity thresholds",
        controllers.categories.routes.RadioactiveController.renderForm());

    JourneyStage categoryWaste = defineStage("categoryWaste", "You must have a licence to export most types of waste",
        routes.StaticContentController.renderCategoryWaste());

    atStage(exportCategory)
        .onEvent(Events.EXPORT_CATEGORY_SELECTED)
        .branch()
        .when(ExportCategory.ARTS_CULTURAL, moveTo(categoryArtsCultural))
        .when(ExportCategory.CHEMICALS_COSMETICS, moveTo(categoryChemicalsCosmetics))
        .when(ExportCategory.DUAL_USE, moveTo(goodsType))
        .when(ExportCategory.FINANCIAL_ASSISTANCE, moveTo(categoryFinancialTechnicalAssistance))
        .when(ExportCategory.FOOD, moveTo(categoryFoodStatic))
        .when(ExportCategory.MEDICINES_DRUGS, moveTo(categoryMedicinesDrugs))
        .when(ExportCategory.MILITARY, moveTo(goodsType))
        .when(ExportCategory.NONE, moveTo(categoryDualUse))
        .when(ExportCategory.NON_MILITARY, moveTo(categoryNonMilitary))
        .when(ExportCategory.PLANTS_ANIMALS, moveTo(categoryPlantsAnimals))
        .when(ExportCategory.RADIOACTIVE, moveTo(categoryRadioactive))
        .when(ExportCategory.TECHNICAL_ASSISTANCE, moveTo(categoryFinancialTechnicalAssistance))
        .when(ExportCategory.TORTURE_RESTRAINT, moveTo(categoryTortureRestraint))
        .when(ExportCategory.WASTE, moveTo(categoryWaste));

    atStage(exportCategory)
        .onEvent(Events.EXPORT_CATEGORY_COULD_BE_DUAL_USE)
        .then(moveTo(categoryDualUse));

    atStage(categoryArtsCultural)
        .onEvent(Events.ARTS_CULTURAL_CATEGORY_SELECTED)
        .branch()
        .when(ArtsCulturalGoodsType.HISTORIC, moveTo(categoryArtsCulturalHistoric))
        .when(ArtsCulturalGoodsType.NON_HISTORIC, moveTo(categoryArtsCulturalNonHistoric))
        .when(ArtsCulturalGoodsType.FIREARM_HISTORIC, moveTo(categoryArtsCulturalFirearmHistoric))
        .when(ArtsCulturalGoodsType.FIREARM_NON_HISTORIC, moveTo(categoryNonMilitary));

    // Note use of EXPORT_CATEGORY_SELECTED for single value
    atStage(categoryArtsCulturalFirearmHistoric)
        .onEvent(Events.EXPORT_CATEGORY_SELECTED)
        .branch()
        .when(ExportCategory.NON_MILITARY, moveTo(categoryNonMilitary));

    atStage(categoryChemicalsCosmetics)
        .onEvent(Events.SEARCH_PHYSICAL_GOODS)
        .then(moveTo(physicalGoodsSearch));

    atStage(categoryDualUse)
        .onEvent(Events.IS_DUAL_USE)
        .branch()
        .when(true, moveTo(goodsType))
        .when(false, moveTo(notApplicable));

    atStage(categoryFinancialTechnicalAssistance)
        .onEvent(StandardEvents.NEXT)
        .then(moveTo(notImplemented)); // TODO This should go through to the technical information search (when implemented)

    atStage(categoryMedicinesDrugs)
        .onEvent(Events.IS_USED_FOR_EXECUTION_TORTURE)
        .branch()
        .when(true, moveTo(categoryTortureRestraint))
        .when(false, moveTo(categoryMedicinesDrugsStatic));

    atStage(categoryNonMilitary)
        .onEvent(Events.NON_MILITARY_FIREARMS_QUESTION_ANSWERERD)
        .branch()
        .when(NonMilitaryFirearmExportBySelfType.YES, moveTo(categoryNonMilitaryCheckDestination))
        .when(NonMilitaryFirearmExportBySelfType.NO_INCLUDED_IN_PERSONAL_EFFECTS, moveTo(categoryNonMilitaryCheckDestination))
        .when(NonMilitaryFirearmExportBySelfType.NO_TRANSFER_TO_THIRD_PARTY, moveTo(categoryNonMilitaryNeedExportLicence));

    atStage(categoryPlantsAnimals)
        .onEvent(Events.LIFE_TYPE_SELECTED)
        .branch()
        .when(LifeType.ENDANGERED, moveTo(categoryEndangeredAnimalStatic))
        .when(LifeType.NON_ENDANGERED, moveTo(categoryNonEndangeredAnimalStatic))
        .when(LifeType.PLANT, moveTo(categoryPlantStatic));

    atStage(categoryTortureRestraint)
        .onEvent(Events.SEARCH_PHYSICAL_GOODS)
        .then(moveTo(physicalGoodsSearch));

    atStage(categoryRadioactive)
        .onEvent(StandardEvents.NEXT)
        .then(moveTo(destinationCountries));

  }

  private void physicalGoodsStages() {

    JourneyStage physicalGoodsSearchResults = defineStage("physicalGoodsSearchResults", "Possible matches",
        controllers.search.routes.PhysicalGoodsSearchResultsController.renderForm());

    JourneyStage controlCode = defineStage("controlCode", "Summary",
        controllers.controlcode.routes.ControlCodeController.renderForm());

    JourneyStage controlCodeNotApplicable = defineStage("controlCodeNotApplicable", "Rating is not applicable",
        controllers.controlcode.routes.NotApplicableController.renderForm(Boolean.FALSE.toString()));

    JourneyStage controlCodeNotApplicableExtended = defineStage("controlCodeNotApplicableExtended", "Rating is not applicable",
        controllers.controlcode.routes.NotApplicableController.renderForm(Boolean.TRUE.toString()));

    JourneyStage additionalSpecifications = defineStage("additionalSpecifications", "Additional specifications",
        controllers.controlcode.routes.AdditionalSpecificationsController.renderForm());

    JourneyStage decontrols = defineStage("decontrols", "Decontrols",
        controllers.controlcode.routes.DecontrolsController.renderForm());

    JourneyStage technicalNotes = defineStage("technicalNotes", "Technical notes",
        controllers.controlcode.routes.TechnicalNotesController.renderForm());

    JourneyStage ogelResults = defineStage("ogelResults", "Licences applicable to your answers",
        controllers.ogel.routes.OgelResultsController.renderForm());

    JourneyStage ogelConditions = defineStage("ogelConditions", "Conditions apply to your licence",
        controllers.ogel.routes.OgelConditionsController.renderForm());

    JourneyStage virtualEU = defineStage("virtualEU", "You do not need a licence",
        routes.StaticContentController.renderVirtualEU());

    JourneyStage ogelSummary = defineStage("ogelSummary", "Licence summary",
        controllers.ogel.routes.OgelSummaryController.renderForm());

    atStage(physicalGoodsSearch)
        .onEvent(Events.SEARCH_PHYSICAL_GOODS)
        .then(moveTo(physicalGoodsSearchResults));

    atStage(physicalGoodsSearchResults)
        .onEvent(Events.CONTROL_CODE_SELECTED)
        .then(moveTo(controlCode));

    atStage(physicalGoodsSearchResults)
        .onEvent(Events.NONE_MATCHED)
        .then(moveTo(notApplicable));

    atStage(controlCode)
        .onEvent(Events.CONTROL_CODE_FLOW_NEXT)
        .branch()
        .when(ControlCodeFlowStage.NOT_APPLICABLE, moveTo(controlCodeNotApplicable)) //4
        .when(ControlCodeFlowStage.ADDITIONAL_SPECIFICATIONS, moveTo(additionalSpecifications))
        .when(ControlCodeFlowStage.DECONTROLS, moveTo(decontrols))
        .when(ControlCodeFlowStage.TECHNICAL_NOTES, moveTo(technicalNotes))
        .when(ControlCodeFlowStage.CONFIRMED, moveTo(destinationCountries))
        .when(ControlCodeFlowStage.BACK_TO_SEARCH, moveTo(physicalGoodsSearch))
        .when(ControlCodeFlowStage.BACK_TO_SEARCH_RESULTS, moveTo(physicalGoodsSearchResults));

    atStage(additionalSpecifications)
        .onEvent(Events.CONTROL_CODE_FLOW_NEXT)
        .branch()
        .when(ControlCodeFlowStage.NOT_APPLICABLE, moveTo(controlCodeNotApplicableExtended))
        .when(ControlCodeFlowStage.DECONTROLS, moveTo(decontrols))
        .when(ControlCodeFlowStage.TECHNICAL_NOTES, moveTo(technicalNotes))
        .when(ControlCodeFlowStage.CONFIRMED, moveTo(destinationCountries));

    atStage(decontrols)
        .onEvent(Events.CONTROL_CODE_FLOW_NEXT)
        .branch()
        .when(ControlCodeFlowStage.NOT_APPLICABLE, moveTo(controlCodeNotApplicableExtended)) // 5
        .when(ControlCodeFlowStage.TECHNICAL_NOTES, moveTo(technicalNotes))
        .when(ControlCodeFlowStage.CONFIRMED, moveTo(destinationCountries));

    atStage(technicalNotes)
        .onEvent(Events.CONTROL_CODE_FLOW_NEXT)
        .branch()
        .when(ControlCodeFlowStage.NOT_APPLICABLE, moveTo(controlCodeNotApplicableExtended))
        .when(ControlCodeFlowStage.CONFIRMED, moveTo(destinationCountries));

    atStage(controlCodeNotApplicable)
        .onEvent(Events.CONTROL_CODE_FLOW_NEXT)
        .branch()
        .when(ControlCodeFlowStage.BACK_TO_SEARCH, moveTo(physicalGoodsSearch))
        .when(ControlCodeFlowStage.BACK_TO_SEARCH_RESULTS, moveTo(physicalGoodsSearchResults));

    atStage(controlCodeNotApplicableExtended)
        .onEvent(Events.CONTROL_CODE_FLOW_NEXT)
        .branch()
        .when(ControlCodeFlowStage.BACK_TO_SEARCH, moveTo(physicalGoodsSearch))
        .when(ControlCodeFlowStage.BACK_TO_SEARCH_RESULTS, moveTo(physicalGoodsSearchResults));

    atStage(destinationCountries)
        .onEvent(Events.DESTINATION_COUNTRIES_SELECTED)
        .then(moveTo(ogelQuestions));

    atStage(ogelQuestions)
        .onEvent(Events.VIRTUAL_EU_OGEL_STAGE)
        .branch()
        .when(VirtualEUOgelStage.NO_VIRTUAL_EU, moveTo(ogelResults))
        .when(VirtualEUOgelStage.VIRTUAL_EU_WITH_CONDITIONS, moveTo(ogelConditions))
        .when(VirtualEUOgelStage.VIRTUAL_EU_WITHOUT_CONDITIONS, moveTo(virtualEU));

    atStage(ogelResults)
        .onEvent(Events.OGEL_SELECTED)
        .then(moveTo(ogelSummary));

    atStage(ogelResults)
        .onEvent(Events.OGEL_CONDITIONS_APPLY)
        .then(moveTo(ogelConditions));

    atStage(ogelConditions)
        .onEvent(Events.OGEL_DO_CONDITIONS_APPLY)
        .then(moveTo(ogelSummary));

    atStage(ogelConditions)
        .onEvent(Events.VIRTUAL_EU_OGEL_STAGE)
        .branch()
        .when(VirtualEUOgelStage.VIRTUAL_EU_CONDITIONS_DO_APPLY, moveTo(virtualEU))
        .when(VirtualEUOgelStage.VIRTUAL_EU_CONDITIONS_DO_NOT_APPLY, moveTo(ogelResults));

    atStage(ogelSummary)
        .onEvent(Events.OGEL_CHOOSE_AGAIN)
        .then(moveTo(ogelResults));
  }

  private void softwareStages() {
    JourneyStage dualUseSoftwareCategories = defineStage("dualUseSoftwareCategories", "What is your software for?",
        controllers.software.routes.DualUseSoftwareCategoriesController.renderForm());

    JourneyStage relatedToEquipmentOrMaterials = defineStage("relatedToEquipmentOrMaterials", "Is your software any of the following?",
        controllers.software.routes.RelatedEquipmentController.renderForm());

    JourneyStage categoryControls = defineStage("categoryControls", "Showing controls related to software category",
        controllers.software.controls.routes.CategoryControlsController.renderForm());

    atStage(softwareExemptions)
        .onEvent(StandardEvents.YES)
        .then(moveTo(notApplicable)); // TODO check this is the correct NLR to show

    atStage(softwareExemptions)
        .onEvent(StandardEvents.NO)
        .branchWith(() -> softwareExemptionsFlow(permissionsFinderDao.getExportCategory().get()))
        .when(SoftwareExemptionsFlow.DUAL_USE, moveTo(dualUseSoftwareCategories))
        .when(SoftwareExemptionsFlow.MILITARY_ZERO_CONTROLS, moveTo(relatedToEquipmentOrMaterials))
        .when(SoftwareExemptionsFlow.MILITARY_ONE_CONTROL, moveTo(notImplemented))
        .when(SoftwareExemptionsFlow.MILITARY_GREATER_THAN_ONE_CONTROL, moveTo(categoryControls));

    atStage(dualUseSoftwareCategories)
        .onEvent(Events.DUAL_USE_SOFTWARE_CATEGORY_SELECTED)
        .branchWith(() -> checkSoftwareControls(permissionsFinderDao.getDualUseSoftwareCategory().get()))
        .when(ApplicableSoftwareControls.ZERO, moveTo(relatedToEquipmentOrMaterials))
        .when(ApplicableSoftwareControls.ONE, moveTo(notImplemented))
        .when(ApplicableSoftwareControls.GREATER_THAN_ONE, moveTo(categoryControls));

    atStage(relatedToEquipmentOrMaterials)
        .onEvent(StandardEvents.YES).then(moveTo(notImplemented));

    atStage(relatedToEquipmentOrMaterials)
        .onEvent(StandardEvents.NO).then(moveTo(notImplemented));

    atStage(categoryControls)
        .onEvent(Events.CONTROL_CODE_SELECTED)
        .then(moveTo(notImplemented));

    atStage(categoryControls)
        .onEvent(Events.NONE_MATCHED)
        .then(moveTo(relatedToEquipmentOrMaterials));
  }

  private SoftwareExemptionsFlow softwareExemptionsFlow(ExportCategory exportCategory) {
    if (exportCategory == ExportCategory.MILITARY) {
      ApplicableSoftwareControls controls = checkSoftwareControls(SoftwareCategory.MILITARY);
      if (controls == ApplicableSoftwareControls.ZERO) {
        return SoftwareExemptionsFlow.MILITARY_ZERO_CONTROLS;
      }
      else if (controls == ApplicableSoftwareControls.ONE) {
        return SoftwareExemptionsFlow.MILITARY_ONE_CONTROL;
      }
      else if (controls == ApplicableSoftwareControls.GREATER_THAN_ONE) {
        return SoftwareExemptionsFlow.MILITARY_GREATER_THAN_ONE_CONTROL;
      }
      else {
        throw new RuntimeException(String.format("Unexpected member of ApplicableSoftwareControls enum: \"%s\""
            , controls.toString()));
      }
    }
    else if (exportCategory == ExportCategory.DUAL_USE) {
      return SoftwareExemptionsFlow.DUAL_USE;
    }
    else {
      throw new RuntimeException(String.format("Unexpected member of ExportCategory enum: \"%s\""
          , exportCategory.toString()));
    }
  }


  private ApplicableSoftwareControls checkSoftwareControls(SoftwareCategory softwareCategory) {
    // TODO Move to Utility class, call in controllers and use CategoryControlsServiceClient
    if (softwareCategory == SoftwareCategory.MILITARY) {
      return ApplicableSoftwareControls.ZERO;
    }
    else if (softwareCategory == SoftwareCategory.DUMMY) {
      return ApplicableSoftwareControls.ONE;
    }
    else if (softwareCategory == SoftwareCategory.RADIOACTIVE) {
      return ApplicableSoftwareControls.GREATER_THAN_ONE;
    }
    else {
      throw new RuntimeException(String.format("Unexpected member of SoftwareCategory enum: \"%s\""
          , softwareCategory.toString()));
    }
  }

}
