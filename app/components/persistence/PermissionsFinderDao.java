package components.persistence;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.journey.JourneySerialiser;
import components.common.persistence.CommonRedisDao;
import components.common.persistence.RedisKeyConfig;
import components.common.transaction.TransactionManager;
import controllers.categories.ArtsCulturalController.ArtsCulturalForm;
import controllers.ogel.OgelQuestionsController.OgelQuestionsForm;
import controllers.search.SearchController.ControlCodeSearchForm;
import models.ExportCategory;
import models.GoodsType;
import models.LifeType;
import models.TradeType;
import models.controlcode.ControlCodeJourney;
import models.softtech.SoftTechCategory;
import org.apache.commons.lang3.StringUtils;
import play.libs.Json;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class PermissionsFinderDao extends CommonRedisDao implements JourneySerialiser {

  public static final String JOURNEY = "journey";
  public static final String SOURCE_COUNTRY = "sourceCountry";
  public static final String SELECTED_CONTROL_CODE = "selectedControlCode";
  public static final String CONFIRMED_CONTROL_CODE = "confirmedControlCode";
  public static final String OGEL_ID = "ogelId";
  public static final String EXPORT_CATEGORY = "exportCategory";
  public static final String APPLICATION_CODE = "applicationCode";
  public static final String EMAIL_ADDRESS = "emailAddress";
  public static final String PHYSICAL_GOOD_SEARCH_PAGINATION_DISPLAY_COUNT = "physicalGoodSearchPaginationDisplayCount";
  public static final String PHYSICAL_GOOD_SEARCH_LAST_CHOSEN_CONTROL_CODE = "physicalGoodSearchLastChosenControlCode";
  public static final String TRADE_TYPE = "tradeType";
  public static final String ARTS_CULTURAL_GOODS = "artsCulturalGoods";
  public static final String IS_DUAL_USE_GOOD = "isDualUseGood";
  public static final String IS_USED_FOR_EXECUTION_TORTURE = "isUsedForExecutionTorture";
  public static final String PLANTS_ANIMALS_LIFE_TYPE = "plantsAnimalsLifeType";
  public static final String GOODS_TYPE = "goodsType";
  public static final String PHYSICAL_GOOD_SEARCH = "physicalGoodSearch";
  public static final String OGEL_QUESTIONS = "ogelQuestions";
  public static final String OGEL_CONDITIONS_APPLY = "ogelConditionsApply";
  public static final String ITEM_THROUGH_MULTIPLE_COUNTRIES = "itemThroughMultipleCountries";
  public static final String FINAL_DESTINATION_COUNTRY = "finalDestinationCountry";
  public static final String THROUGH_DESTINATION_COUNTRY_LIST = "throughDestinationCountryList";
  public static final String OGEL_REGISTRATION_SERVICE_TRANSACTION_EXISTS = "ogelRegistrationServiceTransactionExists";
  public static final String NON_MILITARY_FIREARMS_EXPORTED_BY_SELF = "nonMilitaryFirearmsExportedBySelf";
  public static final String CONTROL_CODE_APPLIES = "controlCodeAdditionalApplies";
  public static final String CONTROL_CODE_ADDITIONAL_SPECIFICATIONS_APPLY = "controlCodeAdditionalSpecificationsApply";
  public static final String CONTROL_CODE_DECONTROLS_APPLY = "controlCodeDecontrolsApply";
  public static final String CONTROL_CODE_TECHNICAL_NOTES_APPLY = "controlCodeTechnicalNotesApply";
  public static final String DO_EXEMPTIONS_APPLY_Q1 = "doExemptionsApplyQ1";
  public static final String DO_EXEMPTIONS_APPLY_Q2 = "doExemptionsApplyQ2";
  public static final String SOFT_TECH_CATEGORY = "softTechCategory";
  public static final String RELATED_TO_EQUIPMENT_OR_MATERIALS = "relatedToEquipmentOrMaterials";
  public static final String SOFTWARE_IS_COVERED_BY_TECHNOLOGY_RELATIONSHIP = "softwareIsCoveredByTechnologyRelationship";

  @Inject
  public PermissionsFinderDao(@Named("permissionsFinderDaoHash") RedisKeyConfig keyConfig, JedisPool pool, TransactionManager transactionManager) {
    super(keyConfig, pool, transactionManager);
  }

  public void saveSelectedControlCode(ControlCodeJourney controlCodeJourney, String selectedControlCode) {
    writeString(prependFieldName(controlCodeJourney, SELECTED_CONTROL_CODE), selectedControlCode);
  }

  public String getSelectedControlCode(ControlCodeJourney controlCodeJourney) {
    return readString(prependFieldName(controlCodeJourney, SELECTED_CONTROL_CODE));
  }

  public void saveConfirmedControlCode(String confirmedControlCode) {
    writeString(CONFIRMED_CONTROL_CODE, confirmedControlCode);
  }

  public String getConfirmedControlCode() {
    return readString(CONFIRMED_CONTROL_CODE);
  }

  public void saveSourceCountry(String sourceCountry) {
    writeString(SOURCE_COUNTRY, sourceCountry);
  }

  public String getSourceCountry() {
    return readString(SOURCE_COUNTRY);
  }

  public void saveOgelId(String ogelId) {
    writeString(OGEL_ID, ogelId);
  }

  public String getOgelId() {
    return readString(OGEL_ID);
  }

  public void saveExportCategory(ExportCategory exportCategory) {
    writeString(EXPORT_CATEGORY, exportCategory.value());
  }

  public Optional<ExportCategory> getExportCategory() {
    return ExportCategory.getMatched(readString(EXPORT_CATEGORY));
  }

  public void saveApplicationCode(String applicationCode) {
    writeString(APPLICATION_CODE, applicationCode);
  }

  public String getApplicationCode() {
    return readString(APPLICATION_CODE);
  }

  public void saveEmailAddress(String emailAddress) {
    writeString(EMAIL_ADDRESS, emailAddress);
  }

  public String getEmailAddress() {
    return readString(EMAIL_ADDRESS);
  }

  public void savePhysicalGoodSearchPaginationDisplayCount(ControlCodeJourney controlCodeJourney, int physicalGoodSearchPaginationDisplayCount) {
    writeString(prependFieldName(controlCodeJourney, PHYSICAL_GOOD_SEARCH_PAGINATION_DISPLAY_COUNT), Integer.toString(physicalGoodSearchPaginationDisplayCount));
  }

  public Optional<Integer> getPhysicalGoodSearchPaginationDisplayCount(ControlCodeJourney controlCodeJourney) {
    String count = readString(prependFieldName(controlCodeJourney, PHYSICAL_GOOD_SEARCH_PAGINATION_DISPLAY_COUNT));
    if (count != null) {
      return Optional.of(Integer.parseInt(count));
    }
    else {
      return Optional.empty();
    }
  }

  public void savePhysicalGoodSearchLastChosenControlCode(ControlCodeJourney controlCodeJourney, String controlCode) {
    writeString(prependFieldName(controlCodeJourney, PHYSICAL_GOOD_SEARCH_LAST_CHOSEN_CONTROL_CODE), controlCode);
  }

  public String getPhysicalGoodSearchLastChosenControlCode(ControlCodeJourney controlCodeJourney) {
    return readString(prependFieldName(controlCodeJourney, PHYSICAL_GOOD_SEARCH_LAST_CHOSEN_CONTROL_CODE));
  }

  public void clearPhysicalGoodSearchLastChosenControlCode(ControlCodeJourney controlCodeJourney) {
    deleteString(prependFieldName(controlCodeJourney, PHYSICAL_GOOD_SEARCH_LAST_CHOSEN_CONTROL_CODE));
  }

  public void saveTradeType(TradeType tradeType) {
    writeString(TRADE_TYPE, tradeType.toString());
  }

  public Optional<TradeType> getTradeType() {
    String tradeType = readString(TRADE_TYPE);
    return StringUtils.isBlank(tradeType) ? Optional.empty() : Optional.of(TradeType.valueOf(tradeType));
  }

  public void saveArtsCulturalForm (ArtsCulturalForm form) {
    writeObject(ARTS_CULTURAL_GOODS, form);
  }

  public Optional<ArtsCulturalForm> getArtsCulturalForm() {
    return readObject(ARTS_CULTURAL_GOODS, ArtsCulturalForm.class);
  }

  public void saveIsDualUseGood(boolean isDualUseGood) {
    writeBoolean(IS_DUAL_USE_GOOD, isDualUseGood);
  }

  public Optional<Boolean> getIsDualUseGood() {
    return readBoolean(IS_DUAL_USE_GOOD);
  }

  public void saveIsUsedForExecutionTorture(boolean isUsedForExecutionTorture) {
    writeBoolean(IS_USED_FOR_EXECUTION_TORTURE, isUsedForExecutionTorture);
  }

  public Optional<Boolean> getIsUsedForExecutionTorture() {
    return readBoolean(IS_USED_FOR_EXECUTION_TORTURE);
  }

  public void writeBoolean(String fieldName, boolean value){
    writeString(fieldName, Boolean.toString(value));
  }

  public Optional<Boolean> readBoolean(String fieldName) {
    String value = readString(fieldName);
    if (value == null || value.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(value.equalsIgnoreCase("true"));
  }

  public void savePlantsAnimalsLifeType(LifeType lifeType) {
    writeString(PLANTS_ANIMALS_LIFE_TYPE, lifeType.value());
  }

  public Optional<LifeType> getPlantsAnimalsLifeType() {
    return LifeType.getMatched(readString(PLANTS_ANIMALS_LIFE_TYPE));
  }

  public void saveGoodsType(GoodsType goodsType) {
    writeString(GOODS_TYPE, goodsType.value());
  }

  public Optional<GoodsType> getGoodsType() {
    return GoodsType.getMatchedByValue(readString(GOODS_TYPE));
  }

  public void clearGoodsType() {
    deleteString(GOODS_TYPE);
  }

  public void savePhysicalGoodSearchForm(ControlCodeJourney controlCodeJourney, ControlCodeSearchForm controlCodeSearchForm) {
    writeObject(prependFieldName(controlCodeJourney, PHYSICAL_GOOD_SEARCH), controlCodeSearchForm);
  }

  public Optional<ControlCodeSearchForm> getPhysicalGoodsSearchForm(ControlCodeJourney controlCodeJourney) {
    return readObject(prependFieldName(controlCodeJourney, PHYSICAL_GOOD_SEARCH), ControlCodeSearchForm.class);
  }

  public void saveOgelQuestionsForm(OgelQuestionsForm ogelQuestionsForm) {
    writeObject(OGEL_QUESTIONS, ogelQuestionsForm);
  }

  public Optional<OgelQuestionsForm> getOgelQuestionsForm() {
    return readObject(OGEL_QUESTIONS, OgelQuestionsForm.class);
  }

  public void saveOgelConditionsApply(boolean ogelConditionsApply) {
    writeBoolean(OGEL_CONDITIONS_APPLY, ogelConditionsApply);
  }

  public Optional<Boolean> getOgelConditionsApply() {
    return readBoolean(OGEL_CONDITIONS_APPLY);
  }

  public void saveItemThroughMultipleCountries (boolean itemThroughMultipleCountries) {
    writeBoolean(ITEM_THROUGH_MULTIPLE_COUNTRIES, itemThroughMultipleCountries);
  }

  public Optional<Boolean> getItemThroughMultipleCountries() {
    return readBoolean(ITEM_THROUGH_MULTIPLE_COUNTRIES);
  }

  public void saveFinalDestinationCountry(String finalDestinationCountry) {
    writeString(FINAL_DESTINATION_COUNTRY, finalDestinationCountry);
  }

  public String getFinalDestinationCountry() {
    return readString(FINAL_DESTINATION_COUNTRY);
  }

  public void saveThroughDestinationCountries(List<String> throughDestinationCountries) {
    writeObject(THROUGH_DESTINATION_COUNTRY_LIST, throughDestinationCountries);
  }

  public List<String> getThroughDestinationCountries() {
    String countriesJson = readString(THROUGH_DESTINATION_COUNTRY_LIST);
    if (countriesJson == null || countriesJson.isEmpty()) {
      return Collections.emptyList();
    }
    else {
      return new LinkedList<>(Arrays.asList(Json.fromJson(Json.parse(countriesJson), String[].class)));
    }
  }

  @Override
  public String readJourneyString() {
    return readString(JOURNEY);
  }

  @Override
  public void writeJourneyString(String journeyString) {
    writeString(JOURNEY, journeyString);
  }

  public void saveOgelRegistrationServiceTransactionExists (boolean transactionCreated) {
    writeBoolean(OGEL_REGISTRATION_SERVICE_TRANSACTION_EXISTS, transactionCreated);
  }

  public Optional<Boolean> getOgelRegistrationServiceTransactionExists() {
    return readBoolean(OGEL_REGISTRATION_SERVICE_TRANSACTION_EXISTS);
  }

  public void saveNonMilitaryFirearmsExportedBySelf(String nonMilitaryFirearmsExportedBySelf) {
    writeString(NON_MILITARY_FIREARMS_EXPORTED_BY_SELF, nonMilitaryFirearmsExportedBySelf);
  }

  public String readNonMilitaryFirearmsExportedBySelf() {
    return readString(NON_MILITARY_FIREARMS_EXPORTED_BY_SELF);
  }

  public void saveControlCodeApplies(ControlCodeJourney controlCodeJourney, boolean controlCodeApplies) {
    writeBoolean(prependFieldName(controlCodeJourney, CONTROL_CODE_APPLIES), controlCodeApplies);
  }

  public Optional<Boolean> getControlCodeApplies(ControlCodeJourney controlCodeJourney) {
    return readBoolean(prependFieldName(controlCodeJourney, CONTROL_CODE_APPLIES));
  }

  public void clearControlCodeApplies(ControlCodeJourney controlCodeJourney) {
    deleteString(prependFieldName(controlCodeJourney, CONTROL_CODE_APPLIES));
  }

  public void saveControlCodeDecontrolsApply(ControlCodeJourney controlCodeJourney, boolean decontrolsApply) {
    writeBoolean(prependFieldName(controlCodeJourney, CONTROL_CODE_DECONTROLS_APPLY), decontrolsApply);
  }

  public Optional<Boolean> getControlCodeDecontrolsApply(ControlCodeJourney controlCodeJourney) {
    return readBoolean(prependFieldName(controlCodeJourney, CONTROL_CODE_DECONTROLS_APPLY));
  }

  public void clearControlCodeDecontrolsApply(ControlCodeJourney controlCodeJourney) {
    deleteString(prependFieldName(controlCodeJourney, CONTROL_CODE_DECONTROLS_APPLY));
  }

  public void saveControlCodeAdditionalSpecificationsApply(ControlCodeJourney controlCodeJourney, boolean additionalSpecificationsApply) {
    writeBoolean(prependFieldName(controlCodeJourney, CONTROL_CODE_ADDITIONAL_SPECIFICATIONS_APPLY), additionalSpecificationsApply);
  }

  public Optional<Boolean> getControlCodeAdditionalSpecificationsApply(ControlCodeJourney controlCodeJourney) {
    return readBoolean(prependFieldName(controlCodeJourney, CONTROL_CODE_ADDITIONAL_SPECIFICATIONS_APPLY));
  }

  public void clearControlCodeAdditionalSpecificationsApply(ControlCodeJourney controlCodeJourney) {
    deleteString(prependFieldName(controlCodeJourney, CONTROL_CODE_ADDITIONAL_SPECIFICATIONS_APPLY));
  }

  public void saveControlCodeTechnicalNotesApply(ControlCodeJourney controlCodeJourney, boolean technicalNotesApply) {
    writeBoolean(prependFieldName(controlCodeJourney, CONTROL_CODE_TECHNICAL_NOTES_APPLY), technicalNotesApply);
  }

  public Optional<Boolean> getControlCodeTechnicalNotesApply(ControlCodeJourney controlCodeJourney) {
    return readBoolean(prependFieldName(controlCodeJourney, CONTROL_CODE_TECHNICAL_NOTES_APPLY));
  }

  public void clearControlCodeTechnicalNotesApply(ControlCodeJourney controlCodeJourney) {
    deleteString(prependFieldName(controlCodeJourney, CONTROL_CODE_TECHNICAL_NOTES_APPLY));
  }

  public void saveDoExemptionsApplyQ1(String doExemptionsApply) {
    writeString(DO_EXEMPTIONS_APPLY_Q1, doExemptionsApply);
  }

  public String getDoExemptionsApplyQ1() {
    return readString(DO_EXEMPTIONS_APPLY_Q1);
  }

  public void saveDoExemptionsApplyQ2(String doExemptionsApply) {
    writeString(DO_EXEMPTIONS_APPLY_Q2, doExemptionsApply);
  }

  public String getDoExemptionsApplyQ2() {
    return readString(DO_EXEMPTIONS_APPLY_Q2);
  }

  public void saveSoftTechCategory(GoodsType goodsType, SoftTechCategory softTechCategory) {
    writeString(prependFieldName(goodsType, SOFT_TECH_CATEGORY), softTechCategory.toString());
  }

  public Optional<SoftTechCategory> getSoftTechCategory(GoodsType goodsType) {
    String softwareCategory = readString(prependFieldName(goodsType, SOFT_TECH_CATEGORY));
    if (StringUtils.isEmpty(softwareCategory)) {
      return Optional.empty();
    }
    else {
      try {
        return Optional.of(SoftTechCategory.valueOf(softwareCategory));
      }
      catch (IllegalArgumentException e) {
        return Optional.empty();
      }
    }
  }

  public void saveRelatedToEquipmentOrMaterials(GoodsType goodsType, Boolean relatedToEquipmentOrMaterials) {
    writeString(prependFieldName(goodsType.urlString(), RELATED_TO_EQUIPMENT_OR_MATERIALS), relatedToEquipmentOrMaterials.toString());
  }

  public Optional<Boolean> getRelatedToEquipmentOrMaterials(GoodsType goodsType) {
    return readBoolean(prependFieldName(goodsType.urlString(), RELATED_TO_EQUIPMENT_OR_MATERIALS));
  }

  public String prependFieldName(ControlCodeJourney controlCodeJourney, String fieldName) {
    return prependFieldName(controlCodeJourney.value(), fieldName);
  }

  public String prependFieldName(GoodsType goodsType, String fieldName) {
    return prependFieldName(goodsType.urlString(), fieldName);
  }

  public String prependFieldName(String prefix, String fieldName) {
    return prefix + ":" + fieldName;
  }

  public void saveSoftwareIsCoveredByTechnologyRelationship(Boolean isCoveredByRelationship) {
    writeBoolean(SOFTWARE_IS_COVERED_BY_TECHNOLOGY_RELATIONSHIP, isCoveredByRelationship);
  }

  public Optional<Boolean> getSoftwareIsCoveredByTechnologyRelationship() {
    return readBoolean(SOFTWARE_IS_COVERED_BY_TECHNOLOGY_RELATIONSHIP);
  }

  public void clearControlCodeJourneyDaoFields(ControlCodeJourney controlCodeJourney) {
    clearControlCodeApplies(controlCodeJourney);
    clearControlCodeDecontrolsApply(controlCodeJourney);
    clearControlCodeAdditionalSpecificationsApply(controlCodeJourney);
    clearControlCodeTechnicalNotesApply(controlCodeJourney);
  }

  public void clearAndUpdateControlCodeJourneyDaoFieldsIfChanged(ControlCodeJourney controlCodeJourney, String newSelectedControlCode) {
    String oldSelectedControlCode = getSelectedControlCode(controlCodeJourney);
    if (!StringUtils.equals(newSelectedControlCode, oldSelectedControlCode)) {
      clearControlCodeJourneyDaoFields(controlCodeJourney);
      saveSelectedControlCode(controlCodeJourney,newSelectedControlCode);
    }
  }

}
