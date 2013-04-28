package com.lyfam.proxy.invocation;

import java.lang.reflect.*;

public abstract class AbstractInvocationHandler<T> implements InvocationHandler
{

    protected T nextTarget;
    protected T realTarget = null;

    public AbstractInvocationHandler(T target)
    {
        nextTarget = target;
        if (nextTarget != null)
        {
            realTarget = findRealTarget(nextTarget);
            if (realTarget == null)
                throw new RuntimeException("findRealTarget failure");
        }
    }

    protected final T getRealTarget()
    {
        return realTarget;
    }

    protected static final <T> T findRealTarget(T t)
    {
        if (!Proxy.isProxyClass(t.getClass()))
            return t;
        InvocationHandler ih = Proxy.getInvocationHandler(t);
        if (AbstractInvocationHandler.class.isInstance(ih))
        {
            return (T) ((AbstractInvocationHandler) ih).getRealTarget();
        } else
        {
            try
            {
                Field f = findField(ih.getClass(), "target");
                if (Object.class.isAssignableFrom(f.getType())
                        && !f.getType().isArray())
                {

                    f.setAccessible(true); // suppress access checks
                    Object innerTarget = f.get(ih);
                    return (T) findRealTarget(innerTarget);
                }
                return null;
            } catch (NoSuchFieldException e)
            {
                return null;
            } catch (SecurityException e)
            {
                return null;
            } catch (IllegalAccessException e)
            {
                return null;
            } // IllegalArgumentException cannot be raised
        }
    }

    public static Field findField(Class cls, String name)
            throws NoSuchFieldException
    {
        if (cls != null)
        {
            try
            {
                return cls.getDeclaredField(name);
            } catch (NoSuchFieldException e)
            {
                return findField(cls.getSuperclass(), name);
            }
        } else
        {
            throw new NoSuchFieldException();
        }
    }
}
