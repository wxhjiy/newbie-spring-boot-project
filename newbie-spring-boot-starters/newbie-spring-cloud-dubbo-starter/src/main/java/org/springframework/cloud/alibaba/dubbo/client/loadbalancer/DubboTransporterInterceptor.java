
package org.springframework.cloud.alibaba.dubbo.client.loadbalancer;

import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.service.GenericService;

import org.springframework.cloud.alibaba.dubbo.http.MutableHttpServerRequest;
import org.springframework.cloud.alibaba.dubbo.metadata.DubboRestServiceMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.RequestMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.RestMethodMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.repository.DubboServiceMetadataRepository;
import org.springframework.cloud.alibaba.dubbo.service.DubboGenericServiceExecutionContext;
import org.springframework.cloud.alibaba.dubbo.service.DubboGenericServiceExecutionContextFactory;
import org.springframework.cloud.alibaba.dubbo.service.DubboGenericServiceFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.UriComponents;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.springframework.web.util.UriComponentsBuilder.fromUri;

/**
 * Dubbo Transporter {@link ClientHttpRequestInterceptor} implementation
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see LoadBalancerInterceptor
 */
public class DubboTransporterInterceptor implements ClientHttpRequestInterceptor {

    private final DubboServiceMetadataRepository repository;

    private final DubboClientHttpResponseFactory clientHttpResponseFactory;

    private final Map<String, Object> dubboTranslatedAttributes;

    private final DubboGenericServiceFactory serviceFactory;

    private final DubboGenericServiceExecutionContextFactory contextFactory;

    private final PathMatcher pathMatcher = new AntPathMatcher();

    public DubboTransporterInterceptor(DubboServiceMetadataRepository dubboServiceMetadataRepository,
                                       List<HttpMessageConverter<?>> messageConverters,
                                       ClassLoader classLoader,
                                       Map<String, Object> dubboTranslatedAttributes,
                                       DubboGenericServiceFactory serviceFactory,
                                       DubboGenericServiceExecutionContextFactory contextFactory) {
        this.repository = dubboServiceMetadataRepository;
        this.dubboTranslatedAttributes = dubboTranslatedAttributes;
        this.clientHttpResponseFactory = new DubboClientHttpResponseFactory(messageConverters, classLoader);
        this.serviceFactory = serviceFactory;
        this.contextFactory = contextFactory;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        URI originalUri = request.getURI();

        String serviceName = originalUri.getHost();

        RequestMetadata clientMetadata = buildRequestMetadata(request);

        DubboRestServiceMetadata metadata = repository.get(serviceName, clientMetadata);

        if (metadata == null) {
            // if DubboServiceMetadata is not found, executes next
            return execution.execute(request, body);
        }

        RestMethodMetadata dubboRestMethodMetadata = metadata.getRestMethodMetadata();

        GenericService genericService = serviceFactory.create(metadata, dubboTranslatedAttributes);

        MutableHttpServerRequest httpServerRequest = new MutableHttpServerRequest(request, body);

        customizeRequest(httpServerRequest, dubboRestMethodMetadata, clientMetadata);

        DubboGenericServiceExecutionContext context = contextFactory.create(dubboRestMethodMetadata, httpServerRequest);

        Object result = null;
        GenericException exception = null;

        try {
            result = genericService.$invoke(context.getMethodName(), context.getParameterTypes(), context.getParameters());
        } catch (GenericException e) {
            exception = e;
        }

        return clientHttpResponseFactory.build(result, exception, clientMetadata, dubboRestMethodMetadata);
    }

    protected void customizeRequest(MutableHttpServerRequest httpServerRequest,
                                    RestMethodMetadata dubboRestMethodMetadata, RequestMetadata clientMetadata) {

        RequestMetadata dubboRequestMetadata = dubboRestMethodMetadata.getRequest();
        String pathPattern = dubboRequestMetadata.getPath();

        Map<String, String> pathVariables = pathMatcher.extractUriTemplateVariables(pathPattern, httpServerRequest.getPath());

        if (!CollectionUtils.isEmpty(pathVariables)) {
            // Put path variables Map into query parameters Map
            httpServerRequest.params(pathVariables);
        }

    }

    private RequestMetadata buildRequestMetadata(HttpRequest request) {
        UriComponents uriComponents = fromUri(request.getURI()).build(true);
        RequestMetadata requestMetadata = new RequestMetadata();
        requestMetadata.setPath(uriComponents.getPath());
        requestMetadata.setMethod(request.getMethod().name());
        requestMetadata.setParams(uriComponents.getQueryParams());
        requestMetadata.setHeaders(request.getHeaders());
        return requestMetadata;
    }
}
