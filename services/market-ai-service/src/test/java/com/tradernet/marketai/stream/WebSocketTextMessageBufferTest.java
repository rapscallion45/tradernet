package com.tradernet.marketai.stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketTextMessageBufferTest {

    @Test
    void emitsOnlyCompleteMessages() {
        final WebSocketTextMessageBuffer buffer = new WebSocketTextMessageBuffer(100);

        assertNull(buffer.append("{\"b\":[", false));
        assertEquals("{\"b\":[[\"100\",\"1\"]]}", buffer.append("[\"100\",\"1\"]]}", true));
    }

    @Test
    void resetsAfterOversizedMessages() {
        final WebSocketTextMessageBuffer buffer = new WebSocketTextMessageBuffer(4);

        assertThrows(IllegalStateException.class, () -> buffer.append("12345", false));
        assertEquals("ok", buffer.append("ok", true));
    }
}
