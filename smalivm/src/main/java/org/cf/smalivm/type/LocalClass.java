package org.cf.smalivm.type;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class LocalClass implements LocalType {

    private final String className;

    public LocalClass(String className) {
        this.className = className;
    }

    @Override
    public boolean equals(Object other) {
        if (null == other) {
            return false;
        } else if (this == other) {
            return true;
        } else if (!(other instanceof LocalClass)) {
            return false;
        }

        LocalClass rhs = (LocalClass) other;

        return new EqualsBuilder().append(className, rhs.className).isEquals();
    }

    public String getName() {
        return className;
    }

    @Override
    public String toString() {
        return className;
    }

}
