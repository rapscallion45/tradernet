package com.tradernet.marketai.forecast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates concise market commentary through Ollama-hosted Gemma 4.
 */
public class OllamaNarrativeClient {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaNarrativeClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI generateUri;
    private final String model;

    public OllamaNarrativeClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.generateUri = URI.create(System.getProperty("market.ai.ollama.url", "http://ollama:11434")).resolve("/api/generate");
        this.model = System.getProperty("market.ai.ollama.model", "gemma4:e4b");
    }

    public String summarize(MarketForecast forecast) {
        if (!Boolean.parseBoolean(System.getProperty("market.ai.ollama.enabled", "true"))) {
            return fallbackNarrative(forecast);
        }

        final Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", false);
        requestBody.put("prompt", prompt(forecast));
        requestBody.put("options", Map.of("temperature", 0.2, "num_predict", 80));

        try {
            final String json = objectMapper.writeValueAsString(requestBody);
            final HttpRequest request = HttpRequest.newBuilder(generateUri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                LOG.warn("Ollama returned status {}", response.statusCode());
                return fallbackNarrative(forecast);
            }
            final JsonNode root = objectMapper.readTree(response.body());
            final String narrative = root.path("response").asText("").trim();
            return narrative.isBlank() ? fallbackNarrative(forecast) : narrative;
        } catch (IOException ex) {
            LOG.warn("Unable to call Ollama Gemma 4 narrative service", ex);
            return fallbackNarrative(forecast);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.warn("Ollama narrative call interrupted", ex);
            return fallbackNarrative(forecast);
        } catch (RuntimeException ex) {
            LOG.warn("Unable to build Ollama narrative", ex);
            return fallbackNarrative(forecast);
        }
    }

    private String prompt(MarketForecast forecast) {
        return "Write one concise, compliance-safe Bitcoin market forecast sentence. "
                + "Do not give financial advice. Use this exact shape: Today's Bitcoin Bull Score is <score>. "
                + "<two or three drivers>. Probability of a positive <horizon>-day return: <probability>%. "
                + "Data: score=" + Math.round(forecast.getBullScore())
                + ", horizon=" + forecast.getHorizonDays()
                + ", probability=" + Math.round(forecast.getProbabilityPositiveReturn() * 100.0)
                + ", expected_return=" + String.format("%.2f", forecast.getExpectedReturn() * 100.0) + "%"
                + ", drivers=" + String.join(", ", safeDrivers(forecast.getDrivers())) + ".";
    }

    private List<String> safeDrivers(List<String> drivers) {
        return drivers == null ? List.of() : drivers.stream().limit(5).collect(java.util.stream.Collectors.toList());
    }

    private String fallbackNarrative(MarketForecast forecast) {
        final String drivers = forecast.getDrivers() == null || forecast.getDrivers().isEmpty()
                ? "model drivers are mixed"
                : String.join(", ", forecast.getDrivers().stream().limit(3).collect(java.util.stream.Collectors.toList()));
        return "Today's Bitcoin Bull Score is " + Math.round(forecast.getBullScore())
                + ". " + drivers + ". Probability of a positive " + forecast.getHorizonDays()
                + "-day return: " + Math.round(forecast.getProbabilityPositiveReturn() * 100.0) + "%.";
    }
}
