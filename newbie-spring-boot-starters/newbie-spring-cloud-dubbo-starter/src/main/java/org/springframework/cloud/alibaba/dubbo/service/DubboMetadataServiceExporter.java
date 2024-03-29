
package org.springframework.cloud.alibaba.dubbo.service;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ServiceConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link DubboMetadataService} exporter
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
@Component
public class DubboMetadataServiceExporter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    private ObjectProvider<DubboMetadataService> dubboMetadataService;

    @Autowired
    private Supplier<ProtocolConfig> protocolConfigSupplier;

    @Value("${spring.application.name:${dubbo.application.name:application}}")
    private String currentApplicationName;

    /**
     * The ServiceConfig of DubboMetadataConfigService to be exported, can be nullable.
     */
    private ServiceConfig<DubboMetadataService> serviceConfig;

    /**
     * export {@link DubboMetadataService} as Dubbo service
     *
     * @return the exported {@link URL URLs}
     */
    public List<URL> export() {

        if (serviceConfig == null || !serviceConfig.isExported()) {

            serviceConfig = new ServiceConfig<>();

            serviceConfig.setInterface(DubboMetadataService.class);
            //  Use DubboMetadataService.VERSION as the Dubbo Service version
            serviceConfig.setVersion(DubboMetadataService.VERSION);
            // Use current Spring application name as the Dubbo Service group
            serviceConfig.setGroup(currentApplicationName);
            serviceConfig.setRef(dubboMetadataService.getIfAvailable());
            serviceConfig.setApplication(applicationConfig);
            serviceConfig.setProtocol(protocolConfigSupplier.get());

            serviceConfig.export();

            if (logger.isInfoEnabled()) {
                logger.info("The Dubbo service[{}] has been exported.", serviceConfig.toString());
            }
        }

        return serviceConfig.getExportedUrls();
    }


    /**
     * unexport {@link DubboMetadataService}
     */
    @PreDestroy
    public void unexport() {

        if (serviceConfig == null || serviceConfig.isUnexported()) {
            return;
        }

        serviceConfig.unexport();

        if (logger.isInfoEnabled()) {
            logger.info("The Dubbo service[{}] has been unexported.", serviceConfig.toString());
        }
    }
}
