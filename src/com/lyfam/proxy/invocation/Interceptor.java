package com.lyfam.proxy.invocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Interceptor<T, A extends Annotation> extends AbstractInvocationHandler<T>
{
    private Class<A> annotationClass;
    private Invoker<A> invoker;
    
    private Interceptor(T target, Class<A> annotationClass, Invoker<A> invoker)
    {
        super(target);
        this.annotationClass = annotationClass;
        this.invoker = invoker;
    }
    
    public static <T, A extends Annotation> T createProxy(T obj, Class<A> annotationClass, Invoker<A> invoker)
    {
        return (T) Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj
                .getClass().getInterfaces(), new Interceptor(obj, annotationClass, invoker));
    }
    
    @Override
    public Object invoke(Object proxy, final Method method, final Object[] args)
            throws Throwable {
        Object result = null;
        A annotation = (A) this.realTarget.getClass().getMethod(method.getName()).getAnnotation(annotationClass);
        if (annotation != null)
        {
            return invoker.execute(method, args, annotation, new TargetInvoker() {
                @Override
                public Object invoke() throws Throwable {
                    return method.invoke(Interceptor.this.nextTarget, args);
                }
            });
        }
        else
        {
            result = method.invoke(this.nextTarget, args);
        }

        return result;
    }
    
}
