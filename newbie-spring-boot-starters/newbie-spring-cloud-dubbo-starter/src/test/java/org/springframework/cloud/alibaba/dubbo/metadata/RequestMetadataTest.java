
package org.springframework.cloud.alibaba.dubbo.metadata;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link RequestMetadata} Test
 *
 *
 */
public class RequestMetadataTest {

    private String method = "GET";

    private String url = "/param";

    private Set<String> paramNames = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));

    private Set<String> headerNames = new LinkedHashSet<>(Arrays.asList("d", "e", "f"));

    @Test
    public void testEqualsAndHashCodeAndCompareTo() {

        RequestMetadata metadata = new RequestMetadata();
        RequestMetadata metadata2 = new RequestMetadata();

        Assert.assertEquals(metadata, metadata2);
        Assert.assertEquals(metadata.hashCode(), metadata2.hashCode());

        metadata.setMethod(method);
        metadata2.setMethod(method);

        Assert.assertEquals(metadata, metadata2);
        Assert.assertEquals(metadata.hashCode(), metadata2.hashCode());

        metadata.setPath(url);
        metadata2.setPath(url);

        Assert.assertEquals(metadata, metadata2);
        Assert.assertEquals(metadata.hashCode(), metadata2.hashCode());

        metadata.addParam("a", "1").addParam("b", "2").addParam("c", "3");
        metadata2.addParam("a", "1a").addParam("b", "2b").addParam("c", "3c");

        Assert.assertEquals(metadata, metadata2);
        Assert.assertEquals(metadata.hashCode(), metadata2.hashCode());

        metadata.addHeader("d", "1").addHeader("e", "2").addHeader("f", "3");
        metadata2.addHeader("d", "1").addHeader("e", "2");

        Assert.assertNotEquals(metadata, metadata2);
        Assert.assertNotEquals(metadata.hashCode(), metadata2.hashCode());
    }

//    @Test
//    public void testBestMatch() {
//
//        NavigableMap<RequestMetadata, RequestMetadata> requestMetadataMap = new TreeMap<>();
//
//        RequestMetadata metadata = new RequestMetadata();
//        metadata.setMethod(method);
//
//        RequestMetadata metadata1 = new RequestMetadata();
//        metadata1.setMethod(method);
//        metadata1.setPath(url);
//
//        RequestMetadata metadata2 = new RequestMetadata();
//        metadata2.setMethod(method);
//        metadata2.setPath(url);
//        metadata2.setParams(paramNames);
//
//        RequestMetadata metadata3 = new RequestMetadata();
//        metadata3.setMethod(method);
//        metadata3.setPath(url);
//        metadata3.setParams(paramNames);
//        metadata3.setHeaders(headerNames);
//
//        requestMetadataMap.put(metadata, metadata);
//        requestMetadataMap.put(metadata1, metadata1);
//        requestMetadataMap.put(metadata2, metadata2);
//        requestMetadataMap.put(metadata3, metadata3);
//
//        RequestMetadata result = getBestMatch(requestMetadataMap, metadata);
//        Assert.assertEquals(result, metadata);
//
//        result = getBestMatch(requestMetadataMap, metadata1);
//        Assert.assertEquals(result, metadata1);
//
//        result = getBestMatch(requestMetadataMap, metadata2);
//        Assert.assertEquals(result, metadata2);
//
//        result = getBestMatch(requestMetadataMap, metadata3);
//        Assert.assertEquals(result, metadata3);
//
//        // REDO
//        requestMetadataMap.clear();
//
//        requestMetadataMap.put(metadata1, metadata1);
//
//        result = getBestMatch(requestMetadataMap, metadata2);
//        Assert.assertEquals(metadata1, result);
//
//        requestMetadataMap.put(metadata2, metadata2);
//
//        result = getBestMatch(requestMetadataMap, metadata3);
//        Assert.assertEquals(metadata2, result);
//
//        result = getBestMatch(requestMetadataMap, new RequestMetadata());
//        Assert.assertNull(result);
//
//        result = getBestMatch(requestMetadataMap, metadata);
//        Assert.assertNull(result);
//
//    }

}
