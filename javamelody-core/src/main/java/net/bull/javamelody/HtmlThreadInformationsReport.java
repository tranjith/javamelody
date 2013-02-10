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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Partie du rapport html pour les threads sur le serveur.
 * @author Emeric Vernat
 */
class HtmlThreadInformationsReport extends HtmlAbstractReport {
	private final List<ThreadInformations> threadInformationsList;
	private final DecimalFormat integerFormat = I18N.createIntegerFormat();
	private final boolean stackTraceEnabled;
	private final boolean cpuTimeEnabled;
	private final boolean systemActionsEnabled = Parameters.isSystemActionsEnabled();

	HtmlThreadInformationsReport(List<ThreadInformations> threadInformationsList,
			boolean stackTraceEnabled, Writer writer) {
		super(writer);
		assert threadInformationsList != null;

		this.threadInformationsList = threadInformationsList;
		this.stackTraceEnabled = stackTraceEnabled;
		this.cpuTimeEnabled = !threadInformationsList.isEmpty()
				&& threadInformationsList.get(0).getCpuTimeMillis() != -1;
	}

	@Override
	void toHtml() throws IOException {
		final HtmlTable table = new HtmlTable();
		table.beginTable(getString("Threads"));
		write("<th>#Thread#</th>");
		write("<th>#Demon#</th><th class='sorttable_numeric'>#Priorite#</th><th>#Etat#</th>");
		if (stackTraceEnabled) {
			write("<th>#Methode_executee#</th>");
		}
		if (cpuTimeEnabled) {
			write("<th class='sorttable_numeric'>#Temps_cpu#</th><th class='sorttable_numeric'>#Temps_user#</th>");
		}
		if (systemActionsEnabled) {
			writeln("<th class='noPrint'>#Tuer#</th>");
		}
		for (final ThreadInformations threadInformations : threadInformationsList) {
			table.nextRow();
			writeThreadInformations(threadInformations);
		}
		table.endTable();
		writeln("<div align='right'>");
		writeln("#Temps_threads#");
		if (stackTraceEnabled) {
			writeln("<br/><a href='?part=threadsDump'><img src='?resource=text.png' alt='#Dump_threads_en_texte#'/>&nbsp;#Dump_threads_en_texte#</a>");
		}
		writeln("</div>");
	}

	void writeDeadlocks() throws IOException {
		final List<ThreadInformations> deadlockedThreads = getDeadLockedThreads();
		if (!deadlockedThreads.isEmpty()) {
			write("<div class='severe'>#Threads_deadlocks#");
			String separator = " ";
			for (final ThreadInformations thread : deadlockedThreads) {
				writeDirectly(separator);
				writeDirectly(htmlEncode(thread.getName()));
				separator = ", ";
			}
			write("</div>");
		}
	}

	void writeThreadsDump() throws IOException {
		final List<ThreadInformations> deadlockedThreads = getDeadLockedThreads();
		if (!deadlockedThreads.isEmpty()) {
			write("#Threads_deadlocks#");
			String separator = " ";
			for (final ThreadInformations thread : deadlockedThreads) {
				writeDirectly(separator);
				writeDirectly(thread.getName());
				separator = ", ";
			}
			writeDirectly("\n\n");
		}
		if (stackTraceEnabled) {
			for (final ThreadInformations threadInformations : threadInformationsList) {
				writeDirectly("\"");
				writeDirectly(threadInformations.getName());
				writeDirectly("\"");
				if (threadInformations.isDaemon()) {
					writeDirectly(" daemon");
				}
				writeDirectly(" prio=");
				writeDirectly(String.valueOf(threadInformations.getPriority()));
				writeDirectly(" ");
				writeDirectly(String.valueOf(threadInformations.getState()));
				final List<StackTraceElement> stackTrace = threadInformations.getStackTrace();
				if (stackTrace != null && !stackTrace.isEmpty()) {
					for (final StackTraceElement element : stackTrace) {
						writeDirectly("\n\t");
						writeDirectly(element.toString());
					}
				}
				writeDirectly("\n\n");
			}
			writeDirectly("\n");
		}
	}

	private List<ThreadInformations> getDeadLockedThreads() {
		final List<ThreadInformations> deadlockedThreads = new ArrayList<ThreadInformations>();
		for (final ThreadInformations thread : threadInformationsList) {
			if (thread.isDeadlocked()) {
				deadlockedThreads.add(thread);
			}
		}
		return deadlockedThreads;
	}

	private void writeThreadInformations(ThreadInformations threadInformations) throws IOException {
		write("<td>");
		writeThreadWithStackTrace(threadInformations);
		write("</td> <td align='center'>");
		if (threadInformations.isDaemon()) {
			write("#oui#");
		} else {
			write("#non#");
		}
		write("</td> <td align='right'>");
		write(integerFormat.format(threadInformations.getPriority()));
		write("</td> <td>");
		write("<img src='?resource=bullets/");
		write(getStateIcon(threadInformations));
		write("' alt='");
		write(String.valueOf(threadInformations.getState()));
		write("'/>");
		write(String.valueOf(threadInformations.getState()));
		if (stackTraceEnabled) {
			write("</td> <td>");
			writeExecutedMethod(threadInformations);
		}
		if (cpuTimeEnabled) {
			write("</td> <td align='right'>");
			write(integerFormat.format(threadInformations.getCpuTimeMillis()));
			write("</td> <td align='right'>");
			write(integerFormat.format(threadInformations.getUserTimeMillis()));
		}
		if (systemActionsEnabled) {
			write("</td> <td align='center' class='noPrint'>");
			write("<a href='?action=kill_thread&amp;threadId=");
			write(threadInformations.getGlobalThreadId());
			final String confirmKillThread = javascriptEncode(getFormattedString(
					"confirm_kill_thread", threadInformations.getName()));
			// writeDirectly pour ne pas gérer de traductions si le nom contient '#'
			writeDirectly("' onclick=\"javascript:return confirm('" + confirmKillThread + "');\">");
			final String title = htmlEncode(getFormattedString("kill_thread",
					threadInformations.getName()));
			writeDirectly("<img width='16' height='16' src='?resource=stop.png' alt='" + title
					+ "' title='" + title + "' />");
			write("</a>");
		}
		write("</td>");
	}

	static String getStateIcon(ThreadInformations threadInformations) {
		switch (threadInformations.getState()) { // NOPMD
		case RUNNABLE:
			return "green.png";
		case WAITING:
			return "yellow.png";
		case TIMED_WAITING:
			if (isSleeping(threadInformations)) {
				return "blue.png";
			}
			return "yellow.png";
		case BLOCKED:
			return "red.png";
		case NEW:
		case TERMINATED:
			return "gray.png";
		default:
			throw new IllegalArgumentException("state inconnu" + threadInformations.getState());
		}
	}

	private static boolean isSleeping(ThreadInformations threadInformations) {
		final List<StackTraceElement> stackTrace = threadInformations.getStackTrace();
		return stackTrace != null && !stackTrace.isEmpty()
				&& "sleep".equals(stackTrace.get(0).getMethodName())
				&& "java.lang.Thread".equals(stackTrace.get(0).getClassName());
	}

	void writeThreadWithStackTrace(ThreadInformations threadInformations) throws IOException {
		final List<StackTraceElement> stackTrace = threadInformations.getStackTrace();
		final String encodedName = htmlEncode(threadInformations.getName());
		if (stackTrace != null && !stackTrace.isEmpty()) {
			// même si stackTraceEnabled, ce thread n'a pas forcément de stack-trace
			writeln("<a class='tooltip'>");
			writeln("<em>");
			// writeDirectly pour ne pas gérer de traductions si le nom contient '#'
			writeDirectly(encodedName);
			writeln("<br/>");
			for (final StackTraceElement stackTraceElement : stackTrace) {
				write(htmlEncode(stackTraceElement.toString()));
				writeln("<br/>");
			}
			writeln("</em>");
			writeDirectly(encodedName);
			writeln("</a>");
		} else {
			// writeDirectly pour ne pas gérer de traductions si le nom contient '#'
			writeDirectly(encodedName);
		}
	}

	void writeExecutedMethod(ThreadInformations threadInformations) throws IOException {
		final String executedMethod = threadInformations.getExecutedMethod();
		if (executedMethod != null && executedMethod.length() != 0) {
			write(htmlEncode(executedMethod));
		} else {
			write("&nbsp;");
		}
	}
}
