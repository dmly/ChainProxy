package com.lyfam.proxy.aspects;

import java.lang.reflect.Method;

import com.lyfam.proxy.invocation.Invoker;
import com.lyfam.proxy.invocation.TargetInvoker;

public class RetryProxy implements Invoker<Retry>
{
    @Override
    public Object execute(Method method, Object[] args, Retry ann,
            TargetInvoker realInvoker) throws Throwable {
        Object result = null;
        
        int retries = 0;
        Class<Throwable>[] retryExceptions = ann.exceptions();
        Throwable captured = null;
        while (retries < ann.times())
        {
            try
            {
                result = realInvoker.invoke();
                break;
            }
            catch (Throwable crap)
            {
                captured = crap;
                if (retryExceptions == null || retryExceptions.length == 0)
                {
                    retries++;
                }
                else
                {
                    for (Class<Throwable> exception : retryExceptions)
                    {
                        if (crap.getClass().isInstance(exception))
                        {
                            retries++;
                            break;
                        }
                    }
                    
                    throw crap;
                }
            }                   
        }
        
        if (retries >= ann.times()) throw new RuntimeException("Exceeded maximum retry policy", captured);
        
        return result;
    }
}
