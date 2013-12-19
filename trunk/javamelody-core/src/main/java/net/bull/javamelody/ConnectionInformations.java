/*
 * Copyright 2008-2012 by Emeric Vernat
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

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Informations sur l'ouverture d'une connexion jdbc (heure et stack trace).
 * Cet état est celui d'une connexion à un instant t.
 * Les instances sont sérialisables pour pouvoir être transmises au serveur de collecte.
 * @author Emeric Vernat
 */
class ConnectionInformations implements Serializable {
	private static final long serialVersionUID = -6063966419161604125L;
	private static final String OWN_PACKAGE = ConnectionInformations.class.getName().substring(0,
			ConnectionInformations.class.getName().lastIndexOf('.'));
	private static final boolean CONNECTIONS_STACK_TRACES_DISABLED = Boolean
			.parseBoolean(Parameters.getParameter(Parameter.CONNECTIONS_STACK_TRACES_DISABLED));
	private final long openingTime;
	private final StackTraceElement[] openingStackTrace;
	private final long threadId;

	ConnectionInformations() {
		super();
		this.openingTime = System.currentTimeMillis();
		final Thread currentThread = Thread.currentThread();
		if (CONNECTIONS_STACK_TRACES_DISABLED) {
			this.openingStackTrace = null;
		} else {
			this.openingStackTrace = currentThread.getStackTrace();
		}
		this.threadId = currentThread.getId();
	}

	static int getUniqueIdOfConnection(Connection connection) {
		// ce hashCode est normalement implémenté par l'adresse mémoire de l'objet
		// qui est donc unique
		return System.identityHashCode(connection);
	}

	Date getOpeningDate() {
		return new Date(openingTime);
	}

	List<StackTraceElement> getOpeningStackTrace() {
		if (openingStackTrace == null) {
			return Collections.emptyList();
		}
		final List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>(
				Arrays.asList(openingStackTrace));
		// on enlève les premiers éléments qui sont forcément ceux de javamelody
		// (Thread.getStackTrace(), constructeur ConnectionInformations,
		// JdbcWrapper.createConnectionProxy, appelant de createConnectionProxy...)
		stackTrace.remove(0);
		while (stackTrace.get(0).getClassName().startsWith(OWN_PACKAGE)) {
			stackTrace.remove(0);
		}
		return stackTrace;
	}

	long getThreadId() {
		return threadId;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[openingDate=" + getOpeningDate() + ", threadId="
				+ getThreadId() + ']';
	}
}
