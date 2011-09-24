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

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JLabel;

import net.bull.javamelody.swing.Utilities;
import net.bull.javamelody.swing.table.MTable;
import net.bull.javamelody.swing.table.MTableScrollPane;

/**
 * Panel de l'histogramme mémoire.
 * @author Emeric Vernat
 */
class HeapInformationsPanel extends MelodyPanel {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("all")
	private HeapHistogram heapHistogram;
	private MTable<HeapHistogram.ClassInfo> table;

	HeapInformationsPanel(RemoteCollector remoteCollector) throws IOException {
		super(remoteCollector);

		refresh();
	}

	private void refresh() throws IOException {
		removeAll();

		this.heapHistogram = getRemoteCollector().collectHeapHistogram();
		this.table = new MTable<HeapHistogram.ClassInfo>();

		setName(I18N.getFormattedString("heap_histo_du",
				I18N.createDateAndTimeFormat().format(heapHistogram.getTime())));
		final JLabel titleLabel = Utilities.createParagraphTitle(
				I18N.getFormattedString("heap_histo_du",
						I18N.createDateAndTimeFormat().format(heapHistogram.getTime())),
				"memory.png");
		add(titleLabel, BorderLayout.NORTH);

		addScrollPane();

		final JLabel label = new JLabel(' ' + I18N.getString("Temps_threads"));
		add(label, BorderLayout.SOUTH);
	}

	private void addScrollPane() {
		final MTableScrollPane<HeapHistogram.ClassInfo> tableScrollPane = new MTableScrollPane<HeapHistogram.ClassInfo>(
				table);
		//		table.addColumn("name", I18N.getString("Thread"));
		// TODO

		add(tableScrollPane, BorderLayout.CENTER);
	}
}
