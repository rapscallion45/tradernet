package com.tradernet.marketai.orderbook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Parses Binance REST snapshots and websocket depth updates into typed provider messages.
 */
final class BinanceOrderBookPayloadParser {

    private final ObjectMapper objectMapper;

    BinanceOrderBookPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    SnapshotData parseSnapshot(String payload, long receivedAtMs) {
        try {
            final JsonNode node = objectMapper.readTree(payload);
            final long lastUpdateId = node.path("lastUpdateId").asLong(-1L);
            if (lastUpdateId < 0L) {
                return null;
            }
            return new SnapshotData(
                lastUpdateId,
                parseLevels(node.path("bids")),
                parseLevels(node.path("asks")),
                receivedAtMs
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid Binance order book snapshot payload", ex);
        }
    }

    DepthUpdate parseDepthUpdate(String payload) {
        try {
            final JsonNode node = objectMapper.readTree(payload);
            final long firstUpdateId = node.path("U").asLong(-1L);
            final long finalUpdateId = node.path("u").asLong(-1L);
            if (firstUpdateId < 0L || finalUpdateId < 0L) {
                return null;
            }
            return new DepthUpdate(
                firstUpdateId,
                finalUpdateId,
                node.path("pu").asLong(-1L),
                node.path("E").asLong(System.currentTimeMillis()),
                parseLevelUpdates(node.path("b")),
                parseLevelUpdates(node.path("a"))
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid Binance depth update payload", ex);
        }
    }

    private NavigableMap<BigDecimal, BigDecimal> parseLevels(JsonNode levelsNode) {
        final NavigableMap<BigDecimal, BigDecimal> levels = new TreeMap<>();
        for (LevelUpdate level : parseLevelUpdates(levelsNode)) {
            if (level.quantity.signum() > 0) {
                levels.put(level.price, level.quantity);
            }
        }
        return levels;
    }

    private List<LevelUpdate> parseLevelUpdates(JsonNode levelsNode) {
        final List<LevelUpdate> updates = new ArrayList<>();
        if (levelsNode == null || !levelsNode.isArray()) {
            return updates;
        }
        for (JsonNode levelNode : levelsNode) {
            final LevelUpdate level = parseLevel(levelNode);
            if (level != null) {
                updates.add(level);
            }
        }
        return updates;
    }

    private LevelUpdate parseLevel(JsonNode levelNode) {
        if (levelNode == null || !levelNode.isArray() || levelNode.size() < 2) {
            return null;
        }
        final BigDecimal price = decimalValue(levelNode.get(0));
        final BigDecimal quantity = decimalValue(levelNode.get(1));
        if (price == null || quantity == null || price.signum() <= 0 || quantity.signum() < 0) {
            return null;
        }
        return new LevelUpdate(price, quantity);
    }

    private BigDecimal decimalValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static final class DepthUpdate {
        final long firstUpdateId;
        final long finalUpdateId;
        final long previousFinalUpdateId;
        final long eventTime;
        final List<LevelUpdate> bids;
        final List<LevelUpdate> asks;

        DepthUpdate(
            long firstUpdateId,
            long finalUpdateId,
            long previousFinalUpdateId,
            long eventTime,
            List<LevelUpdate> bids,
            List<LevelUpdate> asks
        ) {
            this.firstUpdateId = firstUpdateId;
            this.finalUpdateId = finalUpdateId;
            this.previousFinalUpdateId = previousFinalUpdateId;
            this.eventTime = eventTime;
            this.bids = List.copyOf(bids);
            this.asks = List.copyOf(asks);
        }
    }

    static final class LevelUpdate {
        final BigDecimal price;
        final BigDecimal quantity;

        LevelUpdate(BigDecimal price, BigDecimal quantity) {
            this.price = price;
            this.quantity = quantity;
        }
    }

    static final class SnapshotData {
        final long lastUpdateId;
        final NavigableMap<BigDecimal, BigDecimal> bids;
        final NavigableMap<BigDecimal, BigDecimal> asks;
        final long receivedAtMs;

        SnapshotData(
            long lastUpdateId,
            NavigableMap<BigDecimal, BigDecimal> bids,
            NavigableMap<BigDecimal, BigDecimal> asks,
            long receivedAtMs
        ) {
            this.lastUpdateId = lastUpdateId;
            this.bids = new TreeMap<>(Comparator.reverseOrder());
            this.bids.putAll(bids);
            this.asks = new TreeMap<>();
            this.asks.putAll(asks);
            this.receivedAtMs = receivedAtMs;
        }
    }
}
