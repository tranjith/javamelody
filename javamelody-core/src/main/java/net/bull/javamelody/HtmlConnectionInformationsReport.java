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

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Partie du rapport html pour les connections jdbc ouvertes.
 * @author Emeric Vernat
 */
class HtmlConnectionInformationsReport extends HtmlAbstractReport {
	private final List<ConnectionInformations> connectionsInformations;
	private final DateFormat dateTimeFormat = I18N.createDateAndTimeFormat();
	private final Map<Long, Thread> threadsById;
	private final Map<Thread, StackTraceElement[]> stackTracesByThread;

	HtmlConnectionInformationsReport(List<ConnectionInformations> connectionsInformations,
			Writer writer) {
		super(writer);
		assert connectionsInformations != null;
		this.connectionsInformations = connectionsInformations;
		// rq: cette partie du rapport n'est pas exécutée sur le serveur de collecte
		// donc les threads sont ok
		if (JavaInformations.STACK_TRACES_ENABLED) {
			this.stackTracesByThread = Thread.getAllStackTraces();
			this.threadsById = new HashMap<Long, Thread>(stackTracesByThread.size());
			for (final Thread thread : stackTracesByThread.keySet()) {
				this.threadsById.put(thread.getId(), thread);
			}
		} else {
			this.stackTracesByThread = Collections.emptyMap();
			final List<Thread> threads = JavaInformations.getThreadsFromThreadGroups();
			this.threadsById = new HashMap<Long, Thread>(threads.size());
			for (final Thread thread : threads) {
				this.threadsById.put(thread.getId(), thread);
			}
		}
	}

	@Override
	void toHtml() throws IOException {
		writeBackAndRefreshLinks();
		writeln("<br/>");

		writeTitle("db.png", getString("Connexions_jdbc_ouvertes"));
		writeln("<br/>#connexions_intro#<br/><br/>");
		writeConnections();
	}

	void writeConnections() throws IOException {
		if (connectionsInformations.isEmpty()) {
			writeln("#Aucune_connexion_jdbc_ouverte#");
			return;
		}
		final HtmlTable table = new HtmlTable();
		table.beginTable(getString("Connexions_jdbc_ouvertes"));
		write("<th class='sorttable_date'>#Date_et_stack_trace_ouverture#</th>");
		if (JavaInformations.STACK_TRACES_ENABLED) {
			write("<th>#Thread_et_stack_trace_actuelle#</th>");
		} else {
			write("<th>#Thread#</th>");
		}
		for (final ConnectionInformations connection : connectionsInformations) {
			table.nextRow();
			writeConnection(connection);
		}
		table.endTable();
		final int nbConnections = connectionsInformations.size();
		writeln("<div align='right'>" + getFormattedString("nb_connexions_ouvertes", nbConnections)
				+ "</div>");
	}

	private void writeBackAndRefreshLinks() throws IOException {
		writeln("<div class='noPrint'>");
		writeln("<a href='javascript:history.back()'>");
		writeln("<img src='?resource=action_back.png' alt='#Retour#'/> #Retour#</a>");
		writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		writeln("<a href='?part=connections'>");
		writeln("<img src='?resource=action_refresh.png' alt='#Actualiser#'/> #Actualiser#</a>");
		writeln("</div>");
	}

	private void writeConnection(ConnectionInformations connection) throws IOException {
		write("<td align='right'>");
		writeTextWithStackTrace(dateTimeFormat.format(connection.getOpeningDate()),
				connection.getOpeningStackTrace());
		write("</td><td>");
		final Thread thread = threadsById.get(connection.getThreadId());
		if (thread == null) {
			write("&nbsp;");
		} else {
			final StackTraceElement[] stackTrace = stackTracesByThread.get(thread);
			writeTextWithStackTrace(thread.getName(),
					stackTrace != null ? Arrays.asList(stackTrace) : null);
		}
		write("</td>");
	}

	private void writeTextWithStackTrace(String text, List<StackTraceElement> stackTrace)
			throws IOException {
		final String encodedText = htmlEncode(text);
		if (stackTrace != null && !stackTrace.isEmpty()) {
			// même si stackTraceEnabled, ce thread n'a pas forcément de stack-trace
			writeln("<a class='tooltip'>");
			writeln("<em>");
			// writeDirectly pour ne pas gérer de traductions si le texte contient '#'
			writeDirectly(encodedText);
			writeln("<br/>");
			for (final StackTraceElement stackTraceElement : stackTrace) {
				writeln(htmlEncode(stackTraceElement.toString()));
				writeln("<br/>");
			}
			writeln("</em>");
			writeDirectly(encodedText);
			writeln("</a>");
		} else {
			// writeDirectly pour ne pas gérer de traductions si le texte contient '#'
			writeDirectly(encodedText);
		}
	}
}
