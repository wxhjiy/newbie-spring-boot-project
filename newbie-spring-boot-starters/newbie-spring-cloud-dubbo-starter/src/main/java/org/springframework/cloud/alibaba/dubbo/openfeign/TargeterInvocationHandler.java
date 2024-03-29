
package org.springframework.cloud.alibaba.dubbo.openfeign;


import org.apache.dubbo.rpc.service.GenericService;

import feign.Contract;
import feign.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.alibaba.dubbo.annotation.DubboTransported;
import org.springframework.cloud.alibaba.dubbo.metadata.DubboRestServiceMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.DubboTransportedMethodMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.MethodMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.RequestMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.RestMethodMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.repository.DubboServiceMetadataRepository;
import org.springframework.cloud.alibaba.dubbo.metadata.resolver.DubboTransportedMethodMetadataResolver;
import org.springframework.cloud.alibaba.dubbo.service.DubboGenericServiceExecutionContextFactory;
import org.springframework.cloud.alibaba.dubbo.service.DubboGenericServiceFactory;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * org.springframework.cloud.openfeign.Targeter {@link InvocationHandler}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
class TargeterInvocationHandler implements InvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Object bean;

    private final Environment environment;

    private final ClassLoader classLoader;

    private final DubboServiceMetadataRepository repository;

    private final DubboGenericServiceFactory dubboGenericServiceFactory;

    private final DubboGenericServiceExecutionContextFactory contextFactory;

    TargeterInvocationHandler(Object bean, Environment environment,
                              ClassLoader classLoader,
                              DubboServiceMetadataRepository repository,
                              DubboGenericServiceFactory dubboGenericServiceFactory,
                              DubboGenericServiceExecutionContextFactory contextFactory) {
        this.bean = bean;
        this.environment = environment;
        this.classLoader = classLoader;
        this.repository = repository;
        this.dubboGenericServiceFactory = dubboGenericServiceFactory;
        this.contextFactory = contextFactory;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        /**
         * args[0]: FeignClientFactoryBean factory
         * args[1]: Feign.Builder feign
         * args[2]: FeignContext context
         * args[3]: Target.HardCodedTarget<T> target
         */
        FeignContext feignContext = cast(args[2]);
        Target.HardCodedTarget<?> target = cast(args[3]);

        // Execute Targeter#target method first
        method.setAccessible(true);
        // Get the default proxy object
        Object defaultProxy = method.invoke(bean, args);
        // Create Dubbo Proxy if required
        return createDubboProxyIfRequired(feignContext, target, defaultProxy);
    }

    private Object createDubboProxyIfRequired(FeignContext feignContext, Target target, Object defaultProxy) {

        DubboInvocationHandler dubboInvocationHandler = createDubboInvocationHandler(feignContext, target, defaultProxy);

        if (dubboInvocationHandler == null) {
            return defaultProxy;
        }

        return newProxyInstance(target.type().getClassLoader(), new Class<?>[]{target.type()}, dubboInvocationHandler);
    }

    private DubboInvocationHandler createDubboInvocationHandler(FeignContext feignContext, Target target,
                                                                Object defaultFeignClientProxy) {

        // Service name equals @FeignClient.name()
        String serviceName = target.name();
        Class<?> targetType = target.type();

        // Get Contract Bean from FeignContext
        Contract contract = feignContext.getInstance(serviceName, Contract.class);

        DubboTransportedMethodMetadataResolver resolver =
                new DubboTransportedMethodMetadataResolver(environment, contract);

        Map<DubboTransportedMethodMetadata, RestMethodMetadata> feignRestMethodMetadataMap = resolver.resolve(targetType);

        if (feignRestMethodMetadataMap.isEmpty()) { // @DubboTransported method was not found from the Client interface
            if (logger.isDebugEnabled()) {
                logger.debug("@{} method was not found in the Feign target type[{}]",
                        DubboTransported.class.getSimpleName(), targetType.getName());
            }
            return null;
        }

        // Update Metadata
        repository.initialize(serviceName);

        Map<Method, FeignMethodMetadata> feignMethodMetadataMap = getFeignMethodMetadataMap(serviceName, feignRestMethodMetadataMap);

        InvocationHandler defaultFeignClientInvocationHandler = Proxy.getInvocationHandler(defaultFeignClientProxy);

        DubboInvocationHandler dubboInvocationHandler = new DubboInvocationHandler(feignMethodMetadataMap,
                defaultFeignClientInvocationHandler, classLoader, contextFactory);

        return dubboInvocationHandler;
    }

    private Map<Method, FeignMethodMetadata> getFeignMethodMetadataMap(String serviceName,
                                                                       Map<DubboTransportedMethodMetadata, RestMethodMetadata>
                                                                               feignRestMethodMetadataMap) {
        Map<Method, FeignMethodMetadata> feignMethodMetadataMap = new HashMap<>();

        for (Map.Entry<DubboTransportedMethodMetadata, RestMethodMetadata> entry : feignRestMethodMetadataMap.entrySet()) {
            RestMethodMetadata feignRestMethodMetadata = entry.getValue();
            RequestMetadata feignRequestMetadata = feignRestMethodMetadata.getRequest();
            DubboRestServiceMetadata metadata = repository.get(serviceName, feignRequestMetadata);
            if (metadata != null) {
                DubboTransportedMethodMetadata dubboTransportedMethodMetadata = entry.getKey();
                Map<String, Object> dubboTranslatedAttributes = dubboTransportedMethodMetadata.getAttributes();
                Method method = dubboTransportedMethodMetadata.getMethod();
                GenericService dubboGenericService = dubboGenericServiceFactory.create(metadata, dubboTranslatedAttributes);
                RestMethodMetadata dubboRestMethodMetadata = metadata.getRestMethodMetadata();
                MethodMetadata methodMetadata = dubboTransportedMethodMetadata.getMethodMetadata();
                FeignMethodMetadata feignMethodMetadata = new FeignMethodMetadata(dubboGenericService,
                        dubboRestMethodMetadata, feignRestMethodMetadata);
                feignMethodMetadataMap.put(method, feignMethodMetadata);
            }
        }

        return feignMethodMetadataMap;
    }

    private static <T> T cast(Object object) {
        return (T) object;
    }
}
