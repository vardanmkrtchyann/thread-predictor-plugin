package com.github.vardanmkrtchyann.threadpredictorplugin.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiService {
    // TODO: For local testing only. Never commit your actual API key to GitHub!
    private static final String API_KEY = "YOUR API KEY";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public static String analyzeCode(String codeSnippet) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 1. Build the JSON payload for Gemini
            JsonObject part = new JsonObject();
            String systemPrompt = "You are a deterministic JVM Execution Engine. " +
                    "Calculate the exact STDOUT prints or final variable states. " +
                    "BE AGGRESSIVELY CONCISE. ZERO fluff. DO NOT write paragraphs. " +
                    "Return ONLY HTML. Structure it EXACTLY like this:\n" +
                    "<b>Status:</b> [Thread-Safe or NOT Thread-Safe]<br><br>" +
                    "<b>Precise STDOUT/Results:</b> [e.g., '012012', 'XY']<br><br>" +
                    "<b>Execution Trace:</b> [MAXIMUM 15 WORDS. Use arrow notation. e.g., 'T1 reads x=0 -> T2 reads x=0 -> T1 prints 0 -> T2 prints 0']\n\n" +
                    "Code to execute:\n\n";

            part.addProperty("text", systemPrompt + codeSnippet);

            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            parts.add(part);
            content.add("parts", parts);

            JsonArray contents = new JsonArray();
            contents.add(content);

            JsonObject requestBody = new JsonObject();
            requestBody.add("contents", contents);

            // 2. Make the HTTP POST Request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // 1. Log the raw response to your main IDE console so we can debug it
            System.out.println("=== RAW GEMINI RESPONSE ===");
            System.out.println(responseBody);

            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            // 2. Safely check if Google returned an error
            if (jsonResponse.has("error")) {
                JsonObject errorObj = jsonResponse.getAsJsonObject("error");
                return "API Error: " + errorObj.get("message").getAsString();
            }

            // 3. Safely parse the success response
            if (jsonResponse.has("candidates")) {
                return jsonResponse.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
            }

            return "Unexpected response format. Check your main IDE console.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling Gemini API: " + e.getMessage();
        }
    }

    // If the current version is not working for you, you can use this method to print the available models
    public static void printAvailableModels() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            // Note: This is a GET request, so we don't build a JSON body
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=" + API_KEY))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("=== AVAILABLE MODELS ===");
            System.out.println(response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}