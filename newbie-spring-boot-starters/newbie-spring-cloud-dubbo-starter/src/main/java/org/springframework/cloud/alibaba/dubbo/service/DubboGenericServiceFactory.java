
package org.springframework.cloud.alibaba.dubbo.service;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.rpc.service.GenericService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.cloud.alibaba.dubbo.metadata.DubboRestServiceMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.ServiceRestMetadata;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import javax.annotation.PreDestroy;
import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.emptyMap;
import static org.apache.dubbo.common.Constants.GROUP_KEY;
import static org.apache.dubbo.common.Constants.VERSION_KEY;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;

/**
 * Dubbo {@link GenericService} Factory
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class DubboGenericServiceFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConcurrentMap<Integer, ReferenceBean<GenericService>> cache = new ConcurrentHashMap<>();

    @Autowired
    private ObjectProvider<List<RegistryConfig>> registryConfigs;

    public GenericService create(DubboRestServiceMetadata dubboServiceMetadata,
                                 Map<String, Object> dubboTranslatedAttributes) {

        ReferenceBean<GenericService> referenceBean = build(dubboServiceMetadata.getServiceRestMetadata(), dubboTranslatedAttributes);

        return referenceBean == null ? null : referenceBean.get();
    }

    public GenericService create(String serviceName, Class<?> serviceClass, String version) {
        String interfaceName = serviceClass.getName();
        ReferenceBean<GenericService> referenceBean = build(interfaceName, version, serviceName, emptyMap());
        return referenceBean.get();
    }


    private ReferenceBean<GenericService> build(ServiceRestMetadata serviceRestMetadata,
                                                Map<String, Object> dubboTranslatedAttributes) {
        String urlValue = serviceRestMetadata.getUrl();
        URL url = URL.valueOf(urlValue);
        String interfaceName = url.getServiceInterface();
        String version = url.getParameter(VERSION_KEY);
        String group = url.getParameter(GROUP_KEY);

        return build(interfaceName, version, group, dubboTranslatedAttributes);
    }

    private ReferenceBean<GenericService> build(String interfaceName, String version, String group,
                                                Map<String, Object> dubboTranslatedAttributes) {

        Integer key = Objects.hash(interfaceName, version, group, dubboTranslatedAttributes);

        return cache.computeIfAbsent(key, k -> {
            ReferenceBean<GenericService> referenceBean = new ReferenceBean<>();
            referenceBean.setGeneric(true);
            referenceBean.setInterface(interfaceName);
            referenceBean.setVersion(version);
            referenceBean.setGroup(group);
            bindReferenceBean(referenceBean, dubboTranslatedAttributes);
            return referenceBean;
        });
    }

    private void bindReferenceBean(ReferenceBean<GenericService> referenceBean, Map<String, Object> dubboTranslatedAttributes) {
        DataBinder dataBinder = new DataBinder(referenceBean);
        // Register CustomEditors for special fields
        dataBinder.registerCustomEditor(String.class, "filter", new StringTrimmerEditor(true));
        dataBinder.registerCustomEditor(String.class, "listener", new StringTrimmerEditor(true));
        dataBinder.registerCustomEditor(Map.class, "parameters", new PropertyEditorSupport() {

            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                // Trim all whitespace
                String content = StringUtils.trimAllWhitespace(text);
                if (!StringUtils.hasText(content)) { // No content , ignore directly
                    return;
                }
                // replace "=" to ","
                content = StringUtils.replace(content, "=", ",");
                // replace ":" to ","
                content = StringUtils.replace(content, ":", ",");
                // String[] to Map
                Map<String, String> parameters = CollectionUtils.toStringMap(commaDelimitedListToStringArray(content));
                setValue(parameters);
            }
        });

        // ignore "registries" field and then use RegistryConfig beans
        dataBinder.setDisallowedFields("registries");

        dataBinder.bind(new MutablePropertyValues(dubboTranslatedAttributes));

        registryConfigs.ifAvailable(referenceBean::setRegistries);
    }

    @PreDestroy
    public void destroy() {
        destroyReferenceBeans();
        cache.values();
    }

    private void destroyReferenceBeans() {
        Collection<ReferenceBean<GenericService>> referenceBeans = cache.values();
        if (logger.isInfoEnabled()) {
            logger.info("The Dubbo GenericService ReferenceBeans are destroying...");
        }
        for (ReferenceBean referenceBean : referenceBeans) {
            referenceBean.destroy(); // destroy ReferenceBean
            if (logger.isInfoEnabled()) {
                logger.info("Destroyed the ReferenceBean  : {} ", referenceBean);
            }
        }
    }

}
