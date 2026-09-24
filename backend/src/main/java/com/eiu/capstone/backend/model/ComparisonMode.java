package com.eiu.capstone.backend.model;

public enum ComparisonMode {
    EXACT,
    TRIMMED,
    NORMALIZED_WHITESPACE,
    /** Compare numeric values after widening to double (e.g. int 10 equals double 10.0). */
    VALUE_ONLY
}
