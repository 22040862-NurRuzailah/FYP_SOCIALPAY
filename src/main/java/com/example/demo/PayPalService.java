package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Scanner;

@Service
public class PayPalService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MemberRepository memberRepository;

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    private static final String PAYPAL_AUTH_URL = "https://api.sandbox.paypal.com/v1/oauth2/token";
    private static final String PAYPAL_PAYMENT_URL = "https://api.sandbox.paypal.com/v1/payments/payment";
    private static final String PAYPAL_BALANCE_URL = "https://api.sandbox.paypal.com/v1/reporting/balances";

    private String getAccessToken() throws IOException {
        URL url = new URL(PAYPAL_AUTH_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Accept-Language", "en_US");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            os.write("grant_type=client_credentials".getBytes());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String jsonResponse = readResponse(connection);
            JSONObject jsonObject = new JSONObject(jsonResponse);
            return jsonObject.getString("access_token");
        } else {
            throw new IOException("Failed to get access token. Response code: " + responseCode);
        }
    }

    public JSONObject createPayment(double amount, String currency, String description, long userID,
            String recipientEmail) throws IOException {
        String accessToken = getAccessToken();

        URL url = new URL(PAYPAL_PAYMENT_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setDoOutput(true);

        JSONObject payment = new JSONObject();
        payment.put("intent", "sale");

        JSONObject redirectUrls = new JSONObject();
        redirectUrls.put("return_url", "http://localhost:8080/transfer/success"); // Success page URL
        redirectUrls.put("cancel_url", "http://localhost:8080/transfer/cancel"); // Cancel page URL
        payment.put("redirect_urls", redirectUrls);

        JSONObject transaction = new JSONObject();
        transaction.put("description", description);

        JSONObject amountJson = new JSONObject();
        amountJson.put("total", String.format("%.2f", amount));
        amountJson.put("currency", currency);
        transaction.put("amount", amountJson);

        JSONObject payee = new JSONObject();
        payee.put("email", recipientEmail);
        transaction.put("payee", payee);

        JSONObject[] transactions = { transaction };
        payment.put("transactions", transactions);

        JSONObject payer = new JSONObject();
        payer.put("payment_method", "paypal");
        payment.put("payer", payer);

        String requestPayload = payment.toString();
        System.out.println("PayPal API Request Payload: " + requestPayload);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestPayload.getBytes());
        }

        int responseCode = connection.getResponseCode();
        String jsonResponse = readResponse(connection);
        System.out.println("PayPal API Response: " + jsonResponse);

        if (responseCode == HttpURLConnection.HTTP_CREATED) {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            String paymentId = jsonObject.getString("id");

            Transaction transactionEntity = new Transaction();
            transactionEntity.setPaypalID(paymentId);
            transactionEntity.setUserID(userID);
            transactionEntity.setTransactionDate(LocalDate.now());
            transactionEntity.setTransactionAmount(amount);
            transactionEntity.setTransactionType("PAYPAL");
            transactionEntity.setFlagged(false);
            transactionEntity.setFlaggedAutomatically(false);
            transactionEntity.setRecieverID(memberRepository.findByPaypalAccount(recipientEmail).getId());
            transactionEntity.setRcvName(memberRepository.findByPaypalAccount(recipientEmail).getName());

            transactionService.detectAndFlagSuspiciousTransaction(transactionEntity);
            transactionRepository.save(transactionEntity);

            System.out.println("Transaction saved successfully: " + paymentId);

            return jsonObject;
        } else {
            throw new IOException(
                    "Failed to create payment. Response code: " + responseCode + ", Response: " + jsonResponse);
        }
    }

    public void executePayment(String paymentId, String payerId) throws IOException {
        String accessToken = getAccessToken();

        URL url = new URL(PAYPAL_PAYMENT_URL + "/" + paymentId + "/execute");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        connection.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        requestBody.put("payer_id", payerId);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.toString().getBytes());
        }

        int responseCode = connection.getResponseCode();
        String jsonResponse = readResponse(connection);
        System.out.println("PayPal API Response: " + jsonResponse);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException(
                    "Failed to execute payment. Response code: " + responseCode + ", Response: " + jsonResponse);
        }
    }

    public JSONObject getPaymentDetails(String paymentId) throws IOException {
        String accessToken = getAccessToken();

        URL url = new URL(PAYPAL_PAYMENT_URL + "/" + paymentId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();
        String jsonResponse = readResponse(connection);
        System.out.println("PayPal API Response: " + jsonResponse);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            JSONObject transaction = jsonObject.getJSONArray("transactions").getJSONObject(0);
            JSONObject amount = transaction.getJSONObject("amount");
            String totalAmount = amount.getString("total");
            String currency = amount.getString("currency");

            JSONObject payee = transaction.getJSONObject("payee");
            String payeeEmail = payee.getString("email");

            JSONObject payerInfo = jsonObject.getJSONObject("payer").getJSONObject("payer_info");
            String payerName = payerInfo.getString("first_name") + " " + payerInfo.getString("last_name");

            JSONObject result = new JSONObject();
            result.put("paymentId", paymentId);
            result.put("amount", totalAmount);
            result.put("currency", currency);
            result.put("payeeEmail", payeeEmail);
            result.put("payerName", payerName);

            return result;
        } else {
            throw new IOException(
                    "Failed to fetch payment details. Response code: " + responseCode + ", Response: " + jsonResponse);
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (Scanner scanner = new Scanner(connection.getInputStream()).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    public JSONObject getBalance() throws IOException {
        String accessToken = getAccessToken();

        URL url = new URL(PAYPAL_BALANCE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();
        String jsonResponse = readResponse(connection);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            JSONArray balances = new JSONObject(jsonResponse).getJSONArray("balances");
            JSONObject result = new JSONObject();

            for (int i = 0; i < balances.length(); i++) {
                JSONObject balance = balances.getJSONObject(i);
                String currency = balance.getString("currency");
                String amount = balance.getJSONObject("total_balance").getString("value");
                result.put(currency, amount);
            }

            return result;
        } else {
            throw new IOException(
                    "Failed to fetch balance. Response code: " + responseCode + ", Response: " + jsonResponse);
        }
    }

}