/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bull.javamelody;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

/**
 * Persistence provider pour monitorer JPA.
 * From Sirona, http://sirona.incubator.apache.org/
 */
public class JpaPersistence implements PersistenceProvider {
	// TODO tenir compte de DISABLED et de isDisplayed comme dans MonitoringSpringInterceptor

	private static final String OWN_PACKAGE = JpaPersistence.class.getName().substring(0,
			JpaPersistence.class.getName().lastIndexOf('.'));
	private static final String DELEGATE_PROVIDER_KEY = OWN_PACKAGE + ".jpa.provider";

	private static final String[] PROVIDERS = {
			"org.apache.openjpa.persistence.PersistenceProviderImpl",
			"org.hibernate.jpa.HibernatePeristenceProvider",
			"org.hibernate.ejb.HibernatePeristence",
			"org.eclipse.persistence.jpa.PersistenceProvider",
			"oracle.toplink.essentials.ejb.cmp3.EntityManagerFactoryProvider",
			"oracle.toplink.essentials.PersistenceProvider",
			"me.prettyprint.hom.CassandraPersistenceProvider",
			"org.datanucleus.jpa.PersistenceProviderImpl",
			"com.orientechnologies.orient.core.db.object.jpa.OJPAPersistenceProvider",
			"com.orientechnologies.orient.object.jpa.OJPAPersistenceProvider",
			"com.spaceprogram.simplejpa.PersistenceProviderImpl" };

	private volatile PersistenceProvider delegate; // NOPMD

	/** {@inheritDoc} */
	@SuppressWarnings("rawtypes")
	@Override
	public EntityManagerFactory createEntityManagerFactory(final String unit, final Map map) {
		final PersistenceProvider persistenceProvider = findDelegate(map);
		final ClassLoader tccl = tccl();

		final ClassLoader hack = new JpaOverridePersistenceXmlClassLoader(tccl, persistenceProvider
				.getClass().getName());
		Thread.currentThread().setContextClassLoader(hack);
		try {
			final EntityManagerFactory entityManagerFactory = persistenceProvider
					.createEntityManagerFactory(unit, map);
			if (entityManagerFactory == null) {
				return null;
			}
			return JpaWrapper.createEntityManagerFactoryProxy(entityManagerFactory);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	/** {@inheritDoc} */
	@SuppressWarnings("rawtypes")
	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(final PersistenceUnitInfo info,
			final Map map) {
		final PersistenceProvider persistenceProvider = findDelegate(map);
		// on surcharge PersistenceUnitInfo.getPersistenceProviderClassName()
		// pour retourner le PersistenceProvider délégué et pas nous même
		final PersistenceUnitInfo proxiedInfo = createPersistentUnitInfoProxy(info,
				persistenceProvider);
		final EntityManagerFactory entityManagerFactory = persistenceProvider
				.createContainerEntityManagerFactory(proxiedInfo, map);
		if (entityManagerFactory == null) {
			return null;
		}
		return JpaWrapper.createEntityManagerFactoryProxy(entityManagerFactory);
	}

	private PersistenceUnitInfo createPersistentUnitInfoProxy(final PersistenceUnitInfo info,
			final PersistenceProvider persistenceProvider) {
		final InvocationHandler invocationHandler = new ProviderAwareHandler(persistenceProvider
				.getClass().getName(), info);
		return JdbcWrapper.createProxy(info, invocationHandler);
	}

	/** {@inheritDoc} */
	@Override
	public ProviderUtil getProviderUtil() { // we suppose it is loaded later than createXXXEMF so we'll get the delegate
		return loadOrGuessDelegate(null).getProviderUtil();
	}

	private PersistenceProvider findDelegate(final Map<?, ?> map) {
		if (map == null) {
			return loadOrGuessDelegate(null);
		}
		return loadOrGuessDelegate(String.class.cast(map.get(DELEGATE_PROVIDER_KEY)));
	}

	private PersistenceProvider loadOrGuessDelegate(final String name) {
		if (delegate == null) {
			synchronized (this) {
				if (delegate == null) {
					if (name == null) {
						guessDelegate();
					} else {
						try {
							delegate = newPersistence(name);
						} catch (final Exception e) {
							throw new IllegalStateException(new ClassNotFoundException(
									"Can't instantiate '" + name + "'", e));
						}
					}
				}
			}
		}
		if (name != null && !delegate.getClass().getName().equals(name)) {
			try {
				return newPersistence(name);
			} catch (final Exception e) {
				throw new IllegalStateException(new ClassNotFoundException("Can't instantiate '"
						+ name + "'", e));
			}
		}
		return delegate;
	}

	private void guessDelegate() {
		for (final String provider : PROVIDERS) {
			try {
				delegate = newPersistence(provider);
			} catch (final Throwable th2) { // NOPMD
				continue;
			}
		}
		if (delegate == null) { // NOPMD
			throw new IllegalStateException(new ClassNotFoundException("Can't find a delegate"));
		}
	}

	private static ClassLoader tccl() {
		return Thread.currentThread().getContextClassLoader();
	}

	private static PersistenceProvider newPersistence(final String name) throws Exception { // NOPMD
		return PersistenceProvider.class.cast(tccl().loadClass(name).newInstance());
	}

	private static class ProviderAwareHandler implements InvocationHandler {
		private final String provider;
		private final PersistenceUnitInfo info;

		ProviderAwareHandler(final String provider, final PersistenceUnitInfo info) {
			super();
			this.provider = provider;
			this.info = info;
		}

		/** {@inheritDoc} */
		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args)
				throws Throwable {
			if ("getPersistenceProviderClassName".equals(method.getName())) {
				return provider;
			}
			return method.invoke(info, args);
		}
	}
}
