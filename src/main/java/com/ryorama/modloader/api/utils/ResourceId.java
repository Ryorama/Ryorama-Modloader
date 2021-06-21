package com.ryorama.modloader.api.utils;

public class ResourceId {

    private final String namespace, path;

    public ResourceId(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String toString() {
        return this.namespace + ':' + this.path;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (! (object instanceof ResourceId) ) {
            return false;
        } else {
            ResourceId identifier = (ResourceId) object;
            return this.namespace.equals(identifier.namespace) && this.path.equals(identifier.path);
        }
    }

    public int hashCode() {
        return 31 * this.namespace.hashCode() + this.path.hashCode();
    }

    public int compareTo(ResourceId identifier) {
        int i = this.path.compareTo(identifier.path);
        if (i == 0) {
            i = this.namespace.compareTo(identifier.namespace);
        }

        return i;
    }
}
