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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Partie du rapport pdf pour les erreurs http et dans les logs.
 * @author Emeric Vernat
 */
class PdfCounterErrorReport extends PdfAbstractReport {
	private final Counter counter;
	private final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
			DateFormat.MEDIUM, I18N.getCurrentLocale());
	private final Font cellFont = PdfFonts.TABLE_CELL.getFont();
	private final Font severeFont = PdfFonts.SEVERE_CELL.getFont();
	private final Font normalFont = PdfFonts.NORMAL.getFont();
	private PdfPTable currentTable;

	PdfCounterErrorReport(Counter counter, Document document) {
		super(document);
		assert counter != null;
		assert counter.isErrorCounter();
		this.counter = counter;
	}

	@Override
	void toPdf() throws DocumentException {
		final List<CounterError> errors = counter.getErrors();
		if (errors.isEmpty()) {
			addToDocument(new Phrase(getString("Aucune_erreur"), normalFont));
		} else {
			writeErrors(errors);
		}
	}

	private void writeErrors(List<CounterError> errors) throws DocumentException {
		assert errors != null;
		final boolean displayUser = HtmlCounterErrorReport.shouldDisplayUser(errors);
		final boolean displayHttpRequest = HtmlCounterErrorReport.shouldDisplayHttpRequest(errors);
		if (errors.size() >= Counter.MAX_ERRORS_COUNT) {
			addToDocument(new Phrase(getFormattedString("Dernieres_erreurs_seulement",
					Counter.MAX_ERRORS_COUNT) + '\n', severeFont));
		}
		writeHeader(displayUser, displayHttpRequest);

		final PdfPCell defaultCell = getDefaultCell();
		boolean odd = false;
		for (final CounterError error : errors) {
			if (odd) {
				defaultCell.setGrayFill(0.97f);
			} else {
				defaultCell.setGrayFill(1);
			}
			odd = !odd; // NOPMD
			writeError(error, displayUser, displayHttpRequest);
		}
		addToDocument(currentTable);
	}

	private void writeHeader(boolean displayUser, boolean displayHttpRequest)
			throws DocumentException {
		final List<String> headers = createHeaders(displayUser, displayHttpRequest);
		final int[] relativeWidths = new int[headers.size()];
		Arrays.fill(relativeWidths, 0, headers.size(), 1);
		if (displayHttpRequest) {
			relativeWidths[1] = 4; // requête http
		}
		relativeWidths[headers.size() - 1] = 4; // message d'erreur

		currentTable = PdfDocumentFactory.createPdfPTable(headers, relativeWidths);
	}

	private List<String> createHeaders(boolean displayUser, boolean displayHttpRequest) {
		final List<String> headers = new ArrayList<String>();
		headers.add(getString("Date"));
		if (displayHttpRequest) {
			headers.add(getString("Requete"));
		}
		if (displayUser) {
			headers.add(getString("Utilisateur"));
		}
		headers.add(getString("Erreur"));
		return headers;
	}

	private void writeError(CounterError error, boolean displayUser, boolean displayHttpRequest) {
		getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
		addCell(dateTimeFormat.format(error.getDate()));
		getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
		if (displayHttpRequest) {
			if (error.getHttpRequest() == null) {
				addCell("");
			} else {
				addCell(error.getHttpRequest());
			}
		}
		if (displayUser) {
			if (error.getRemoteUser() == null) {
				addCell("");
			} else {
				addCell(error.getRemoteUser());
			}
		}
		addCell(error.getMessage());
	}

	private PdfPCell getDefaultCell() {
		return currentTable.getDefaultCell();
	}

	private void addCell(String string) {
		currentTable.addCell(new Phrase(string, cellFont));
	}
}
