package components.services.controlcode.category;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import models.GoodsType;
import models.softtech.SoftTechCategory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import pact.PactConfig;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import uk.gov.bis.lite.controlcode.api.view.ControlCodeFullView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class CategoryControlsServiceConsumerPact {

  private CategoryControlsServiceClient client;
  private WSClient ws;

  private final static String CONTROL_CODE = "ML1a";
  private final static String TITLE = "Smooth-bore military weapons, components and accessories";

  @Rule
  public PactProviderRule mockProvider = new PactProviderRule(PactConfig.CONTROL_CODE_SERVICE_PROVIDER, this);

  @Before
  public void setUp() throws Exception {
    ws = WS.newClient(mockProvider.getConfig().getPort());
    client = new CategoryControlsServiceClient(new HttpExecutionContext(Runnable::run), ws, mockProvider.getConfig().url(), 10000);
  }

  @After
  public void tearDown() throws Exception {
    ws.close();
  }

  @Pact(provider = PactConfig.CONTROL_CODE_SERVICE_PROVIDER, consumer = PactConfig.CONSUMER)
  public PactFragment softwareMilitary(PactDslWithProvider builder) {
    PactDslJsonArray existing = PactDslJsonArray.arrayMinLike(1,3)
        .stringType("controlCode", CONTROL_CODE)
        .stringType("title", TITLE)
        .closeObject()
        .asArray();

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    return builder
        .given("software exists as a goods type and there are military controls")
        .uponReceiving("a request for software military controls")
          .path("/specific-controls/software/military")
          .method("GET")
          .willRespondWith()
            .status(200)
            .headers(headers)
            .body(existing)
        .toFragment();
  }

  @Pact(provider = PactConfig.CONTROL_CODE_SERVICE_PROVIDER, consumer = PactConfig.CONSUMER)
  public PactFragment softwareMilitaryEmpty(PactDslWithProvider builder) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
        .given("software exists as a goods type and no military controls exist")
        .uponReceiving("a request for software military controls")
          .path("/specific-controls/software/military")
          .method("GET")
          .willRespondWith()
            .status(200)
            .headers(headers)
            .body(new PactDslJsonArray())
        .toFragment();
  }

  @Pact(provider = PactConfig.CONTROL_CODE_SERVICE_PROVIDER, consumer = PactConfig.CONSUMER)
  public PactFragment softwareDualUse(PactDslWithProvider builder) {
    PactDslJsonArray existing = PactDslJsonArray.arrayMinLike(1,3)
        .stringType("controlCode", CONTROL_CODE)
        .stringType("title", TITLE)
        .closeObject()
        .asArray();

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    return builder
        .given("software exists as a goods type and telecoms exists as dual use category with controls")
        .uponReceiving("a request for software dual use telecoms controls")
          .path("/specific-controls/software/dual-use/telecoms")
          .method("GET")
          .willRespondWith()
            .status(200)
            .headers(headers)
            .body(existing)
        .toFragment();
  }

  @Pact(provider = PactConfig.CONTROL_CODE_SERVICE_PROVIDER, consumer = PactConfig.CONSUMER)
  public PactFragment softwareDualUseEmpty(PactDslWithProvider builder) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
        .given("software exists as a goods type and telecoms exists as dual use category without controls")
        .uponReceiving("a request for software dual use telecoms controls")
          .path("/specific-controls/software/dual-use/telecoms")
          .method("GET")
          .willRespondWith()
            .status(200)
            .headers(headers)
            .body(new PactDslJsonArray())
        .toFragment();
  }

  @Test
  @PactVerification(value = PactConfig.CONTROL_CODE_SERVICE_PROVIDER, fragment = "softwareMilitary")
  public void softwareMilitary() throws Exception {
    CategoryControlsServiceResult result;
    try {
      result = client.get(GoodsType.SOFTWARE, SoftTechCategory.MILITARY).toCompletableFuture().get();
    }
    catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    assertThat(result).isNotNull();

    List<ControlCodeFullView> controlCodes = result.getControlCodes();

    assertThat(controlCodes.size()).isEqualTo(3);

    ControlCodeFullView controlCode = result.getControlCodes().get(0);

    assertThat(controlCode.getControlCode()).isEqualTo(CONTROL_CODE);
    assertThat(controlCode.getTitle()).isEqualTo(TITLE);
  }

  @Test
  @PactVerification(value = PactConfig.CONTROL_CODE_SERVICE_PROVIDER, fragment = "softwareMilitaryEmpty")
  public void softwareMilitaryEmpty() throws Exception {
    CategoryControlsServiceResult result;
    try {
      result = client.get(GoodsType.SOFTWARE, SoftTechCategory.MILITARY).toCompletableFuture().get();
    }
    catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    assertThat(result).isNotNull();
    assertThat(result.getControlCodes().isEmpty()).isTrue();
  }

  @Test
  @PactVerification(value = PactConfig.CONTROL_CODE_SERVICE_PROVIDER, fragment = "softwareDualUse")
  public void softwareDualUse() throws Exception {
    CategoryControlsServiceResult result;
    try {
      result = client.get(GoodsType.SOFTWARE, SoftTechCategory.TELECOMS).toCompletableFuture().get();
    }
    catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    assertThat(result).isNotNull();

    List<ControlCodeFullView> controlCodes = result.getControlCodes();

    assertThat(controlCodes.size()).isEqualTo(3);

    ControlCodeFullView controlCode = result.getControlCodes().get(0);

    assertThat(controlCode.getControlCode()).isEqualTo(CONTROL_CODE);
    assertThat(controlCode.getTitle()).isEqualTo(TITLE);
  }

  @Test
  @PactVerification(value = PactConfig.CONTROL_CODE_SERVICE_PROVIDER, fragment = "softwareDualUseEmpty")
  public void softwareDualUseEmpty() throws Exception {
    CategoryControlsServiceResult result;
    try {
      result = client.get(GoodsType.SOFTWARE, SoftTechCategory.TELECOMS).toCompletableFuture().get();
    }
    catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    assertThat(result).isNotNull();
    assertThat(result.getControlCodes().isEmpty()).isTrue();
  }
}
