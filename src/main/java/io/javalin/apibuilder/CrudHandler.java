package io.javalin.apibuilder;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.handler.ParameterHandler;
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction2;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.handler;
import static kotlin.TuplesKt.to;
import static kotlin.collections.MapsKt.mapOf;

/**
 * The CrudHandler is an interface for handling the five most
 * common CRUD operations. It's only available through the ApiBuilder.
 *
 * @see ApiBuilder
 */
public interface CrudHandler {
    void getAll(Context ctx);

    void getOne(Context ctx, String resourceId);

    void create(Context ctx);

    void update(Context ctx, String resourceId);

    void delete(Context ctx, String resourceId);

    default Map<CrudHandlerType, Handler> asMap(String resourceId) {
        return mapOf(
            to(CrudHandlerType.GET_ALL, handler(this::getAll)),
            to(CrudHandlerType.GET_ONE, new ParameterHandler<SerializableNoRFunction2<Context, String>>(this::getOne,
                (ctx, handler) -> handler.invoke(ctx, ctx.pathParam(resourceId)))),
            to(CrudHandlerType.CREATE, handler(this::create)),
            to(CrudHandlerType.UPDATE, new ParameterHandler<SerializableNoRFunction2<Context, String>>(this::update,
                (ctx, handler) -> handler.invoke(ctx, ctx.pathParam(resourceId)))),
            to(CrudHandlerType.DELETE, new ParameterHandler<SerializableNoRFunction2<Context, String>>(this::delete,
                (ctx, handler) -> handler.invoke(ctx, ctx.pathParam(resourceId))))
        );
    }
}
