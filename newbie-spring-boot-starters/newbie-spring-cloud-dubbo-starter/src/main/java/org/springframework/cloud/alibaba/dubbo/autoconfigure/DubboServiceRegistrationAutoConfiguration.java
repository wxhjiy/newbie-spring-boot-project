
package org.springframework.cloud.alibaba.dubbo.autoconfigure;

import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ServiceBean;

import com.ecwid.consul.v1.agent.model.NewService;
import com.netflix.appinfo.InstanceInfo;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.alibaba.dubbo.autoconfigure.condition.MissingSpringCloudRegistryConfigPropertyCondition;
import org.springframework.cloud.alibaba.dubbo.metadata.repository.DubboServiceMetadataRepository;
import org.springframework.cloud.alibaba.dubbo.registry.DubboServiceRegistrationEventPublishingAspect;
import org.springframework.cloud.alibaba.dubbo.registry.event.ServiceInstancePreRegisteredEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.alibaba.dubbo.autoconfigure.DubboServiceRegistrationAutoConfiguration.CONSUL_AUTO_CONFIGURATION_CLASS_NAME;
import static org.springframework.cloud.alibaba.dubbo.autoconfigure.DubboServiceRegistrationAutoConfiguration.EUREKA_AUTO_CONFIGURATION_CLASS_NAME;
import static org.springframework.cloud.alibaba.dubbo.registry.SpringCloudRegistryFactory.ADDRESS;
import static org.springframework.cloud.alibaba.dubbo.registry.SpringCloudRegistryFactory.PROTOCOL;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Dubbo Service Registration Auto-{@link Configuration}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
@Configuration
@Import({DubboServiceRegistrationEventPublishingAspect.class})
@ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
@AutoConfigureAfter(name = {
        EUREKA_AUTO_CONFIGURATION_CLASS_NAME,
        CONSUL_AUTO_CONFIGURATION_CLASS_NAME,
        "org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration"
}, value = {
        DubboMetadataAutoConfiguration.class
})
public class DubboServiceRegistrationAutoConfiguration {

    public static final String EUREKA_AUTO_CONFIGURATION_CLASS_NAME =
            "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration";

    public static final String CONSUL_AUTO_CONFIGURATION_CLASS_NAME =
            "org.springframework.cloud.consul.serviceregistry.ConsulAutoServiceRegistrationAutoConfiguration";

    public static final String CONSUL_AUTO_REGISTRATION_CLASS_NAME =
            "org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration";

    public static final String ZOOKEEPER_AUTO_CONFIGURATION_CLASS_NAME =
            "org.springframework.cloud.zookeeper.serviceregistry.ZookeeperAutoServiceRegistrationAutoConfiguration";

    private static final Logger logger = LoggerFactory.getLogger(DubboServiceRegistrationAutoConfiguration.class);

    @Autowired
    private DubboServiceMetadataRepository dubboServiceMetadataRepository;

    @Bean
    @Conditional(value = {
            MissingSpringCloudRegistryConfigPropertyCondition.class
    })
    public RegistryConfig defaultSpringCloudRegistryConfig() {
        return new RegistryConfig(ADDRESS, PROTOCOL);
    }

    @EventListener(ServiceInstancePreRegisteredEvent.class)
    public void onServiceInstancePreRegistered(ServiceInstancePreRegisteredEvent event) {
        Registration registration = event.getSource();
        attachDubboMetadataServiceMetadata(registration);
    }

    @Configuration
    @ConditionalOnBean(name = EUREKA_AUTO_CONFIGURATION_CLASS_NAME)
    @Aspect
    class EurekaConfiguration implements SmartInitializingSingleton {

        @Autowired
        private ObjectProvider<Collection<ServiceBean>> serviceBeans;

        @EventListener(ServiceInstancePreRegisteredEvent.class)
        public void onServiceInstancePreRegistered(ServiceInstancePreRegisteredEvent event) {
            Registration registration = event.getSource();
            EurekaRegistration eurekaRegistration = EurekaRegistration.class.cast(registration);
            InstanceInfo instanceInfo = eurekaRegistration.getApplicationInfoManager().getInfo();
            attachDubboMetadataServiceMetadata(instanceInfo.getMetadata());
        }

        /**
         * {@link EurekaServiceRegistry} will register current {@link ServiceInstance service instance} on
         * {@link EurekaAutoServiceRegistration#start()} execution(in {@link SmartLifecycle#start() start phase}),
         * thus this method must {@link ServiceBean#export() export} all {@link ServiceBean ServiceBeans} in advance.
         */
        @Override
        public void afterSingletonsInstantiated() {
            Collection<ServiceBean> serviceBeans = this.serviceBeans.getIfAvailable();
            if (!isEmpty(serviceBeans)) {
                serviceBeans.forEach(ServiceBean::export);
            }
        }
    }

    @Configuration
    @ConditionalOnBean(name = CONSUL_AUTO_CONFIGURATION_CLASS_NAME)
    @AutoConfigureOrder
    class ConsulConfiguration {

        /**
         * Handle the pre-registered event of {@link ServiceInstance} for Consul
         *
         * @param event {@link ServiceInstancePreRegisteredEvent}
         */
        @EventListener(ServiceInstancePreRegisteredEvent.class)
        public void onServiceInstancePreRegistered(ServiceInstancePreRegisteredEvent event) {
            Registration registration = event.getSource();
            Class<?> registrationClass = AopUtils.getTargetClass(registration);
            String registrationClassName = registrationClass.getName();
            if (CONSUL_AUTO_REGISTRATION_CLASS_NAME.equalsIgnoreCase(registrationClassName)) {
                ConsulRegistration consulRegistration = (ConsulRegistration) registration;
                attachURLsIntoMetadata(consulRegistration);
            }
        }

        private void attachURLsIntoMetadata(ConsulRegistration consulRegistration) {
            NewService newService = consulRegistration.getService();
            Map<String, String> serviceMetadata = dubboServiceMetadataRepository.getDubboMetadataServiceMetadata();
            if (!isEmpty(serviceMetadata)) {
                List<String> tags = newService.getTags();
                for (Map.Entry<String, String> entry : serviceMetadata.entrySet()) {
                    tags.add(entry.getKey() + "=" + entry.getValue());
                }
            }
        }
    }

    private void attachDubboMetadataServiceMetadata(Registration registration) {
        if (registration == null) {
            return;
        }
        synchronized (registration) {
            Map<String, String> metadata = registration.getMetadata();
            attachDubboMetadataServiceMetadata(metadata);
        }
    }

    private void attachDubboMetadataServiceMetadata(Map<String, String> metadata) {
        Map<String, String> serviceMetadata = dubboServiceMetadataRepository.getDubboMetadataServiceMetadata();
        if (!isEmpty(serviceMetadata)) {
            metadata.putAll(serviceMetadata);
        }
    }
}