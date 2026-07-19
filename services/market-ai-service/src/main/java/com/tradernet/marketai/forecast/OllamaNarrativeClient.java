package com.tradernet.marketai.forecast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.model.ExplanationItem;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
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
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OllamaNarrativeClient {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaNarrativeClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI generateUri = URI.create(System.getProperty("market.ai.ollama.url", "http://ollama:11434")).resolve("/api/generate");
    private final String model = System.getProperty("market.ai.ollama.model", "gemma4:e4b");

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
            return narrative.isBlank() ? fallbackNarrative(forecast) : enforceSelectedSymbol(narrative, forecast);
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
        final String symbol = displaySymbol(forecast);
        return "Write one concise, compliance-safe market forecast sentence for " + symbol + ". "
                + "Do not give financial advice. Use this exact shape: Today's " + symbol + " Bull Score is <score>. "
                + "<two or three drivers>. Probability of a positive <horizon>-day return: <probability>%. "
                + "Data: score=" + Math.round(forecast.getBullScore())
                + ", horizon=" + forecast.getHorizonDays()
                + ", probability=" + Math.round(forecast.getProbabilityPositiveReturn() * 100.0)
                + ", expected_return=" + String.format("%.2f", forecast.getExpectedReturn() * 100.0) + "%"
                + ", drivers=" + String.join(", ", safeDriverLabels(forecast.getDrivers())) + ".";
    }

    private String displaySymbol(MarketForecast forecast) {
        if (forecast == null || forecast.getSymbol() == null || forecast.getSymbol().isBlank()) {
            return "Market";
        }
        return forecast.getSymbol().trim().toUpperCase();
    }

    private List<String> safeDriverLabels(List<ExplanationItem> drivers) {
        return drivers == null ? List.of() : drivers.stream()
                .map(this::driverLabel)
                .filter(label -> label != null && !label.isBlank())
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
    }

    private String enforceSelectedSymbol(String narrative, MarketForecast forecast) {
        final String symbol = displaySymbol(forecast);
        return narrative
                .replace("Today's Bitcoin Bull Score", "Today's " + symbol + " Bull Score")
                .replace("Today’s Bitcoin Bull Score", "Today’s " + symbol + " Bull Score")
                .replace("Bitcoin Bull Score", symbol + " Bull Score")
                .replace("Bitcoin", symbol)
                .replace("bitcoin", symbol);
    }

    private String fallbackNarrative(MarketForecast forecast) {
        final List<String> driverLabels = safeDriverLabels(forecast.getDrivers());
        final String drivers = driverLabels.isEmpty()
                ? "model drivers are mixed"
                : String.join(", ", driverLabels.stream().limit(3).collect(java.util.stream.Collectors.toList()));
        return "Today's " + displaySymbol(forecast) + " Bull Score is " + Math.round(forecast.getBullScore())
                + ". " + drivers + ". Probability of a positive " + forecast.getHorizonDays()
                + "-day return: " + Math.round(forecast.getProbabilityPositiveReturn() * 100.0) + "%.";
    }

    private String driverLabel(ExplanationItem driver) {
        if (driver == null) {
            return "";
        }
        if (driver.getLabel() != null && !driver.getLabel().isBlank()) {
            return driver.getLabel();
        }
        if (driver.getValue() != null && !driver.getValue().isBlank()) {
            return driver.getValue();
        }
        return driver.getKey();
    }
}
