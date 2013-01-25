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
import java.util.List;

/**
 * Partie du rapport html pour les caches de données.
 * @author Emeric Vernat
 */
class HtmlCacheInformationsReport extends HtmlAbstractReport {
	private final List<CacheInformations> cacheInformationsList;
	private final DecimalFormat integerFormat = I18N.createIntegerFormat();
	private final boolean hitsRatioEnabled;
	private final boolean configurationEnabled;
	private final boolean systemActionsEnabled = Parameters.isSystemActionsEnabled();

	HtmlCacheInformationsReport(List<CacheInformations> cacheInformationsList, Writer writer) {
		super(writer);
		assert cacheInformationsList != null;

		this.cacheInformationsList = cacheInformationsList;
		this.hitsRatioEnabled = isHitsRatioEnabled(cacheInformationsList);
		this.configurationEnabled = isConfigurationEnabled(cacheInformationsList);
	}

	@Override
	void toHtml() throws IOException {
		writeln("<table class='sortable' width='100%' border='1' cellspacing='0' cellpadding='2' summary='#Caches#'>");
		write("<thead><tr>");
		write("<th>#Cache#</th>");
		if (configurationEnabled) {
			write("<th class='sorttable_numeric'>#Pourcentage_memoire_utilise#</th>");
		}
		write("<th class='sorttable_numeric'>#Nb_objets_en_memoire#</th>");
		write("<th class='sorttable_numeric'>#Nb_objets_sur_disque#</th>");
		if (hitsRatioEnabled) {
			write("<th class='sorttable_numeric'>");
			write(getString("Efficacite_cache_memoire").replaceAll("\n", "<br/>"));
			write("</th><th class='sorttable_numeric'>");
			write(getString("Efficacite_cache").replaceAll("\n", "<br/>"));
			write("</th>");
		}
		if (configurationEnabled) {
			write("<th>#Configuration#</th>");
		}
		if (systemActionsEnabled) {
			write("<th class='noPrint'>#Purger#</th>");
		}
		writeln("</tr></thead><tbody>");
		boolean odd = false;
		for (final CacheInformations cacheInformations : cacheInformationsList) {
			if (odd) {
				write("<tr class='odd' onmouseover=\"this.className='highlight'\" onmouseout=\"this.className='odd'\">");
			} else {
				write("<tr onmouseover=\"this.className='highlight'\" onmouseout=\"this.className=''\">");
			}
			odd = !odd; // NOPMD
			writeCacheInformations(cacheInformations);
			writeln("</tr>");
		}
		writeln("</tbody></table>");
		write("<div align='right' class='noPrint'>");
		if (!hitsRatioEnabled) {
			writeln("#caches_statistics_enable#<br/>");
		}
		if (systemActionsEnabled) {
			writeln("<a href='?action=clear_caches' onclick=\"javascript:return confirm('"
					+ getStringForJavascript("confirm_purge_caches") + "');\">");
			writeln("<img src='?resource=user-trash.png' width='18' height='18' alt=\"#Purge_caches#\" /> #Purge_caches#</a>");
			writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		}
		// writeDirectly pour éviter traduction car # dans l'url
		writeDirectly("<a href='http://ehcache.sourceforge.net/apidocs/net/sf/ehcache/config/CacheConfiguration.html#field_summary'");
		writeln("target='_blank'>Configuration reference</a></div>");
	}

	private void writeCacheInformations(CacheInformations cacheInformations) throws IOException {
		write("<td>");
		writeDirectly(htmlEncodeButNotSpace(cacheInformations.getName()));
		final String nextColumnAlignRight = "</td> <td align='right'>";
		if (configurationEnabled) {
			write(nextColumnAlignRight);
			write(integerFormat.format(cacheInformations.getInMemoryPercentUsed()));
		}
		write(nextColumnAlignRight);
		write(integerFormat.format(cacheInformations.getInMemoryObjectCount()));
		write(nextColumnAlignRight);
		write(integerFormat.format(cacheInformations.getOnDiskObjectCount()));
		if (hitsRatioEnabled) {
			write(nextColumnAlignRight);
			write(integerFormat.format(cacheInformations.getInMemoryHitsRatio()));
			write(nextColumnAlignRight);
			write(integerFormat.format(cacheInformations.getHitsRatio()));
		}
		if (configurationEnabled) {
			write("</td> <td>");
			write(cacheInformations.getConfiguration());
		}
		write("</td>");
		if (systemActionsEnabled) {
			write("<td align='center' class='noPrint'>");
			final String confirmClearCache = javascriptEncode(getFormattedString(
					"confirm_purge_cache", cacheInformations.getName()));
			// writeDirectly pour ne pas gérer de traductions si le nom contient '#'
			writeDirectly("<a href='?action=clear_cache&amp;cacheId="
					+ urlEncode(cacheInformations.getName())
					+ "' onclick=\"javascript:return confirm('" + confirmClearCache + "');\">");
			final String title = htmlEncode(getFormattedString("Purge_cache",
					cacheInformations.getName()));
			writeDirectly("<img src='?resource=user-trash.png' width='16' height='16' alt='"
					+ title + "' title='" + title + "' /></a>");
			write("</td>");
		}
	}

	static boolean isHitsRatioEnabled(List<CacheInformations> cacheInformationsList) {
		for (final CacheInformations cacheInformations : cacheInformationsList) {
			if (cacheInformations.getHitsRatio() >= 0
					|| cacheInformations.getInMemoryHitsRatio() >= 0) {
				return true;
			}
		}
		return false;
	}

	static boolean isConfigurationEnabled(List<CacheInformations> cacheInformationsList) {
		for (final CacheInformations cacheInformations : cacheInformationsList) {
			if (cacheInformations.getConfiguration() != null) {
				return true;
			}
		}
		return false;
	}
}
