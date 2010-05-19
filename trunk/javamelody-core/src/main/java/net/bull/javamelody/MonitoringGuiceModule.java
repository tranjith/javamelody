/*
 * Copyright 2008-2010 by Emeric Vernat
 *
 *     This file is part of Java Melody.
 *
 * Java Melody is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Melody is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java Melody.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.bull.javamelody;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

/**
 * Module Guice pour configurer l'intercepteur de monitoring utilisant l'annotation
 * MonitoringWithGuice, sur des classes et/ou sur des méthodes.<br/>
 * Ce module fait simplement:
 * <code><br/>
 *      // for annotated methods with MonitoredWithGuice<br/>
 *      bindInterceptor(Matchers.any(), Matchers.annotatedWith(MonitoredWithGuice.class),
 *				new MonitoringGuiceInterceptor());<br/>
 *      // and for annotated classes with MonitoredWithGuice<br/>
 *      bindInterceptor(Matchers.annotatedWith(MonitoredWithGuice.class), Matchers.any(),
 *				new MonitoringGuiceInterceptor());
 * </code>
 * @author Emeric Vernat
 */
public class MonitoringGuiceModule extends AbstractModule {
	/** {@inheritDoc} */
	@Override
	protected void configure() {
		// for annotated methods (of implementations) with MonitoredWithGuice
		final MonitoringGuiceInterceptor monitoringGuiceInterceptor = new MonitoringGuiceInterceptor();
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(MonitoredWithGuice.class),
				monitoringGuiceInterceptor);
		// and for annotated classes (of implementations) with MonitoredWithGuice
		bindInterceptor(Matchers.annotatedWith(MonitoredWithGuice.class), Matchers.any(),
				monitoringGuiceInterceptor);
	}
}
