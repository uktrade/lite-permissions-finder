package models.persistence;

public class RegisterLicence {

  private String transactionId;
  private String requestId;
  private String registrationReference;
  private String userId;
  private String customerId;
  private String siteId;
  private String ogelId;
  private String callbackUrl;

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getSiteId() {
    return siteId;
  }

  public void setSiteId(String siteId) {
    this.siteId = siteId;
  }

  public String getOgelId() {
    return ogelId;
  }

  public void setOgelId(String ogelId) {
    this.ogelId = ogelId;
  }

  public String getCallbackUrl() {
    return callbackUrl;
  }

  public void setCallbackUrl(String callbackUrl) {
    this.callbackUrl = callbackUrl;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getRegistrationReference() {
    return registrationReference;
  }

  public void setRegistrationReference(String registrationReference) {
    this.registrationReference = registrationReference;
  }
}
