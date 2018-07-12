package components.client;

import uk.gov.bis.lite.permissions.api.view.OgelRegistrationView;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface PermissionsServiceClient {

  CompletionStage<String> registerOgel(String userId, String customerId, String siteId, String ogelId,
                                       String callbackUrl);

  CompletionStage<List<OgelRegistrationView>> getOgelRegistrations(String userId);

  CompletionStage<OgelRegistrationView> getOgelRegistration(String userId, String registrationReference);

  CompletionStage<Boolean> ping();

}
