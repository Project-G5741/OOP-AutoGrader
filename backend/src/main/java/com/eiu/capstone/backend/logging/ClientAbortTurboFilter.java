package com.eiu.capstone.backend.logging;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Suppresses noisy ERROR logs when the client closes the connection before the response finishes.
 */
public class ClientAbortTurboFilter extends TurboFilter {

    @Override
    public FilterReply decide(
            Marker marker,
            Logger logger,
            Level level,
            String format,
            Object[] params,
            Throwable t) {
        if (isClientAbort(t)) {
            return FilterReply.DENY;
        }
        if (params != null) {
            for (Object param : params) {
                if (param instanceof Throwable throwable && isClientAbort(throwable)) {
                    return FilterReply.DENY;
                }
            }
        }
        return FilterReply.NEUTRAL;
    }

    private static boolean isClientAbort(Throwable t) {
        for (Throwable current = t; current != null; current = current.getCause()) {
            String typeName = current.getClass().getName();
            if (typeName.endsWith("ClientAbortException")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("broken pipe") || lower.contains("connection reset")) {
                    return true;
                }
            }
        }
        return false;
    }
}
