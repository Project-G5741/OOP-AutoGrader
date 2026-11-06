package com.eiu.capstone.backend.desktop.pack;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DesktopPackSerializer {

    private final ObjectMapper mapper;

    public DesktopPackSerializer() {
        this.mapper = DesktopPackJackson.newMapper();
    }

    public byte[] toJson(DesktopPackInnerPayload payload) {
        try {
            return mapper.writeValueAsBytes(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize desktop pack payload", e);
        }
    }

    public DesktopPackInnerPayload fromJson(byte[] json) {
        try {
            byte[] payload = stripUtf8Bom(json);
            return mapper.readValue(payload, DesktopPackInnerPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize desktop pack payload: " + rootMessage(e), e);
        }
    }

    private static byte[] stripUtf8Bom(byte[] json) {
        if (json != null && json.length >= 3
                && (json[0] & 0xFF) == 0xEF
                && (json[1] & 0xFF) == 0xBB
                && (json[2] & 0xFF) == 0xBF) {
            byte[] trimmed = new byte[json.length - 3];
            System.arraycopy(json, 3, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return json;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        String message = null;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message != null ? message : "invalid pack JSON";
    }
}
