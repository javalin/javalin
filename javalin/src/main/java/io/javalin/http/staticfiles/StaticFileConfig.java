package io.javalin.http.staticfiles;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StaticFileConfig {

    private final String urlPathPrefix;
    private final String path;
    private final Location location;

    public StaticFileConfig(@NotNull String urlPathPrefix, @NotNull String path, @NotNull Location location) {
        this.urlPathPrefix = urlPathPrefix;
        this.path = path;
        this.location = location;
    }

    public String getUrlPathPrefix() {
        return urlPathPrefix;
    }

    public String getPath() {
        return path;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticFileConfig that = (StaticFileConfig) o;
        return urlPathPrefix.equals(that.urlPathPrefix) &&
            path.equals(that.path) &&
            location == that.location;
    }

    @Override
    public int hashCode() {
        return Objects.hash(urlPathPrefix, path, location);
    }
}
