package cn.heycloudream.quiz_backend.config.openapi;

import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.common.vo.Result;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 将 Controller 方法上的 {@link Result}{@code <T>} 返回类型展开为带具体 {@code data} 结构的 OpenAPI Schema，
 * 避免 Swagger UI 中 {@code data} 显示为无字段的 object。
 * <p>仅影响文档生成，不参与业务序列化。</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ResultResponseOperationCustomizer implements OperationCustomizer {

    private static final String MEDIA_JSON = "application/json";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Type returnType = handlerMethod.getMethod().getGenericReturnType();
        if (!(returnType instanceof ParameterizedType pt) || pt.getRawType() != Result.class) {
            return operation;
        }
        Type dataType = pt.getActualTypeArguments()[0];
        Schema<?> resultSchema = buildResultSchema(dataType);

        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        ApiResponse ok = responses.get("200");
        if (ok == null) {
            ok = new ApiResponse();
            responses.addApiResponse("200", ok);
        }
        if (ok.getDescription() == null || ok.getDescription().isBlank()) {
            ok.setDescription(ApiDocStandardResponses.HTTP_200_DESCRIPTION);
        }
        Content content = new Content();
        content.addMediaType(MEDIA_JSON, new MediaType().schema(resultSchema));
        ok.setContent(content);
        return operation;
    }

    private Schema<?> buildResultSchema(Type dataType) {
        ObjectSchema schema = new ObjectSchema();
        schema.name(schemaNameForResult(dataType));
        schema.description("统一 API 响应包装（code / message / data）");
        schema.addProperty("code", new IntegerSchema()
                .description("业务状态码：200 成功；4xx/5xx 为业务失败（HTTP 仍多为 200）")
                .example(200));
        schema.addProperty("message", new StringSchema()
                .description("提示信息")
                .example("success"));
        Schema<?> dataSchema = resolveDataSchema(dataType);
        if (dataSchema != null) {
            dataSchema.setNullable(true);
            dataSchema.setDescription(
                    dataSchema.getDescription() != null ? dataSchema.getDescription() : "业务数据；失败或未命中时常为 null");
            schema.addProperty("data", dataSchema);
        }
        return schema;
    }

    private Schema<?> resolveDataSchema(Type type) {
        if (type instanceof Class<?> clazz) {
            if (Void.class.equals(clazz) || void.class.equals(clazz)) {
                return new Schema<>().description("无业务数据（成功时亦为 null）");
            }
            if (Long.class.equals(clazz) || long.class.equals(clazz)) {
                return new IntegerSchema()
                        .format("int64")
                        .description("新建资源主键 ID")
                        .example(1001L);
            }
            return schemaRef(clazz);
        }
        if (type instanceof ParameterizedType pt) {
            if (pt.getRawType() == PageResultVO.class) {
                return buildPageResultSchema(pt.getActualTypeArguments()[0]);
            }
            if (pt.getRawType() == List.class && pt.getActualTypeArguments().length == 1) {
                ArraySchema array = new ArraySchema();
                array.setItems(schemaRef(asClass(pt.getActualTypeArguments()[0])));
                array.setDescription("列表数据");
                return array;
            }
        }
        return new ObjectSchema().description("业务数据");
    }

    private Schema<?> buildPageResultSchema(Type itemType) {
        ObjectSchema page = new ObjectSchema();
        Class<?> itemClass = asClass(itemType);
        page.setName("PageResult_" + itemClass.getSimpleName());
        page.setDescription("分页响应：total + records（字段名固定为 records）");
        page.addProperty("total", new IntegerSchema()
                .format("int64")
                .description("总记录数")
                .example(100L));
        ArraySchema records = new ArraySchema();
        records.setItems(schemaRef(itemClass));
        records.setDescription("当前页数据列表");
        page.addProperty("records", records);
        return page;
    }

    private Schema<?> schemaRef(Class<?> clazz) {
        ResolvedSchema resolved = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(clazz).resolveAsRef(true));
        if (resolved != null && resolved.schema != null && resolved.schema.get$ref() != null) {
            return new Schema<>().$ref(resolved.schema.get$ref());
        }
        return new Schema<>().$ref("#/components/schemas/" + clazz.getSimpleName());
    }

    private static Class<?> asClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        throw new IllegalArgumentException("Unsupported generic type for OpenAPI: " + type);
    }

    private static String schemaNameForResult(Type dataType) {
        if (dataType instanceof Class<?> clazz) {
            if (Void.class.equals(clazz) || void.class.equals(clazz)) {
                return "Result_Void";
            }
            return "Result_" + clazz.getSimpleName();
        }
        if (dataType instanceof ParameterizedType pt) {
            if (pt.getRawType() == PageResultVO.class && pt.getActualTypeArguments().length == 1) {
                return "Result_Page_" + asClass(pt.getActualTypeArguments()[0]).getSimpleName();
            }
            if (pt.getRawType() == List.class && pt.getActualTypeArguments().length == 1) {
                return "Result_List_" + asClass(pt.getActualTypeArguments()[0]).getSimpleName();
            }
        }
        return "Result";
    }
}
