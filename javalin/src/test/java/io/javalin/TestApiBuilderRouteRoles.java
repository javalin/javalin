/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin;

import io.javalin.http.HandlerType;
import io.javalin.router.ParsedEndpoint;
import io.javalin.security.Roles;
import io.javalin.security.RouteRole;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static org.assertj.core.api.Assertions.assertThat;

public class TestApiBuilderRouteRoles {

    enum Role implements RouteRole { A, B, C }

    @Test
    public void testPathInheritsRouteRoles() {
        Javalin app = Javalin.create(cfg -> cfg.routes.apiBuilder(() -> path("/admin", Set.of(Role.A), () -> {
            get("/all", ctx -> {});
            get("/merged", ctx -> {}, Role.B);
            path("/nested", Set.of(Role.B), () -> {
                get("/one", ctx -> {});
            });
        })));

        List<ParsedEndpoint> endpoints = app.unsafe.internalRouter.allHttpHandlers();
        assertThat(findEndpointRoles(endpoints, HandlerType.GET, "/admin/all")).containsExactlyInAnyOrder(Role.A);
        assertThat(findEndpointRoles(endpoints, HandlerType.GET, "/admin/merged")).containsExactlyInAnyOrder(Role.A, Role.B);
        assertThat(findEndpointRoles(endpoints, HandlerType.GET, "/admin/nested/one")).containsExactlyInAnyOrder(Role.A, Role.B);
    }

    @Test
    public void testPathRoleScopeIsClearedAfterException() {
        Javalin app = Javalin.create(cfg -> cfg.routes.apiBuilder(() -> {
            try {
                path("/admin", Set.of(Role.A), () -> {
                    throw new RuntimeException("boom");
                });
            } catch (RuntimeException ignored) {
            }
            get("/public", ctx -> {});
        }));

        List<ParsedEndpoint> endpoints = app.unsafe.internalRouter.allHttpHandlers();
        assertThat(findEndpointRoles(endpoints, HandlerType.GET, "/public")).isEmpty();
    }

    @Test
    public void testEmptyNestedScopeDoesNotAddRoles() {
        Javalin app = Javalin.create(cfg -> cfg.routes.apiBuilder(() -> path("/admin", Set.of(Role.A), () -> {
            path("/sub", () -> {
                get("/endpoint", ctx -> {});
            });
        })));

        List<ParsedEndpoint> endpoints = app.unsafe.internalRouter.allHttpHandlers();
        assertThat(findEndpointRoles(endpoints, HandlerType.GET, "/admin/sub/endpoint")).containsExactlyInAnyOrder(Role.A);
    }

    @Test
    public void testDeepNestingAccumulatesRoles() {
        Javalin app = Javalin.create(cfg -> cfg.routes.apiBuilder(() -> path("/level1", Set.of(Role.A), () -> {
            path("/level2", Set.of(Role.B), () -> {
                path("/level3", Set.of(Role.C), () -> {
                    get("/deep", ctx -> {});
                });
            });
        })));

        List<ParsedEndpoint> endpoints = app.unsafe.internalRouter.allHttpHandlers();
        assertThat(findEndpointRoles(endpoints, HandlerType.GET, "/level1/level2/level3/deep"))
            .containsExactlyInAnyOrder(Role.A, Role.B, Role.C);
    }

    @Test
    public void testPathWithoutRolesIsUnaffected() {
        Javalin app = Javalin.create(cfg -> cfg.routes.apiBuilder(() -> {
            path("/public", () -> {
                get("/endpoint", ctx -> {});
            });
        }));

        List<ParsedEndpoint> endpoints = app.unsafe.internalRouter.allHttpHandlers();
        assertThat(findEndpointRoles(endpoints, HandlerType.GET, "/public/endpoint")).isEmpty();
    }

    private static Set<RouteRole> findEndpointRoles(List<ParsedEndpoint> endpoints, HandlerType handlerType, String path) {
        return endpoints.stream()
            .filter(endpoint -> endpoint.endpoint.method == handlerType && endpoint.endpoint.path.equals(path))
            .findFirst()
            .map(TestApiBuilderRouteRoles::roles)
            .orElse(Collections.emptySet());
    }

    private static Set<RouteRole> roles(ParsedEndpoint parsedEndpoint) {
        Roles metadata = parsedEndpoint.endpoint.metadata(Roles.class);
        return metadata == null ? Collections.emptySet() : metadata.getRoles();
    }
}
