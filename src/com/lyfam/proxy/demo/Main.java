package com.lyfam.proxy.demo;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.lyfam.proxy.invocation.AbstractInvocationHandler;

interface RealInvoker
{
	public Object invoke() throws Throwable;
}

interface Invoker<A extends Annotation>
{
	public Object execute(final Method method, final Object[] args, A ann, RealInvoker realInvoker) throws Throwable;
}

class Interceptor<T, A extends Annotation> extends AbstractInvocationHandler<T>
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
        	return invoker.execute(method, args, annotation, new RealInvoker() {
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

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Loggable
{
   String sayWhat() default "";  
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Timeit
{  
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Loggable1
{
   String sayWhat() default "";  
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Cache
{  
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Retry
{
	int times() default 3;
}

interface Say
{
    public void say();
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
		
		obj = Interceptor.createProxy(obj, Retry.class, new Invoker<Retry>()
		{
			@Override
			public Object execute(Method method, Object[] args, Retry ann,
					RealInvoker realInvoker) throws Throwable {
				Object result = null;
				
				int retries = 0;
				while (retries < ann.times())
				{
					try
					{
						result = realInvoker.invoke();
						break;
					}
					catch (Throwable crap)
					{
						retries++;
						System.out.println(String.format("Crap catched %s times", retries));
					}					
				}
				
				if (retries >= ann.times()) throw new RuntimeException("Can't handle it anymore");
				
				return result;
			}
		});
		
		obj = Interceptor.createProxy(obj, Timeit.class, new Invoker<Timeit>()
		{
			@Override
			public Object execute(Method method, Object[] args, Timeit ann,
					RealInvoker realInvoker) throws Throwable {
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
		
		obj = Interceptor.createProxy(obj, Loggable.class, new Invoker<Loggable>()
		{
			@Override
			public Object execute(Method method, Object[] args, Loggable loggable,
					RealInvoker realInvoker) throws Throwable {
				String saywhat = loggable.sayWhat();	
	            System.out.println("++ " + saywhat + " Before " + method.getName());
	            Object result = realInvoker.invoke();
	            System.out.println("++ " + saywhat + " After " + method.getName());
				return result;
			}
		});
		
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
    @Loggable(sayWhat="Hello")
    @Timeit
    @Retry
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