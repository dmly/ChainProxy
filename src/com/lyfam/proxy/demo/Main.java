package com.lyfam.proxy.demo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Random;

import com.lyfam.proxy.aspects.Retry;
import com.lyfam.proxy.aspects.RetryProxy;
import com.lyfam.proxy.invocation.Interceptor;
import com.lyfam.proxy.invocation.Invoker;
import com.lyfam.proxy.invocation.TargetInvoker;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Timeit
{  
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Cache
{  
}

class DummyInvocationHandler implements InvocationHandler
{
	private Object target;
	
	public DummyInvocationHandler(Object target)
	{
		this.target = target;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		System.out.println("** I am dummy handler.");
		return method.invoke(target, args);
	}
}

class ProxyFactory<T>
{
	public static <T> T createProxy(T obj)
	{
		/*obj = (T) Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj
                .getClass().getInterfaces(), new DummyInvocationHandler(obj));*/
		
		obj = Interceptor.createProxy(obj, Retry.class, new RetryProxy());
		
		obj = Interceptor.createProxy(obj, Timeit.class, new Invoker<Timeit>()
		{
			@Override
			public Object execute(Method method, Object[] args, Timeit ann,
					TargetInvoker realInvoker) throws Throwable {
				System.out.println("-- Enter the timer");
				long start = System.nanoTime();
				Object result = realInvoker.invoke();
				System.out.println(String.format("-- %s tookkkk %s ms", method.getName(), (System.nanoTime() - start) / 1000000.0));
				return result;
			}
		});
		
		/*obj = Interceptor.createProxy(obj, Cache.class, new Invoker<Cache>()
		{
			ConcurrentMap<String, ConcurrentMap<Object, Object>> cache = new ConcurrentHashMap<>();
			
			@Override
			public Object execute(Method method, Object[] args, Cache ann,
					RealInvoker realInvoker) throws Throwable {
				
				System.out.println("@@ Entering cache proxy with " + method.getName() + " " + args);
	            ConcurrentMap<Object, Object> theCache = cache.get(method.getName());
	            if (theCache == null)
	            {
	            	theCache = new ConcurrentHashMap<>();
	            	cache.put(method.getName(), theCache);
	            }
	            
	            Object key = args;
	            if (args == null)
	            {
	            	key = "";
	            }
	            Object result = theCache.get(key);
	            if (result != null)
	            {
	            	System.out.println("@@ Cache hit with " + method.getName() + " " + args);
	            }
	            else
	            {
	            	System.out.println("@@ Cache missed with " + method.getName() + " " + args);
	            	result = realInvoker.invoke();
	            	theCache.put(key, result == null ? "" : result);
	            }
	            
	            System.out.println("@@ Exiting cache proxy");
	            
				return result;
			}
		});*/
		
		return obj;
	}
}

public class Main implements Say
{
    public static void main(String... args)
    {
        Say main = new Main();
        main = ProxyFactory.createProxy(main);
        
        for (int i = 0; i < 10; i++)
        {
        	main.say();
        	System.out.println(" ##### \n");
        }
    }

    @Cache
    @Timeit
    @Retry(times=3)
    @Override
    public void say()
    {
    	Random rand = new Random();
    	int r = rand.nextInt(9);
    	if (r > 5)
    	{
    		throw new RuntimeException("CRAP rety!!");
    	}
    	
        System.out.println("Say hello");
    }
}