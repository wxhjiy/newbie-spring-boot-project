
package org.springframework.cloud.alibaba.dubbo.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;

import org.springframework.cloud.alibaba.dubbo.metadata.repository.DubboServiceMetadataRepository;
import org.springframework.cloud.alibaba.dubbo.service.DubboMetadataServiceProxy;
import org.springframework.cloud.alibaba.dubbo.util.JSONUtils;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.ScheduledExecutorService;

import static java.lang.System.getProperty;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * Dubbo {@link RegistryFactory} uses Spring Cloud Service Registration abstraction, whose protocol is "spring-cloud"
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see RegistryFactory
 * @see SpringCloudRegistry
 */
public class SpringCloudRegistryFactory implements RegistryFactory {

    public static String PROTOCOL = "spring-cloud";

    public static String ADDRESS = "localhost";

    private static String SERVICES_LOOKUP_SCHEDULER_THREAD_NAME_PREFIX =
            getProperty("dubbo.services.lookup.scheduler.thread.name.prefix ", "dubbo-services-lookup-");

    private static ConfigurableApplicationContext applicationContext;

    private final ScheduledExecutorService servicesLookupScheduler;

    private DiscoveryClient discoveryClient;

    private DubboServiceMetadataRepository dubboServiceMetadataRepository;

    private DubboMetadataServiceProxy dubboMetadataConfigServiceProxy;

    private JSONUtils jsonUtils;

    private volatile boolean initialized = false;

    public SpringCloudRegistryFactory() {
        servicesLookupScheduler = newSingleThreadScheduledExecutor(
                new NamedThreadFactory(SERVICES_LOOKUP_SCHEDULER_THREAD_NAME_PREFIX));
    }

    protected void init() {
        if (initialized || applicationContext == null) {
            return;
        }
        this.discoveryClient = applicationContext.getBean(DiscoveryClient.class);
        this.dubboServiceMetadataRepository = applicationContext.getBean(DubboServiceMetadataRepository.class);
        this.dubboMetadataConfigServiceProxy = applicationContext.getBean(DubboMetadataServiceProxy.class);
        this.jsonUtils = applicationContext.getBean(JSONUtils.class);
    }

    @Override
    public Registry getRegistry(URL url) {
        init();
        return new SpringCloudRegistry(url, discoveryClient, dubboServiceMetadataRepository,
                dubboMetadataConfigServiceProxy, jsonUtils, servicesLookupScheduler);
    }

    public static void setApplicationContext(ConfigurableApplicationContext applicationContext) {
        SpringCloudRegistryFactory.applicationContext = applicationContext;
    }
}
