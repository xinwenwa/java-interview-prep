package com.stripe.interview;

import org.testng.Reporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.helper.StringUtil;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RealtimeCaller {
  public static final String URL =
      "http://jaws-vip.qa.paypal.com/v1/QIJawsServices/restservices/creditcardmill";
  // String response = example.post("http://www.roundsapp.com/post", json);

  public static final String FILE_PATH = "Sample/replay.json";

  public static final String HTTP_CODE = "httpCode";

  public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  public static boolean testMode = true;

  public static Map<String, String> USER_IDS = new HashMap<>();

  /*
   * request class with getter/setter method
   */
  public class PaymentRequest {
    private String amount;
    private String userName;
    private String userId;
    private String currency;

    public String getAmount() {
      return amount;
    }

    public void setAmount(String amount) {
      this.amount = amount;
    }

    public String getCurrency() {
      return currency;
    }

    public void setCurrency(String currency) {
      this.currency = currency;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

  }

  public class PaymentResponse {
    private String httpCode;
    private String userId;

    public String getHttpCode() {
      return httpCode;
    }

    public void setHttpCode(String httpCode) {
      this.httpCode = httpCode;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }


  }

  public static void replay() throws Exception {
    FileReader reader = null;
    try {
      reader = new FileReader(FILE_PATH);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    JSONParser jparser = new JSONParser();
    Object obj = null;
    try {
      obj = jparser.parse(reader);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }

    if (obj != null) {
      JSONArray jsonArray = (JSONArray) obj;
      if (jsonArray != null) {
        Iterator it = jsonArray.iterator();
        while (it.hasNext()) {
          JSONObject jsonObj = (JSONObject) it.next();
          // import org.json.JSONObject
          // JSONObject jsonObject = new JSONObject(jsonSimpleObject.toString());
          if (jsonObj != null && jsonObj.get("paymentRequest") != null
              && jsonObj.get("paymentResponse") != null) {
            System.out.println();

            replayRequest(jsonObj.get("paymentRequest").toString(),
                jsonObj.get("paymentResponse").toString());

            replayRequest(jsonObj.get("paymentRequest").toString(),
                jsonObj.get("paymentResponse").toString());
          }
        }
      }
    }
  }

  public static void replayRequest(String jsonRequest, String jsonResponse) throws IOException {
    Gson gson = new Gson();
    PaymentRequest paymentRequest = gson.fromJson(jsonRequest, PaymentRequest.class);
    if (USER_IDS.containsKey(paymentRequest.getUserName())) {
      paymentRequest.setUserId(USER_IDS.get(paymentRequest.getUserName()));
    }
    Reporter.log("updated json request: " + gson.toJson(paymentRequest), true);

    OkHttpClient client = new OkHttpClient();

    RequestBody body = RequestBody.create(gson.toJson(paymentRequest), JSON);
    Request request = new Request.Builder().url(URL).post(body).build();
    Reporter.log("request via okHttp: " + request, true);

    if (testMode) {
      compareResponse("{'httpCode':'400'}", jsonResponse);
    } else {
      String replayResponse = null;
      try (Response response = client.newCall(request).execute()) {
        replayResponse = response.body().string();
      }
      compareResponse(replayResponse, jsonResponse);
    }
    // should use replayResponse
    PaymentResponse orig = gson.fromJson(jsonResponse, PaymentResponse.class);
    if (!StringUtil.isBlank(orig.getUserId())) {
      USER_IDS.put(paymentRequest.getUserName(), orig.getUserId());
    }
  }

  public static void compareResponse(String replayResponse, String originalResponse) {
    // testing
    Gson gson = new Gson();
    PaymentResponse replay = gson.fromJson(replayResponse, PaymentResponse.class);
    PaymentResponse orig = gson.fromJson(originalResponse, PaymentResponse.class);
    Reporter.log("replay status code: " + replay.getHttpCode() + ", original status code: "
        + orig.getHttpCode(), true);
  }

  public static void main(String[] args) throws Exception {
    RealtimeCaller.replay();
  }

  public static String decodePayload(String encoded) throws IOException {
    byte[] compressedDecodedData = Base64.getDecoder().decode(encoded);

    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPInputStream gis =
            new GZIPInputStream(new ByteArrayInputStream(compressedDecodedData))) {

      int byteRead;
      while ((byteRead = gis.read()) != -1) {
        out.write((byte) byteRead);
      }

      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  public static String encodePayload(String replayDataJson) throws Exception {
    ByteArrayOutputStream obj = new ByteArrayOutputStream();
    // compress
    GZIPOutputStream gzip = new GZIPOutputStream(obj);
    gzip.write(replayDataJson.getBytes(StandardCharsets.UTF_8));

    gzip.flush();
    gzip.close();

    return Base64.getEncoder().encodeToString(obj.toByteArray());
  }

}
