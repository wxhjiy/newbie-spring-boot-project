
package org.springframework.cloud.alibaba.dubbo.http.util;

import org.springframework.cloud.alibaba.dubbo.http.converter.HttpMessageConverterHolder;
import org.springframework.cloud.alibaba.dubbo.metadata.RequestMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.RestMethodMetadata;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableList;

/**
 * {@link HttpMessageConverter} Resolver
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class HttpMessageConverterResolver {

    private static final MediaType MEDIA_TYPE_APPLICATION = new MediaType("application");

    private final List<HttpMessageConverter<?>> messageConverters;

    private final List<MediaType> allSupportedMediaTypes;

    private final ClassLoader classLoader;

    public HttpMessageConverterResolver(List<HttpMessageConverter<?>> messageConverters, ClassLoader classLoader) {
        this.messageConverters = messageConverters;
        this.allSupportedMediaTypes = getAllSupportedMediaTypes(messageConverters);
        this.classLoader = classLoader;
    }

    public HttpMessageConverterHolder resolve(HttpRequest request, Class<?> parameterType) {

        HttpMessageConverterHolder httpMessageConverterHolder = null;

        HttpHeaders httpHeaders = request.getHeaders();

        MediaType contentType = httpHeaders.getContentType();

        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        for (HttpMessageConverter<?> converter : this.messageConverters) {
            if (converter instanceof GenericHttpMessageConverter) {
                GenericHttpMessageConverter genericConverter = (GenericHttpMessageConverter) converter;
                if (genericConverter.canRead(parameterType, parameterType, contentType)) {
                    httpMessageConverterHolder = new HttpMessageConverterHolder(contentType, converter);
                    break;
                }
            } else {
                if (converter.canRead(parameterType, contentType)) {
                    httpMessageConverterHolder = new HttpMessageConverterHolder(contentType, converter);
                    break;
                }
            }

        }

        return httpMessageConverterHolder;
    }

    /**
     * Resolve the most match {@link HttpMessageConverter} from {@link RequestMetadata}
     *
     * @param requestMetadata    {@link RequestMetadata}
     * @param restMethodMetadata {@link RestMethodMetadata}
     * @return
     */
    public HttpMessageConverterHolder resolve(RequestMetadata requestMetadata, RestMethodMetadata
            restMethodMetadata) {

        HttpMessageConverterHolder httpMessageConverterHolder = null;

        Class<?> returnValueClass = resolveReturnValueClass(restMethodMetadata);

        /**
         *  @see AbstractMessageConverterMethodProcessor#writeWithMessageConverters(Object, MethodParameter, ServletServerHttpRequest, ServletServerHttpResponse)
         */
        List<MediaType> requestedMediaTypes = getAcceptableMediaTypes(requestMetadata);
        List<MediaType> producibleMediaTypes = getProducibleMediaTypes(restMethodMetadata, returnValueClass);

        Set<MediaType> compatibleMediaTypes = new LinkedHashSet<MediaType>();
        for (MediaType requestedType : requestedMediaTypes) {
            for (MediaType producibleType : producibleMediaTypes) {
                if (requestedType.isCompatibleWith(producibleType)) {
                    compatibleMediaTypes.add(getMostSpecificMediaType(requestedType, producibleType));
                }
            }
        }

        if (compatibleMediaTypes.isEmpty()) {
            return httpMessageConverterHolder;
        }

        List<MediaType> mediaTypes = new ArrayList<>(compatibleMediaTypes);

        MediaType.sortBySpecificityAndQuality(mediaTypes);

        MediaType selectedMediaType = null;
        for (MediaType mediaType : mediaTypes) {
            if (mediaType.isConcrete()) {
                selectedMediaType = mediaType;
                break;
            } else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION)) {
                selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
                break;
            }
        }

        if (selectedMediaType != null) {
            selectedMediaType = selectedMediaType.removeQualityValue();
            for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
                if (messageConverter.canWrite(returnValueClass, selectedMediaType)) {
                    httpMessageConverterHolder = new HttpMessageConverterHolder(selectedMediaType, messageConverter);
                    break;
                }
            }
        }

        return httpMessageConverterHolder;
    }

    public List<MediaType> getAllSupportedMediaTypes() {
        return unmodifiableList(allSupportedMediaTypes);
    }

    private Class<?> resolveReturnValueClass(RestMethodMetadata restMethodMetadata) {
        String returnClassName = restMethodMetadata.getMethod().getReturnType();
        return ClassUtils.resolveClassName(returnClassName, classLoader);
    }

    /**
     * Resolve the {@link MediaType media-types}
     *
     * @param requestMetadata {@link RequestMetadata} from client side
     * @return non-null {@link List}
     */
    private List<MediaType> getAcceptableMediaTypes(RequestMetadata requestMetadata) {
        return requestMetadata.getProduceMediaTypes();
    }

    /**
     * Returns
     * the media types that can be produced: <ul> <li>The producible media types specified in the request mappings, or
     * <li>Media types of configured converters that can write the specific return value, or <li>{@link MediaType#ALL}
     * </ul>
     *
     * @param restMethodMetadata {@link RestMethodMetadata} from server side
     * @param returnValueClass   the class of return value
     * @return non-null {@link List}
     */
    private List<MediaType> getProducibleMediaTypes(RestMethodMetadata restMethodMetadata, Class<?>
            returnValueClass) {
        RequestMetadata serverRequestMetadata = restMethodMetadata.getRequest();
        List<MediaType> mediaTypes = serverRequestMetadata.getProduceMediaTypes();
        if (!CollectionUtils.isEmpty(mediaTypes)) { // Empty
            return mediaTypes;
        } else if (!this.allSupportedMediaTypes.isEmpty()) {
            List<MediaType> result = new ArrayList<>();
            for (HttpMessageConverter<?> converter : this.messageConverters) {
                if (converter.canWrite(returnValueClass, null)) {
                    result.addAll(converter.getSupportedMediaTypes());
                }
            }
            return result;
        } else {
            return Collections.singletonList(MediaType.ALL);
        }
    }

    /**
     * Return the media types
     * supported by all provided message converters sorted by specificity via {@link
     * MediaType#sortBySpecificity(List)}.
     *
     * @param messageConverters
     * @return
     */
    private List<MediaType> getAllSupportedMediaTypes(List<HttpMessageConverter<?>> messageConverters) {
        Set<MediaType> allSupportedMediaTypes = new LinkedHashSet<MediaType>();
        for (HttpMessageConverter<?> messageConverter : messageConverters) {
            allSupportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
        }
        List<MediaType> result = new ArrayList<MediaType>(allSupportedMediaTypes);
        MediaType.sortBySpecificity(result);
        return unmodifiableList(result);
    }

    /**
     * Return the more specific of the acceptable and the producible media types
     * with the q-value of the former.
     */
    private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
        MediaType produceTypeToUse = produceType.copyQualityValue(acceptType);
        return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceTypeToUse) <= 0 ? acceptType : produceTypeToUse);
    }
}
