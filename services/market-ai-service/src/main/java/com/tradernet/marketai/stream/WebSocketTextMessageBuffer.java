package com.tradernet.marketai.stream;

/**
 * Reassembles Java HttpClient websocket text fragments into complete messages.
 */
public final class WebSocketTextMessageBuffer {

    private final int maxMessageChars;
    private final StringBuilder buffer = new StringBuilder();

    public WebSocketTextMessageBuffer(int maxMessageChars) {
        if (maxMessageChars <= 0) {
            throw new IllegalArgumentException("maxMessageChars must be positive");
        }
        this.maxMessageChars = maxMessageChars;
    }

    public String append(CharSequence data, boolean last) {
        if (data != null) {
            buffer.append(data);
        }
        if (buffer.length() > maxMessageChars) {
            reset();
            throw new IllegalStateException("Websocket text message exceeded " + maxMessageChars + " characters");
        }
        if (!last) {
            return null;
        }

        final String payload = buffer.toString();
        reset();
        return payload;
    }

    public void reset() {
        buffer.setLength(0);
    }
}
