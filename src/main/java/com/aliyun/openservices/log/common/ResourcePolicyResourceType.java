package com.aliyun.openservices.log.common;

/**
 * Resource type supported by the Resource Policy API.
 */
public enum ResourcePolicyResourceType {
    PROJECT("project"),
    LOGSTORE("logstore");

    private final String value;

    ResourcePolicyResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ResourcePolicyResourceType fromValue(String value) {
        for (ResourcePolicyResourceType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ResourcePolicyResourceType value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
