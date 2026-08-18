package com.eiu.capstone.backend.grading.mmd;

public record MmdToken(MmdTokenType type, String text, int line, int column) {

    public static MmdToken of(MmdTokenType type, String text, int line, int column) {
        return new MmdToken(type, text, line, column);
    }
}
