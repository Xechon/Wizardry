package com.teamwizardry.wizardry.api.spell.module;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.teamwizardry.wizardry.api.spell.SpellRing;
import com.teamwizardry.wizardry.api.spell.annotation.ContextRing;
import com.teamwizardry.wizardry.api.spell.annotation.ModuleOverrideInterface;
import com.teamwizardry.wizardry.api.spell.module.ModuleFactory.OverrideMethod;

public class ModuleOverrideHandler {
	
	private HashMap<String, OverridePointer> overridePointers = new HashMap<>();
	private HashMap<String, Object> cachedProxies = new HashMap<>();

	public ModuleOverrideHandler(SpellRing spellRing) throws ModuleOverrideException {
		if( spellRing.getParentRing() != null )
			throw new IllegalArgumentException("passed spellRing is not a root.");
		
		SpellRing[] spellSequence = getSequenceFromSpellRing(spellRing);
		for( SpellRing curRing : spellSequence )
			applyOverrides(curRing);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getConsumerInterface(Class<T> interfaceClass) throws ModuleOverrideException {
		String className = interfaceClass.getName();
		Object obj = cachedProxies.get(className);
		if( obj == null ) {
			T newProxy = createConsumerInterface(interfaceClass);
			cachedProxies.put(className, newProxy);
			return newProxy;
		}
		
		// check for interface compatibility
		if( !interfaceClass.isInstance(obj) )
			throw new IllegalStateException("Incompatible interface class with matching name. Class loader different?");
		return (T)obj;
	}
	
	private <T> T createConsumerInterface(Class<T> interfaceClass) throws ModuleOverrideException {
		// Retrieve all overridable methods and check them for compatibility with base class
		Map<String, Method> overridableMethods = getInterfaceMethods(interfaceClass);
		
		// Create invocation handler. All interface methods are mapped to their base method pendants
		OverrideInvoker invocationHandler = new OverrideInvoker(overridableMethods);
		
		//
		ClassLoader myClassLoader = getClass().getClassLoader();	// Inherit class loader from this class
		Class<?>[] proxyInterfaces = new Class<?>[] { interfaceClass };
		
		@SuppressWarnings("unchecked")
		T proxy = (T)Proxy.newProxyInstance(myClassLoader, proxyInterfaces, invocationHandler);
		
		return proxy;
	}
	
	private void applyOverrides( SpellRing spellRing ) throws ModuleOverrideException {
		ModuleInstance module = spellRing.getModule();
		Map<String, OverrideMethod> overrides = module.getFactory().getOverrides();
		
		for( Entry<String, OverrideMethod> entry : overrides.entrySet() ) {
			OverridePointer ptr = overridePointers.get(entry.getKey());
			if( ptr == null ) {
				ptr = new OverridePointer(spellRing, null, entry.getKey(), entry.getValue());
			}
			else {
				if( !areMethodsCompatible(ptr.getBaseMethod().getMethod(), entry.getValue().getMethod()) )
					throw new ModuleOverrideException("Method '" + ptr.getBaseMethod() + "' can't be overridden by '" + entry.getValue() + "' due to incompatible signature.");
				ptr = new OverridePointer(spellRing,ptr, entry.getKey(), entry.getValue());
			}
			
			overridePointers.put(entry.getKey(), ptr);			
		}
	}
	
	/////////////////
	
	private static SpellRing[] getSequenceFromSpellRing(SpellRing spellRing) {
		SpellRing cur = spellRing;
		LinkedList<SpellRing> instances = new LinkedList<>();
		while( cur != null ) {
			ModuleInstance module = cur.getModule();
			if( module.getFactory().hasOverrides() ) {
				instances.add(cur);
			}			
			cur = cur.getChildRing();
		}
		
		return instances.toArray(new SpellRing[instances.size()]);
	}
	
	private static boolean areMethodsCompatible(Method baseMtd, Method overrideMtd) {
		// WARNING: Update this method, if language conventions in java change. 
		
		// Check compatibility of return types
		Class<?> baseReturnType = baseMtd.getReturnType();
		Class<?> overrideReturnType = overrideMtd.getReturnType();
		if( baseReturnType == null ) {
			if( overrideReturnType != null )
				return false;
		}
		else {
			if( overrideReturnType == null )
				return false;
			if( !baseReturnType.isAssignableFrom(overrideReturnType) )
				return false;
		}
		
		// Check compatibility of parameters
		Parameter[] baseParams = baseMtd.getParameters();
		Parameter[] overrideParams = overrideMtd.getParameters();

		int i = 0, j = 0;
		while( i < baseParams.length || j < baseParams.length ) {
			if( i >= baseParams.length ) {
				while( j < overrideParams.length ) {
					Parameter overrideParam = overrideParams[j];
					if( !overrideParam.isAnnotationPresent(ContextRing.class) )
						return false;	// Unmappable extra parameter.
					j ++;
				}
				break;
			}
			
			if( j >= overrideParams.length ) {
				while( i < overrideParams.length ) {
					Parameter baseParam = baseParams[i];
					if( !baseParam.isAnnotationPresent(ContextRing.class) )
						return false;	// Unmappable extra parameter.
					i ++;
				}
				break;
			}
			
			// 
			Parameter baseParam = baseParams[i];
			if( baseParam.isAnnotationPresent(ContextRing.class) ) {
				i ++;
				continue;	// Ignore parameters taking values from context
			}
			
			Parameter overrideParam = overrideParams[j];
			if( overrideParam.isAnnotationPresent(ContextRing.class) ) {
				j ++;
				continue;	// Ignore parameters taking values from context
			}
			
			if( !baseParam.getType().isAssignableFrom(overrideParam.getType()) )
				return false;
			
			i ++;
			j ++;
		}
		
		// Check compatibility of exceptions
		Class<?>[] baseExcps = baseMtd.getExceptionTypes();
		Class<?>[] overrideExcps = overrideMtd.getExceptionTypes();
		
		// For every checked exception at the interface method
		// there should exist an exception type at base
		// which is assignable from the interface method exception
		for( Class<?> overrideExcp : overrideExcps ) {
			if( RuntimeException.class.equals(overrideExcp) )
				continue;
			
			boolean found = false;
			for( Class<?> baseExcp : baseExcps ) {
				if( RuntimeException.class.equals(baseExcp) )
					continue;
				if( baseExcp.isAssignableFrom(overrideExcp) ) {
					found = true;
					break;
				}
			}
			if( !found )
				return false;
		}

		return true;		
	}
	
	/////////////////
	
	private class OverrideInvoker implements InvocationHandler {
		private final HashMap<String, OverrideInterfaceMethod> callMap = new HashMap<>();
		
		public OverrideInvoker(Map<String, Method> overrides) throws ModuleOverrideException {
			for( Entry<String, Method> override : overrides.entrySet() ) {
				OverridePointer ptr = overridePointers.get(override.getKey());
				if( ptr == null )
					continue;	// Ignore unmapped methods. invoke() will throw a proper exception on attempt to call them.
				
				OverrideInterfaceMethod intfMethod = new OverrideInterfaceMethod(ptr, override.getValue());
				callMap.put(intfMethod.getKey(), intfMethod);
			}
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String name = method.getName();
			OverrideInterfaceMethod intfMethod = callMap.get(name);
			if( intfMethod == null )
				throw new UnsupportedOperationException("Override '" + name + "' for '" + method + "' is not existing.");
			OverridePointer ptr = intfMethod.getOverridePointer();
			int idxContextParamRing = ptr.getBaseMethod().getIdxContextParamRing();
			
			Object passedArgs[] = args;
			if( idxContextParamRing >= 0 ) {
				// Add spell ring
				passedArgs = new Object[args.length + 1];
				int i = 0;
				int j = 0;
				while( i < passedArgs.length ) {
					if( i == idxContextParamRing ) {
						passedArgs[i] = ptr.getSpellRingWithOverride();
						i ++;
						continue;
					}
					passedArgs[i] = args[j];
					
					i ++;
					j ++;
				}
			}

			try {
				return ptr.getBaseMethod().getMethod().invoke(ptr.getModule().getModuleClass(), passedArgs);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			} catch (IllegalAccessException | IllegalArgumentException e) {
				// NOTE: If this happens, then correctness of checks like "areMethodsCompatible()" a.s.o. need to be debugged.
				e.printStackTrace();
				throw new IllegalStateException("Couldn't invoke call. See cause.", e);
			}
		}
	}
	
	////////////////////////
	
	public static Map<String, Method> getInterfaceMethods(Class<?> clazz) throws ModuleOverrideException {
		HashMap<String, Method> overridableMethods = new HashMap<>();

		// TODO: Check for ambiguity of method names. Handle overrides by superclass properly!
		
		for(Method method : clazz.getMethods()) {
			ModuleOverrideInterface ovrd = method.getDeclaredAnnotation(ModuleOverrideInterface.class);
			if( ovrd == null )
				continue;
			
			try {
				method.setAccessible(true);
			}
			catch(SecurityException e) {
				throw new ModuleOverrideException("Failed to aquire reflection access to method '" + method.toString() + "', annotated by @ModuleOverrideInterface.", e);
			}
			
			overridableMethods.put(ovrd.value(), method);
		}
		
		return overridableMethods;
	}
	
	////////////////////////
	
	private static class OverridePointer {
		private final SpellRing spellRingWithOverride;
		private final String overrideName;
		private final OverrideMethod baseMethod;
		private final OverridePointer prev;
		
		OverridePointer(SpellRing spellRingWithOverride, OverridePointer prev, String overrideName, OverrideMethod baseMethod) {
			this.spellRingWithOverride = spellRingWithOverride;
			this.baseMethod = baseMethod;
			this.overrideName = overrideName;
			this.prev = prev;
		}
		
		SpellRing getSpellRingWithOverride() {
			return spellRingWithOverride;
		}
		
		OverrideMethod getBaseMethod() {
			return this.baseMethod;
		}
		
		ModuleInstance getModule() {
			return spellRingWithOverride.getModule();
		}
		
		OverridePointer getPrev() {
			return prev;
		}
		
		String getOverrideName() {
			return overrideName;
		}
	}
	
	private static class OverrideInterfaceMethod {
		private final OverridePointer overridePointer;
		private final Method interfaceMethod;
		
		OverrideInterfaceMethod(OverridePointer overridePointer, Method interfaceMethod) {
			super();
			this.overridePointer = overridePointer;
			this.interfaceMethod = interfaceMethod;
		}

		OverridePointer getOverridePointer() {
			return overridePointer;
		}

		Method getInterfaceMethod() {
			return interfaceMethod;
		}
		
		String getKey() {
			return interfaceMethod.getName();
		}
	}
}
