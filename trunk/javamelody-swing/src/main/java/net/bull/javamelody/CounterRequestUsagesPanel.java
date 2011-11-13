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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.bull.javamelody.swing.MButton;
import net.bull.javamelody.swing.Utilities;
import net.bull.javamelody.swing.table.MDefaultTableCellRenderer;
import net.bull.javamelody.swing.table.MTable;
import net.bull.javamelody.swing.table.MTableScrollPane;

import com.lowagie.text.Font;

/**
 * Panel des utilisations d'une requêtes.
 * @author Emeric Vernat
 */
class CounterRequestUsagesPanel extends MelodyPanel {
	private static final long serialVersionUID = 1L;

	private final Range range;

	@SuppressWarnings("all")
	private final List<Counter> counters;

	private final MTable<CounterRequest> table;

	private final class NameTableCellRenderer extends MDefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("all")
		private final Map<String, ImageIcon> iconByName = new HashMap<String, ImageIcon>();

		NameTableCellRenderer() {
			super();
		}

		@Override
		public Component getTableCellRendererComponent(JTable jtable, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			final MTable<CounterRequest> myTable = getTable();
			final CounterRequest counterRequest = myTable.getList().get(
					myTable.convertRowIndexToModel(row));
			final Counter counter = getCounterByRequestId(counterRequest);
			if (counter != null && counter.getIconName() != null) {
				final Icon icon = getIcon(counter.getIconName());
				setIcon(icon);
			} else {
				setIcon(null);
			}
			return super.getTableCellRendererComponent(jtable, value, isSelected, hasFocus, row,
					column);
		}

		private ImageIcon getIcon(String iconName) {
			ImageIcon icon = iconByName.get(iconName);
			if (icon == null) {
				icon = ImageIconCache.getScaledImageIcon(iconName, 16, 16);
				iconByName.put(iconName, icon);
			}
			return icon;
		}
	}

	CounterRequestUsagesPanel(RemoteCollector remoteCollector, CounterRequest request, Range range)
			throws IOException {
		super(remoteCollector);
		this.range = range;
		this.counters = remoteCollector.getCollector().getRangeCountersToBeDisplayed(range);

		final String graphLabel = truncate(
				I18N.getString("Utilisations_de") + ' ' + request.getName(), 50);
		setName(graphLabel);

		final JLabel label = new JLabel(' ' + I18N.getString("Utilisations_de") + ' '
				+ request.getName());
		label.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		add(label, BorderLayout.NORTH);

		this.table = new CounterRequestTable(remoteCollector);
		final MTableScrollPane<CounterRequest> scrollPane = createScrollPane();
		final List<CounterRequest> requests = new ArrayList<CounterRequest>();
		for (final Counter counter : counters) {
			for (final CounterRequest counterRequest : counter.getOrderedRequests()) {
				if (counterRequest.containsChildRequest(request.getId())) {
					requests.add(counterRequest);
				}
			}
		}

		table.setList(requests);
		add(scrollPane, BorderLayout.CENTER);

		add(createButtonsPanel(), BorderLayout.SOUTH);
	}

	private JPanel createButtonsPanel() {
		// TODO traduction
		final MButton openButton = new MButton("Ouvrir",
				ImageIconCache.getImageIcon("action_open.png"));
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final CounterRequest counterRequest = getTable().getSelectedObject();
				try {
					showRequestDetail(counterRequest);
				} catch (final IOException ex) {
					showException(ex);
				}
			}
		});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					openButton.doClick();
				}
			}
		});
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				final CounterRequest counterRequest = getTable().getSelectedObject();
				openButton.setEnabled(counterRequest != null);
			}
		});
		openButton.setEnabled(false);

		final MButton usagesButton = new MButton(I18N.getString("Chercher_utilisations"),
				ImageIconCache.getImageIcon("find.png"));
		usagesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final CounterRequest counterRequest = getTable().getSelectedObject();
				try {
					showRequestUsages(counterRequest);
				} catch (final IOException ex) {
					showException(ex);
				}
			}
		});
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				final CounterRequest counterRequest = getTable().getSelectedObject();
				usagesButton.setEnabled(counterRequest != null
						&& doesRequestDisplayUsages(counterRequest));
			}
		});
		usagesButton.setEnabled(false);
		return Utilities.createButtonsPanel(openButton, usagesButton);
	}

	private MTableScrollPane<CounterRequest> createScrollPane() {
		final MTableScrollPane<CounterRequest> tableScrollPane = new MTableScrollPane<CounterRequest>(
				table);

		table.addColumn("name", I18N.getString("Requete"));
		table.setColumnCellRenderer("name", new NameTableCellRenderer());

		return tableScrollPane;
	}

	final Counter getCounterByRequestId(CounterRequest counterRequest) {
		return getRemoteCollector().getCollector().getCounterByRequestId(counterRequest);
	}

	final boolean doesRequestDisplayUsages(CounterRequest counterRequest) {
		final Counter parentCounter = getCounterByRequestId(counterRequest);
		return parentCounter != null && !parentCounter.isErrorCounter()
				&& !Counter.HTTP_COUNTER_NAME.equals(parentCounter.getName());
	}

	final MTable<CounterRequest> getTable() {
		return table;
	}

	final void showRequestDetail(CounterRequest counterRequest) throws IOException {
		final CounterRequestDetailPanel panel = new CounterRequestDetailPanel(getRemoteCollector(),
				counterRequest, range);
		MainPanel.addOngletFromChild(this, panel);
	}

	final void showRequestUsages(CounterRequest counterRequest) throws IOException {
		final CounterRequestUsagesPanel panel = new CounterRequestUsagesPanel(getRemoteCollector(),
				counterRequest, range);
		MainPanel.addOngletFromChild(this, panel);
	}

	private static String truncate(String string, int maxLength) {
		return string.substring(0, Math.min(string.length(), maxLength));
	}
}
