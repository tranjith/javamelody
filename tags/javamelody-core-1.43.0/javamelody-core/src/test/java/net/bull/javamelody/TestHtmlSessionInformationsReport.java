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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Test unitaire de la classe HtmlSessionInformationsReport.
 * @author Emeric Vernat
 */
public class TestHtmlSessionInformationsReport {
	/** Check. */
	@Before
	public void setUp() {
		Utils.initialize();
	}

	private static void assertNotEmptyAndClear(StringWriter writer) {
		assertTrue("rapport vide", writer.getBuffer().length() > 0);
		writer.getBuffer().setLength(0);
	}

	/** Test.
	 * @throws IOException e */
	@Test
	public void testSessionsInformations() throws IOException {
		final List<SessionInformations> sessions = new ArrayList<SessionInformations>();
		sessions.add(new SessionInformations(new SessionTestImpl(true), false));
		sessions.add(new SessionInformations(new SessionTestImpl(false), false));
		final SessionTestImpl serializableButNotSession = new SessionTestImpl(true);
		serializableButNotSession.setAttribute("serializable but not",
				Collections.singleton(new Object()));
		sessions.add(new SessionInformations(serializableButNotSession, false));
		final StringWriter writer = new StringWriter();
		new HtmlSessionInformationsReport(Collections.<SessionInformations> emptyList(), writer)
				.toHtml();
		assertNotEmptyAndClear(writer);

		new HtmlSessionInformationsReport(sessions, writer).toHtml();
		assertNotEmptyAndClear(writer);

		// aucune session sérialisable
		new HtmlSessionInformationsReport(Collections.singletonList(new SessionInformations(
				new SessionTestImpl(false), false)), writer).toHtml();
		assertNotEmptyAndClear(writer);

		// pays non existant
		final SessionTestImpl sessionPays = new SessionTestImpl(true);
		sessionPays.setCountry("nimporte.quoi");
		new HtmlSessionInformationsReport(Collections.singletonList(new SessionInformations(
				sessionPays, false)), writer).toHtml();
		assertNotEmptyAndClear(writer);

		// pays null
		sessionPays.setCountry(null);
		assertNull("countryDisplay null",
				new SessionInformations(sessionPays, false).getCountryDisplay());
		new HtmlSessionInformationsReport(Collections.singletonList(new SessionInformations(
				sessionPays, false)), writer).toHtml();
		assertNotEmptyAndClear(writer);

		new HtmlSessionInformationsReport(null, writer).writeSessionDetails("id session",
				new SessionInformations(new SessionTestImpl(true), true));
		new HtmlSessionInformationsReport(null, writer).writeSessionDetails("id session",
				new SessionInformations(new SessionTestImpl(false), true));
		assertNotEmptyAndClear(writer);
	}
}
