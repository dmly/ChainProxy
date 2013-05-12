package com.lyfam.proxy.invocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface Invoker<A extends Annotation>
{
    public Object execute(final Method method, final Object[] args, A ann, TargetInvoker realInvoker) throws Throwable;
}
