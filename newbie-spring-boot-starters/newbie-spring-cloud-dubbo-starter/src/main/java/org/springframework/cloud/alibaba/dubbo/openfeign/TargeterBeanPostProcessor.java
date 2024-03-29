
package org.springframework.cloud.alibaba.dubbo.openfeign;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.alibaba.dubbo.metadata.repository.DubboServiceMetadataRepository;
import org.springframework.cloud.alibaba.dubbo.service.DubboGenericServiceExecutionContextFactory;
import org.springframework.cloud.alibaba.dubbo.service.DubboGenericServiceFactory;
import org.springframework.core.env.Environment;

import static java.lang.reflect.Proxy.newProxyInstance;
import static org.springframework.cloud.alibaba.dubbo.autoconfigure.DubboOpenFeignAutoConfiguration.TARGETER_CLASS_NAME;
import static org.springframework.util.ClassUtils.getUserClass;
import static org.springframework.util.ClassUtils.isPresent;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * org.springframework.cloud.openfeign.Targeter {@link BeanPostProcessor}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class TargeterBeanPostProcessor implements BeanPostProcessor, BeanClassLoaderAware {

    private final Environment environment;

    private final DubboServiceMetadataRepository dubboServiceMetadataRepository;

    private final DubboGenericServiceFactory dubboGenericServiceFactory;

    private final DubboGenericServiceExecutionContextFactory contextFactory;

    private ClassLoader classLoader;

    public TargeterBeanPostProcessor(Environment environment,
                                     DubboServiceMetadataRepository dubboServiceMetadataRepository,
                                     DubboGenericServiceFactory dubboGenericServiceFactory,
                                     DubboGenericServiceExecutionContextFactory contextFactory) {
        this.environment = environment;
        this.dubboServiceMetadataRepository = dubboServiceMetadataRepository;
        this.dubboGenericServiceFactory = dubboGenericServiceFactory;
        this.contextFactory = contextFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
        if (isPresent(TARGETER_CLASS_NAME, classLoader)) {
            Class<?> beanClass = getUserClass(bean.getClass());
            Class<?> targetClass = resolveClassName(TARGETER_CLASS_NAME, classLoader);
            if (targetClass.isAssignableFrom(beanClass)) {
                return newProxyInstance(classLoader, new Class[]{targetClass},
                        new TargeterInvocationHandler(bean, environment, classLoader, dubboServiceMetadataRepository,
                                dubboGenericServiceFactory, contextFactory));
            }
        }
        return bean;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}