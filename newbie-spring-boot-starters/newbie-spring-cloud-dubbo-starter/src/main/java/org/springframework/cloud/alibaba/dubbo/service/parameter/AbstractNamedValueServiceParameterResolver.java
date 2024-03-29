
package org.springframework.cloud.alibaba.dubbo.service.parameter;

import org.springframework.cloud.alibaba.dubbo.http.HttpServerRequest;
import org.springframework.cloud.alibaba.dubbo.metadata.MethodParameterMetadata;
import org.springframework.cloud.alibaba.dubbo.metadata.RestMethodMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Abstract HTTP Names Value {@link DubboGenericServiceParameterResolver Dubbo GenericService Parameter Resolver}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public abstract class AbstractNamedValueServiceParameterResolver extends AbstractDubboGenericServiceParameterResolver {

    /**
     * Get the {@link MultiValueMap} of names and values
     *
     * @param request
     * @return
     */
    protected abstract MultiValueMap<String, String> getNameAndValuesMap(HttpServerRequest request);

    @Override
    public Object resolve(RestMethodMetadata restMethodMetadata, MethodParameterMetadata methodParameterMetadata,
                          HttpServerRequest request) {

        Collection<String> names = getNames(restMethodMetadata, methodParameterMetadata);

        if (isEmpty(names)) { // index can't match
            return null;
        }

        MultiValueMap<String, String> nameAndValues = getNameAndValuesMap(request);

        String targetName = null;

        for (String name : names) {
            if (nameAndValues.containsKey(name)) {
                targetName = name;
                break;
            }
        }

        if (targetName == null) { // request parameter is abstract
            return null;
        }

        Class<?> parameterType = resolveClass(methodParameterMetadata.getType());

        Object paramValue = null;

        if (parameterType.isArray()) { // Array type
            paramValue = nameAndValues.get(targetName);
        } else {
            paramValue = nameAndValues.getFirst(targetName);
        }

        return resolveValue(paramValue, parameterType);
    }

    @Override
    public Object resolve(RestMethodMetadata restMethodMetadata, MethodParameterMetadata methodParameterMetadata,
                          RestMethodMetadata clientRestMethodMetadata, Object[] arguments) {

        Collection<String> names = getNames(restMethodMetadata, methodParameterMetadata);

        if (isEmpty(names)) { // index can't match
            return null;
        }

        Integer index = null;

        Map<Integer, Collection<String>> clientIndexToName = clientRestMethodMetadata.getIndexToName();

        for (Map.Entry<Integer, Collection<String>> entry : clientIndexToName.entrySet()) {

            Collection<String> clientParamNames = entry.getValue();

            if (CollectionUtils.containsAny(names, clientParamNames)) {
                index = entry.getKey();
                break;
            }
        }

        return index > -1 ? arguments[index] : null;
    }

    protected Collection<String> getNames(RestMethodMetadata restMethodMetadata, MethodParameterMetadata methodParameterMetadata) {

        Map<Integer, Collection<String>> indexToName = restMethodMetadata.getIndexToName();

        int index = methodParameterMetadata.getIndex();

        Collection<String> paramNames = indexToName.get(index);

        return paramNames == null ? Collections.emptyList() : paramNames;
    }

}
