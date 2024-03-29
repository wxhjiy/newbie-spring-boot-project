
package org.springframework.cloud.alibaba.dubbo.client.loadbalancer;

import org.apache.dubbo.rpc.service.GenericException;
import org.springframework.cloud.alibaba.dubbo.http.converter.HttpMessageConverterHolder;
import org.springframework.cloud.alibaba.dubbo.http.util.HttpMessageConverterResolver;
import org.springframework.cloud.alibaba.dubbo.metadata.RequestMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.RestMethodMetadata;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;

import java.io.IOException;
import java.util.List;

/**
 * Dubbo {@link ClientHttpResponse} Factory
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
class DubboClientHttpResponseFactory {

    private final HttpMessageConverterResolver httpMessageConverterResolver;

    public DubboClientHttpResponseFactory(List<HttpMessageConverter<?>> messageConverters, ClassLoader classLoader) {
        this.httpMessageConverterResolver = new HttpMessageConverterResolver(messageConverters, classLoader);
    }

    public ClientHttpResponse build(Object result, GenericException exception,
                                    RequestMetadata requestMetadata, RestMethodMetadata restMethodMetadata) {

        DubboHttpOutputMessage httpOutputMessage = new DubboHttpOutputMessage();

        HttpMessageConverterHolder httpMessageConverterHolder = httpMessageConverterResolver.resolve(requestMetadata, restMethodMetadata);

        if (httpMessageConverterHolder != null) {
            MediaType mediaType = httpMessageConverterHolder.getMediaType();
            HttpMessageConverter converter = httpMessageConverterHolder.getConverter();
            try {
                converter.write(result, mediaType, httpOutputMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new DubboClientHttpResponse(httpOutputMessage, exception);
    }
}
