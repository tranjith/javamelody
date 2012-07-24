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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import net.bull.javamelody.MBeanNode.MBeanAttribute;
import net.bull.javamelody.swing.Utilities;
import net.bull.javamelody.swing.table.MDefaultTableCellRenderer;
import net.bull.javamelody.swing.table.MMultiLineTableCellRenderer;
import net.bull.javamelody.swing.table.MTable;

/**
 * Panel des MBeanNode.
 * @author Emeric Vernat
 */
class MBeanNodePanel extends JPanel {
	static final Border LEFT_MARGIN_BORDER = BorderFactory.createEmptyBorder(0, 30, 0, 0);

	private static final long serialVersionUID = 1L;

	private static final Color FOREGROUND = Color.BLUE.darker();

	private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

	private static final ImageIcon PLUS_ICON = ImageIconCache.getImageIcon("bullets/plus.png");

	private static final ImageIcon MINUS_ICON = ImageIconCache.getImageIcon("bullets/minus.png");

	private static final MMultiLineTableCellRenderer FORMATTED_VALUE_CELL_RENDERER = new MMultiLineTableCellRenderer();

	private static final MDefaultTableCellRenderer DESCRIPTION_CELL_RENDERER = new MDefaultTableCellRenderer() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void setValue(Object value) {
			if (value != null) {
				super.setValue('(' + value.toString() + ')');
			} else {
				super.setValue(null);
			}
		}
	};

	private static final MouseListener LABEL_MOUSE_LISTENER = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent event) {
			final MBeanNodePanel nodePanel = (MBeanNodePanel) event.getComponent().getParent();
			nodePanel.onClick();
		}
	};

	private static final MouseListener TABLE_MOUSE_LISTENER = new MouseAdapter() {
		@SuppressWarnings("unchecked")
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				final MTable<MBeanAttribute> table = (MTable<MBeanAttribute>) e.getComponent();
				final MBeanAttribute attribute = table.getSelectedObject();
				Utilities.showTextInPopup(table, attribute.getName(),
						attribute.getFormattedValue());
			}
		}
	};

	private final MBeanNode node;

	private JLabel label;

	private JPanel detailPanel;

	MBeanNodePanel(MBeanNode node) {
		super(new BorderLayout());
		assert node != null;
		this.node = node;
		init();
	}

	private void init() {
		setOpaque(false);
		String name = node.getName();
		final int indexOfComma = name.indexOf(',');
		if (node.getChildren() != null || indexOfComma != -1) {
			if (indexOfComma != -1) {
				name = name.substring(indexOfComma + 1);
			}
			label = new JLabel(name);
			label.setIcon(PLUS_ICON);
			label.setForeground(FOREGROUND);
			label.setCursor(HAND_CURSOR);
			label.addMouseListener(LABEL_MOUSE_LISTENER);
			add(label, BorderLayout.CENTER);
		} else {
			detailPanel = createAttributesPanel();
			add(detailPanel, BorderLayout.CENTER);
		}
	}

	void onClick() {
		if (detailPanel == null) {
			final List<MBeanNode> children = node.getChildren();
			if (children != null) {
				detailPanel = createNodeTreePanel(children);
			} else {
				detailPanel = createAttributesPanel();
			}
			detailPanel.setBorder(LEFT_MARGIN_BORDER);
			detailPanel.setVisible(false);
			add(detailPanel, BorderLayout.SOUTH);
		}
		detailPanel.setVisible(!detailPanel.isVisible());
		if (label.getIcon() == PLUS_ICON) {
			label.setIcon(MINUS_ICON);
		} else {
			label.setIcon(PLUS_ICON);
		}
		validate();
	}

	private JPanel createAttributesPanel() {
		final List<MBeanAttribute> attributes = node.getAttributes();
		boolean descriptionDisplayed = false;
		for (final MBeanAttribute attribute : attributes) {
			if (attribute.getDescription() != null) {
				descriptionDisplayed = true;
				break;
			}
		}
		final JPanel attributesPanel = new JPanel(new BorderLayout());
		attributesPanel.setOpaque(false);
		if (node.getDescription() != null) {
			final JLabel descriptionLabel = new JLabel('(' + node.getDescription() + ')');
			attributesPanel.add(descriptionLabel, BorderLayout.NORTH);
		}

		final MTable<MBeanAttribute> table = new MTable<>();
		table.addColumn("name", I18N.getString("Nom"));
		table.addColumn("formattedValue", I18N.getString("Contenu"));
		table.setColumnCellRenderer("formattedValue", FORMATTED_VALUE_CELL_RENDERER);
		if (descriptionDisplayed) {
			table.addColumn("description", "");
			table.setColumnCellRenderer("description", DESCRIPTION_CELL_RENDERER);
		}
		table.setList(attributes);
		table.addMouseListener(TABLE_MOUSE_LISTENER);
		attributesPanel.add(table, BorderLayout.CENTER);
		return attributesPanel;
	}

	static JPanel createNodeTreePanel(List<MBeanNode> nodes) {
		final JPanel nodeTreePanel = new JPanel();
		nodeTreePanel.setOpaque(false);
		nodeTreePanel.setLayout(new BoxLayout(nodeTreePanel, BoxLayout.Y_AXIS));

		for (final MBeanNode node : nodes) {
			nodeTreePanel.add(new MBeanNodePanel(node));
		}
		return nodeTreePanel;
	}
}
