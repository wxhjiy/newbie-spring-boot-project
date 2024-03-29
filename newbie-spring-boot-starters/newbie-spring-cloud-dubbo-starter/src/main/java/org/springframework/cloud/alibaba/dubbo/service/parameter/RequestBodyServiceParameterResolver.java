
package org.springframework.cloud.alibaba.dubbo.service.parameter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.alibaba.dubbo.http.HttpServerRequest;
import org.springframework.cloud.alibaba.dubbo.http.converter.HttpMessageConverterHolder;
import org.springframework.cloud.alibaba.dubbo.http.util.HttpMessageConverterResolver;
import org.springframework.cloud.alibaba.dubbo.metadata.MethodParameterMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.RestMethodMetadata;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

/**
 * HTTP Request Body {@link DubboGenericServiceParameterResolver}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class RequestBodyServiceParameterResolver extends AbstractDubboGenericServiceParameterResolver {

    public static final int DEFAULT_ORDER = 7;

    @Autowired
    private ObjectProvider<HttpMessageConverters> httpMessageConverters;

    private HttpMessageConverterResolver httpMessageConverterResolver;

    public RequestBodyServiceParameterResolver() {
        super();
        setOrder(DEFAULT_ORDER);
    }

    @PostConstruct
    public void init() {
        HttpMessageConverters httpMessageConverters = this.httpMessageConverters.getIfAvailable();

        httpMessageConverterResolver = new HttpMessageConverterResolver(httpMessageConverters == null ?
                Collections.emptyList() : httpMessageConverters.getConverters(),
                getClassLoader());
    }

    private boolean supportParameter(RestMethodMetadata restMethodMetadata, MethodParameterMetadata methodParameterMetadata) {

        Integer index = methodParameterMetadata.getIndex();

        Integer bodyIndex = restMethodMetadata.getBodyIndex();

        if (!Objects.equals(index, bodyIndex)) {
            return false;
        }

        Class<?> parameterType = resolveClass(methodParameterMetadata.getType());

        Class<?> bodyType = resolveClass(restMethodMetadata.getBodyType());

        return Objects.equals(parameterType, bodyType);
    }

    @Override
    public Object resolve(RestMethodMetadata restMethodMetadata, MethodParameterMetadata methodParameterMetadata,
                          HttpServerRequest request) {

        if (!supportParameter(restMethodMetadata, methodParameterMetadata)) {
            return null;
        }

        Object result = null;

        Class<?> parameterType = resolveClass(methodParameterMetadata.getType());

        HttpMessageConverterHolder holder = httpMessageConverterResolver.resolve(request, parameterType);

        if (holder != null) {
            HttpMessageConverter converter = holder.getConverter();
            try {
                result = converter.read(parameterType, request);
            } catch (IOException e) {
                throw new HttpMessageNotReadableException("I/O error while reading input message", e);
            }
        }

        return result;
    }

    @Override
    public Object resolve(RestMethodMetadata restMethodMetadata, MethodParameterMetadata methodParameterMetadata,
                          RestMethodMetadata clientRestMethodMetadata, Object[] arguments) {

        if (!supportParameter(restMethodMetadata, methodParameterMetadata)) {
            return null;
        }

        Integer clientBodyIndex = clientRestMethodMetadata.getBodyIndex();
        return arguments[clientBodyIndex];
    }
}
