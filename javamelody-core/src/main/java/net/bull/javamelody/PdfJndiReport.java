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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Partie du rapport pdf pour l'arbre JNDI.
 * @author Emeric Vernat
 */
class PdfJndiReport extends PdfAbstractReport {
	private final List<JndiBinding> jndiBindings;
	private final Font cellFont = PdfFonts.TABLE_CELL.getFont();
	private Image folderImage;
	private PdfPTable currentTable;

	PdfJndiReport(List<JndiBinding> jndiBindings, Document document) {
		super(document);
		assert jndiBindings != null;
		this.jndiBindings = jndiBindings;
	}

	@Override
	void toPdf() throws DocumentException, IOException {
		writeHeader();

		final PdfPCell defaultCell = getDefaultCell();
		boolean odd = false;
		for (final JndiBinding jndiBinding : jndiBindings) {
			if (odd) {
				defaultCell.setGrayFill(0.97f);
			} else {
				defaultCell.setGrayFill(1);
			}
			odd = !odd; // NOPMD
			writeJndiBinding(jndiBinding);
		}
		addToDocument(currentTable);
	}

	private void writeHeader() throws DocumentException {
		final List<String> headers = createHeaders();
		final int[] relativeWidths = new int[headers.size()];
		Arrays.fill(relativeWidths, 0, headers.size(), 1);

		currentTable = PdfDocumentFactory.createPdfPTable(headers, relativeWidths);
	}

	private List<String> createHeaders() {
		final List<String> headers = new ArrayList<String>();
		headers.add(getString("Nom"));
		headers.add(getString("Type"));
		headers.add(getString("Value"));
		return headers;
	}

	private void writeJndiBinding(JndiBinding jndiBinding) throws BadElementException, IOException {
		final PdfPCell defaultCell = getDefaultCell();
		defaultCell.setHorizontalAlignment(Element.ALIGN_LEFT);
		final String name = jndiBinding.getName();
		final String className = jndiBinding.getClassName();
		final String contextPath = jndiBinding.getContextPath();
		final String value = jndiBinding.getValue();
		if (contextPath != null) {
			final Image image = getFolderImage();
			final Phrase phrase = new Phrase("", cellFont);
			phrase.add(new Chunk(image, 0, 0));
			phrase.add(" ");
			phrase.add(name);
			currentTable.addCell(phrase);
		} else {
			addCell(name);
		}
		addCell(className != null ? className : "");
		addCell(value != null ? value : "");
	}

	private Image getFolderImage() throws BadElementException, IOException {
		if (folderImage == null) {
			folderImage = PdfDocumentFactory.getImage("folder.png");
			folderImage.scalePercent(40);
		}
		return folderImage;
	}

	private PdfPCell getDefaultCell() {
		return currentTable.getDefaultCell();
	}

	private void addCell(String string) {
		currentTable.addCell(new Phrase(string, cellFont));
	}
}
