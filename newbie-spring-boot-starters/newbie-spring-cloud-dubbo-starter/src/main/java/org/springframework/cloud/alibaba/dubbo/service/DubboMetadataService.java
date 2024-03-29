
package org.springframework.cloud.alibaba.dubbo.service;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.annotation.Service;

import org.springframework.cloud.alibaba.dubbo.metadata.ServiceRestMetadata;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dubbo Metadata Service is a core interface for service subscribers,
 * it must keep the stable of structure in every evolution , makes sure all subscribers' compatibility.
 * <p>
 * The interface contract's version must be {@link #VERSION} constant and group must be current Dubbo application name
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public interface DubboMetadataService {

    /**
     * Current version of the interface contract
     */
    String VERSION = "1.0.0";

    /**
     * Get the json content of {@link ServiceRestMetadata} {@link Set}
     *
     * @return <code>null</code> if present
     */
    String getServiceRestMetadata();


    /**
     * Get all exported {@link URL#getServiceKey() service keys}
     *
     * @return non-null read-only {@link Set}
     */
    Set<String> getAllServiceKeys();

    /**
     * Get all exported Dubbo's {@link URL URLs} {@link Map} whose key is the return value of
     * {@link URL#getServiceKey()} method and value is the json content of List<URL> of {@link URL URLs}
     *
     * @return non-null read-only {@link Map}
     */
    Map<String, String> getAllExportedURLs();

    /**
     * Get the json content of an exported List<URL> of {@link URL URLs} by the serviceInterface , group and version
     *
     * @param serviceInterface The class name of service interface
     * @param group           {@link Service#group() the service group} (optional)
     * @param version         {@link Service#version() the service version} (optional)
     * @return non-null read-only {@link List}
     * @see URL
     */
    String getExportedURLs(String serviceInterface, String group, String version);
}
