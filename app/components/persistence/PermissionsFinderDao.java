package components.persistence;

import com.google.inject.Inject;
import components.common.journey.Journey;
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
import play.libs.Json;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class PermissionsFinderDao extends CommonRedisDao implements JourneySerialiser {

  public static final String JOURNEY = "journey";

  public static final String PHYSICAL_GOOD_CONTROL_CODE = "physicalGoodControlCode";

  public static final String SOURCE_COUNTRY = "sourceCountry";

  public static final String OGEL_ID = "ogelId";

  public static final String EXPORT_CATEGORY = "exportCategory";

  public static final String APPLICATION_CODE = "applicationCode";

  public static final String EMAIL_ADDRESS = "emailAddress";

  public static final String MEMORABLE_WORD = "memorableWord";

  public static final String PHYSICAL_GOOD_SEARCH_PAGINATION_DISPLAY_COUNT = "physicalGoodSearchPaginationDisplayCount";

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

  @Inject
  public PermissionsFinderDao(RedisKeyConfig keyConfig, JedisPool pool, TransactionManager transactionManager) {
    super(keyConfig, pool, transactionManager);
  }

  public void savePhysicalGoodControlCode(String physicalGoodControlCode) {
    writeString(PHYSICAL_GOOD_CONTROL_CODE, physicalGoodControlCode);
  }

  public String getPhysicalGoodControlCode() {
    return readString(PHYSICAL_GOOD_CONTROL_CODE);
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

  public void saveMemorableWord(String memorableWord) {
    writeString(MEMORABLE_WORD, memorableWord);
  }

  public String getMemorableWord() {
    return readString(MEMORABLE_WORD);
  }

  public void savePhysicalGoodSearchPaginationDisplayCount(int physicalGoodSearchPaginationDisplayCount) {
    writeString(PHYSICAL_GOOD_SEARCH_PAGINATION_DISPLAY_COUNT, Integer.toString(physicalGoodSearchPaginationDisplayCount));
  }

  public int getPhysicalGoodSearchPaginationDisplayCount() {
    return Integer.parseInt(readString(PHYSICAL_GOOD_SEARCH_PAGINATION_DISPLAY_COUNT));
  }

  public void saveTradeType(TradeType tradeType) {
    writeString(TRADE_TYPE, tradeType.value());
  }

  public Optional<TradeType> getTradeType() {
    return TradeType.getMatched(readString(TRADE_TYPE));
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
    return GoodsType.getMatched(readString(GOODS_TYPE));
  }

  public void savePhysicalGoodSearchForm(ControlCodeSearchForm controlCodeSearchForm) {
    writeObject(PHYSICAL_GOOD_SEARCH, controlCodeSearchForm);
  }

  public Optional<ControlCodeSearchForm> getPhysicalGoodsSearchForm() {
    return readObject(PHYSICAL_GOOD_SEARCH, ControlCodeSearchForm.class);
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
  public Journey readJourney() {
    return Journey.fromString(readString(JOURNEY));
  }

  @Override
  public void writeJourney(Journey journey) {
    writeString(JOURNEY, journey.serialiseToString());
  }

}
