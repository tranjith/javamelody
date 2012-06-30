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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import net.bull.javamelody.swing.Utilities;
import net.bull.javamelody.swing.table.MDefaultTableCellRenderer;
import net.bull.javamelody.swing.table.MDoubleTableCellRenderer;
import net.bull.javamelody.swing.table.MIntegerTableCellRenderer;
import net.bull.javamelody.swing.table.MTable;
import net.bull.javamelody.swing.table.MTableScrollPane;

/**
 * Panel du tableau de requêtes dans le détail d'une requête.
 * @author Emeric Vernat
 */
class CounterRequestDetailTablePanel extends CounterRequestAbstractPanel {
	private static final long serialVersionUID = 1L;

	private final CounterRequest request;

	@SuppressWarnings("all")
	private final Map<String, Long> childRequestsExecutions;

	private final class ChildValueTableCellRenderer extends MIntegerTableCellRenderer {
		private static final long serialVersionUID = 1L;

		ChildValueTableCellRenderer() {
			super();
		}

		@Override
		public Component getTableCellRendererComponent(JTable jtable, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			final MTable<CounterRequest> myTable = getTable();
			final CounterRequest counterRequest = myTable.getList().get(
					myTable.convertRowIndexToModel(row));
			final Integer myValue;
			if (counterRequest.hasChildHits()) {
				myValue = (Integer) value;
			} else {
				myValue = null;
			}
			return super.getTableCellRendererComponent(jtable, myValue, isSelected, hasFocus, row,
					column);
		}
	}

	private final class NameTableCellRenderer extends MDefaultTableCellRenderer {
		private static final int CHILD_MARGIN = 10;

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
				final Icon icon;
				final ImageIcon counterIcon = getIcon(counter.getIconName());
				if (counterRequest.equals(getRequest())) {
					icon = counterIcon;
				} else {
					// ajoute une marge à gauche de l'icône
					icon = new Icon() {
						@Override
						public void paintIcon(Component c, Graphics g, int x, int y) {
							g.drawImage(counterIcon.getImage(), CHILD_MARGIN + x, y, null);
						}

						@Override
						public int getIconWidth() {
							return counterIcon.getIconWidth() + CHILD_MARGIN;
						}

						@Override
						public int getIconHeight() {
							return counterIcon.getIconHeight();
						}
					};
				}
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

	private final class NbExecutionsTableCellRenderer extends MDoubleTableCellRenderer {
		private static final long serialVersionUID = 1L;

		NbExecutionsTableCellRenderer() {
			super();
		}

		@Override
		public Component getTableCellRendererComponent(JTable jtable, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			final MTable<CounterRequest> myTable = getTable();
			final CounterRequest counterRequest = myTable.getList().get(
					myTable.convertRowIndexToModel(row));
			final CounterRequest parentRequest = getRequest();

			final Float executionsByRequest;
			if (counterRequest.equals(parentRequest)) {
				executionsByRequest = null;
			} else {
				final Long nbExecutions = getChildRequestExecutions(counterRequest);
				executionsByRequest = (float) nbExecutions / parentRequest.getHits();
			}

			return super.getTableCellRendererComponent(jtable, executionsByRequest, isSelected,
					hasFocus, row, column);
		}
	}

	CounterRequestDetailTablePanel(RemoteCollector remoteCollector, CounterRequest request,
			Range range) throws IOException {
		super(remoteCollector, range);
		this.request = request;
		this.childRequestsExecutions = request.getChildRequestsExecutionsByRequestId();

		setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

		final MTableScrollPane<CounterRequest> scrollPane = createScrollPane();
		final List<CounterRequest> requests = new ArrayList<>();
		requests.add(request);

		if (!childRequestsExecutions.isEmpty()) {
			final Map<String, CounterRequest> requestsById = mapAllRequestsById();
			for (final Map.Entry<String, Long> entry : childRequestsExecutions.entrySet()) {
				final CounterRequest childRequest = requestsById.get(entry.getKey());
				if (childRequest != null) {
					requests.add(childRequest);
				}
			}
		}
		getTable().setList(requests);
		Utilities.adjustTableHeight(getTable());
		add(scrollPane, BorderLayout.CENTER);

		add(createButtonsPanel(), BorderLayout.SOUTH);
	}

	private MTableScrollPane<CounterRequest> createScrollPane() {
		final MTable<CounterRequest> table = getTable();
		final MTableScrollPane<CounterRequest> tableScrollPane = new MTableScrollPane<>(table);

		table.addColumn("name", I18N.getString("Requete"));
		table.setColumnCellRenderer("name", new NameTableCellRenderer());
		if (!childRequestsExecutions.isEmpty()) {
			final TableColumn nbExecutionsColumn = new TableColumn(table.getColumnCount());
			nbExecutionsColumn.setIdentifier(table.getColumnCount());
			nbExecutionsColumn.setHeaderValue(I18N.getString("Hits_par_requete"));
			table.addColumn(nbExecutionsColumn);

			nbExecutionsColumn.setCellRenderer(new NbExecutionsTableCellRenderer());
		}
		table.addColumn("mean", I18N.getString("Temps_moyen"));
		table.addColumn("maximum", I18N.getString("Temps_max"));
		table.addColumn("standardDeviation", I18N.getString("Ecart_type"));
		table.addColumn("cpuTimeMean", I18N.getString("Temps_cpu_moyen"));
		table.addColumn("systemErrorPercentage", I18N.getString("erreur_systeme"));
		table.setColumnCellRenderer("cpuTimeMean", new MIntegerTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public void setValue(Object value) {
				final Integer cpuTimeMean = (Integer) value;
				if (cpuTimeMean >= 0) {
					super.setValue(cpuTimeMean);
				} else {
					super.setValue(null);
				}
			}
		});

		final Counter parentCounter = getCounterByRequestId(request);
		final boolean allChildHitsDisplayed = parentCounter != null
				&& parentCounter.getChildCounterName() != null;
		if (allChildHitsDisplayed) {
			final String childCounterName = parentCounter.getChildCounterName();
			table.addColumn("childHitsMean",
					I18N.getFormattedString("hits_fils_moyens", childCounterName));
			table.addColumn("childDurationsMean",
					I18N.getFormattedString("temps_fils_moyen", childCounterName));
			final ChildValueTableCellRenderer childValueTableCellRenderer = new ChildValueTableCellRenderer();
			table.setColumnCellRenderer("childHitsMean", childValueTableCellRenderer);
			table.setColumnCellRenderer("childDurationsMean", childValueTableCellRenderer);
		}

		return tableScrollPane;
	}

	private Map<String, CounterRequest> mapAllRequestsById() {
		final Map<String, CounterRequest> result = new HashMap<>();
		for (final Counter counter : getCounters()) {
			for (final CounterRequest counterRequest : counter.getRequests()) {
				result.put(counterRequest.getId(), counterRequest);
			}
		}
		return result;
	}

	final CounterRequest getRequest() {
		return request;
	}

	final Long getChildRequestExecutions(CounterRequest counterRequest) {
		return childRequestsExecutions.get(counterRequest.getId());
	}
}
