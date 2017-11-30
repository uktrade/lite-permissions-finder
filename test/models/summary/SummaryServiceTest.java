package models.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import components.common.cache.CountryProvider;
import components.common.state.ContextParamManager;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.frontend.FrontendServiceClient;
import components.services.controlcode.frontend.FrontendServiceResult;
import components.services.ogels.applicable.ApplicableOgelServiceClient;
import components.services.ogels.ogel.OgelServiceClient;
import controllers.ogel.OgelQuestionsController;
import models.common.Country;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
import uk.gov.bis.lite.ogel.api.view.ApplicableOgelView;
import uk.gov.bis.lite.ogel.api.view.OgelFullView;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
public class SummaryServiceTest {
  private static final String APPLICATION_CODE = "RV8K-ZR17";
  private static final String DESTINATION_COUNTRY = "CTRY3";
  private static final String SOURCE_COUNTRY = "CTRY0";
  private static final String CONTROL_CODE = "ML12b";
  private static final String OGEL_ID = "OGL8";

  @Mock
  private ContextParamManager cpm;
  @Mock
  private PermissionsFinderDao dao;
  @Mock
  private FrontendServiceClient frontendServiceClient;
  @Mock
  private OgelServiceClient ogelServiceClient;
  @Mock
  private ApplicableOgelServiceClient applicableOgelServiceClient;
  @Mock
  private CountryProvider countryProvider;

  private SummaryService summaryService;

  @Before
  public void setUp() throws Exception {
    JsonNode frontendControlCodesJson = Json.parse(this.getClass().getClassLoader().getResourceAsStream("models/summary/frontend-control-codes.json"));
    FrontendServiceResult frontendServiceResult = new FrontendServiceResult(frontendControlCodesJson);

    JsonNode applicableOgelsJson = Json.parse(this.getClass().getClassLoader().getResourceAsStream("models/summary/applicable-ogels.json"));
    List<ApplicableOgelView> applicableOgelViews = Stream.of(Json.fromJson(applicableOgelsJson, ApplicableOgelView[].class)).collect(Collectors.toList());

    JsonNode ogelsJson = Json.parse(this.getClass().getClassLoader().getResourceAsStream("models/summary/ogels.json"));
    OgelFullView ogelFullView = Json.fromJson(ogelsJson, OgelFullView.class);

    Country country = new Country() {
      @Override
      public String getCountryRef() {
        return DESTINATION_COUNTRY;
      }
    };

    OgelQuestionsController.OgelQuestionsForm ogelQuestionsForm = new OgelQuestionsController.OgelQuestionsForm();
    ogelQuestionsForm.forExhibitionDemonstration = "true";
    ogelQuestionsForm.forRepairReplacement = "true";

    when(cpm.addParamsToCall(any(Call.class))).thenReturn("http://some-url");
    when(dao.getApplicationCode()).thenReturn(APPLICATION_CODE);
    when(dao.getOgelId()).thenReturn(OGEL_ID);
    when(dao.getControlCodeForRegistration()).thenReturn(CONTROL_CODE);
    when(dao.getFinalDestinationCountry()).thenReturn(DESTINATION_COUNTRY);
    when(dao.getThroughDestinationCountries()).thenReturn(Collections.emptyList());
    when(dao.getSourceCountry()).thenReturn(SOURCE_COUNTRY);
    when(dao.getOgelQuestionsForm()).thenReturn(Optional.of(ogelQuestionsForm));

    when(frontendServiceClient.get(anyString())).thenReturn(CompletableFuture.completedFuture(frontendServiceResult));

    when(ogelServiceClient.get(anyString())).thenReturn(CompletableFuture.completedFuture(ogelFullView));

    when(applicableOgelServiceClient.get(anyString(), anyString(), anyList(), anyList()))
        .thenReturn(CompletableFuture.completedFuture(applicableOgelViews));

    when(countryProvider.getCountries()).thenReturn(Collections.singletonList(country));

    summaryService = new SummaryServiceImpl(cpm, dao, new HttpExecutionContext(Runnable::run), frontendServiceClient, ogelServiceClient, applicableOgelServiceClient, countryProvider);
  }

  @Test
  public void summaryIsGenerated() throws Exception {
    Summary summary = summaryService.composeSummary().toCompletableFuture().get();
    assertThat(summary.applicationCode).isEqualTo(APPLICATION_CODE);
    assertThat(summary.isValid()).isTrue();
    assertThat(summary.summaryFields.isEmpty()).isFalse();
    assertThat(summary.summaryFields.size()).isEqualTo(3);
  }

  @Test
  public void ogelSummaryFieldIsGenerated() throws Exception {
    Summary summary = summaryService.composeSummary().toCompletableFuture().get();
    Optional<SummaryField> field = summary.findSummaryField(SummaryFieldType.OGEL_TYPE);
    assertThat(field.isPresent()).isTrue();
  }

  @Test
  public void destinationCountriesFieldIsGenerated() throws Exception {
    Summary summary = summaryService.composeSummary().toCompletableFuture().get();
    Optional<SummaryField> field = summary.findSummaryField(SummaryFieldType.DESTINATION_COUNTRIES);
    assertThat(field.isPresent()).isTrue();
  }

  @Test
  public void controlCodeFieldIsGenerated() throws Exception {
    Summary summary = summaryService.composeSummary().toCompletableFuture().get();
    Optional<SummaryField> field = summary.findSummaryField(SummaryFieldType.CONTROL_CODE);
    assertThat(field.isPresent()).isTrue();
  }

  @Test
  public void summaryFieldsAreValidated() throws Exception {
    Summary summary = summaryService.composeSummary().toCompletableFuture().get();
    assertThat(summary.summaryFields.size()).isEqualTo(3);
    assertThat(summary.isValid()).isTrue();

    // Add invalid field
    SummaryField invalidField = mock(SummaryField.class);
    when(invalidField.isValid()).thenReturn(false);
    summary.addSummaryField(invalidField);

    assertThat(summary.summaryFields.size()).isEqualTo(4);
    assertThat(summary.isValid()).isFalse();
  }

  @Test
  public void emptySummaryIsValid() throws Exception {
    Summary summary = new Summary(APPLICATION_CODE);
    assertThat(summary.summaryFields.isEmpty()).isTrue();
    assertThat(summary.isValid()).isTrue();
  }

  @Test
  public void summaryIsValidWithValidField() throws Exception {
    SummaryField validField = mock(SummaryField.class);
    when(validField.isValid()).thenReturn(true);

    Summary summary = new Summary(APPLICATION_CODE);
    summary.addSummaryField(validField);

    assertThat(summary.summaryFields.size()).isEqualTo(1);
    assertThat(summary.isValid()).isTrue();
  }

  @Test
  public void summaryIsInvalidWithInvalidField() throws Exception {
    SummaryField invalidField = mock(SummaryField.class);
    when(invalidField.isValid()).thenReturn(false);

    Summary summary = new Summary(APPLICATION_CODE);
    summary.addSummaryField(invalidField);

    assertThat(summary.summaryFields.size()).isEqualTo(1);
    assertThat(summary.isValid()).isFalse();
  }
}
