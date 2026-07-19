package com.tradernet.marketai.model;

/**
 * Structured explanation item for model drivers and signal diagnostics.
 */
public class ExplanationItem {

    private String key;
    private String label;
    private String value;
    private Double numericValue;

    public ExplanationItem() {
    }

    public ExplanationItem(String key, String label, String value, Double numericValue) {
        this.key = key;
        this.label = label;
        this.value = value;
        this.numericValue = numericValue;
    }

    public static ExplanationItem text(String key, String label) {
        return new ExplanationItem(key, label, null, null);
    }

    public static ExplanationItem value(String key, String label, String value) {
        return new ExplanationItem(key, label, value, null);
    }

    public static ExplanationItem numeric(String key, String label, double numericValue) {
        return new ExplanationItem(key, label, null, numericValue);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Double getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(Double numericValue) {
        this.numericValue = numericValue;
    }
}
