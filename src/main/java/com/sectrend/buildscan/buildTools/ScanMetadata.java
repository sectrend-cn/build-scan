package com.sectrend.buildscan.buildTools;

import java.util.Objects;

public class ScanMetadata<T> {
    private final String key;

    private final Class<T> metadataClass;

    public ScanMetadata(String key, Class<T> metadataClass) {
        this.key = key;
        this.metadataClass = metadataClass;
    }

    public String getKey() {
        return this.key;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScanMetadata<?> that = (ScanMetadata)o;
        return this.key.equals(that.key);
    }

    public int hashCode() {
        return Objects.hash(new Object[] { this.key });
    }

    public Class<T> getMetadataClass() {
        return this.metadataClass;
    }
}
