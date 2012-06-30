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

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.bull.javamelody.swing.MButton;
import net.bull.javamelody.swing.Utilities;

/**
 * Panel principal.
 * @author Emeric Vernat
 */
class ScrollingPanel extends MelodyPanel {
	static final ImageIcon PLUS_ICON = ImageIconCache.getImageIcon("bullets/plus.png");
	static final ImageIcon MINUS_ICON = ImageIconCache.getImageIcon("bullets/minus.png");
	private static final ImageIcon PURGE_OBSOLETE_FILES_ICON = ImageIconCache.getScaledImageIcon(
			"user-trash.png", 12, 12);
	private static final String DETAILS_KEY = "Details";

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("all")
	private final Collector collector;
	@SuppressWarnings("all")
	private final List<JavaInformations> javaInformationsList;
	private final Range range;
	private final URL monitoringUrl;
	private final long start = System.currentTimeMillis();

	ScrollingPanel(RemoteCollector remoteCollector, Range range, URL monitoringUrl) {
		super(remoteCollector);
		this.collector = remoteCollector.getCollector();
		this.javaInformationsList = remoteCollector.getJavaInformationsList();
		this.range = range;
		this.monitoringUrl = monitoringUrl;

		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(true);
		setBackground(Color.decode("#E6E6E6"));
		add(new ChartsPanel(remoteCollector));

		addCounters();

		addSystemInformations();

		addThreadInformations();

		if (isJobEnabled()) {
			addParagraphTitle(I18N.getString("Jobs"), "jobs.png");
			// on ne peut utiliser collector.getRangeCounter(range, Counter.JOB_COUNTER_NAME),
			// en revanche collector.getCounterByName(Counter.JOB_COUNTER_NAME) contient ici les bonnes données
			final Counter rangeJobCounter = collector.getCounterByName(Counter.JOB_COUNTER_NAME);
			addJobs(rangeJobCounter);
			addCounter(rangeJobCounter);
		}

		if (isCacheEnabled()) {
			addParagraphTitle("Caches", "caches.png");
			addCaches();
		}

		add(new JLabel(" "));
		addDurationAndOverhead();

		for (final Component component : getComponents()) {
			((JComponent) component).setAlignmentX(Component.LEFT_ALIGNMENT);
		}
	}

	private void addCounters() {
		final List<Counter> counters = getCountersToBeDisplayed();
		for (final Counter counter : counters) {
			addCounter(counter);
		}
		if (range.getPeriod() == Period.TOUT && counters.size() > 1) {
			final MButton clearAllCountersButton = new MButton(
					I18N.getString("Reinitialiser_toutes_stats"));
			clearAllCountersButton.setToolTipText(I18N.getString("Vider_toutes_stats"));
			clearAllCountersButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (confirm(I18N.getString("confirm_vider_toutes_stats"))) {
						actionClearAllCounters();
					}
				}
			});
			final JPanel clearAllCountersPanel = Utilities
					.createButtonsPanel(clearAllCountersButton);
			add(clearAllCountersPanel);
		}
	}

	private List<Counter> getCountersToBeDisplayed() {
		// on ne peut utiliser collector.getRangeCountersToBeDisplayed(range),
		// en revanche collector.getCounters() contient ici les bonnes données
		final List<Counter> result = new ArrayList<>(collector.getCounters());
		final Iterator<Counter> it = result.iterator();
		while (it.hasNext()) {
			final Counter counter = it.next();
			if (!counter.isDisplayed() || counter.isJobCounter()) {
				it.remove();
			}
		}
		return Collections.unmodifiableList(result);
	}

	final void actionClearAllCounters() {
		try {
			final String message = getRemoteCollector().executeActionAndCollectData(
					Action.CLEAR_COUNTER, "all", null, null, null);
			showMessage(message);
			MainPanel.refreshMainTabFromChild(this);
		} catch (final IOException ex) {
			showException(ex);
		}
	}

	private void addCounter(Counter counter) {
		final String counterLabel = I18N.getString(counter.getName() + "Label");
		addParagraphTitle(I18N.getFormattedString("Statistiques_compteur", counterLabel) + " - "
				+ range.getLabel(), counter.getIconName());
		// pas de graphique dans les statistiques globales
		final boolean includeGraph = false;
		final StatisticsPanel statisticsPanel = new StatisticsPanel(getRemoteCollector(), counter,
				range, includeGraph);
		statisticsPanel.showGlobalRequests();
		add(statisticsPanel);
	}

	private void addSystemInformations() {
		addParagraphTitle(I18N.getString("Informations_systemes"), "systeminfo.png");
		final List<JavaInformations> list = javaInformationsList;
		// TODO mettre propriété système system-actions-enabled dans jnlp
		if (Parameters.isSystemActionsEnabled()) {
			add(new SystemInformationsButtonsPanel(getRemoteCollector(), monitoringUrl));
		}

		final List<JavaInformationsPanel> javaInformationsPanelList = new ArrayList<>(list.size());
		final JPanel westJavaInformationsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		westJavaInformationsPanel.setOpaque(false);
		for (final JavaInformations javaInformations : list) {
			final JavaInformationsPanel javaInformationsPanel = new JavaInformationsPanel(
					getRemoteCollector(), javaInformations, monitoringUrl);
			javaInformationsPanel.showSummary();
			javaInformationsPanelList.add(javaInformationsPanel);
			westJavaInformationsPanel.add(javaInformationsPanel);
		}
		final MButton javaInformationsDetailsButton = new MButton(I18N.getString(DETAILS_KEY),
				PLUS_ICON);
		javaInformationsDetailsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final boolean repeatHost = list.size() > 1;
				for (final JavaInformationsPanel javaInformationsPanel : javaInformationsPanelList) {
					javaInformationsPanel.showDetails(repeatHost);
					javaInformationsPanel.validate();
				}
				changePlusMinusIcon(javaInformationsDetailsButton);
			}
		});
		westJavaInformationsPanel.add(javaInformationsDetailsButton);
		add(westJavaInformationsPanel);
		add(new JLabel(" "));
	}

	private void addThreadInformations() {
		addParagraphTitle(I18N.getString("Threads"), "threads.png");
		for (final JavaInformations javaInformations : javaInformationsList) {
			final List<ThreadInformations> threadInformationsList = javaInformations
					.getThreadInformationsList();
			final ThreadInformationsPanel threadInformationsPanel = new ThreadInformationsPanel(
					getRemoteCollector(), threadInformationsList,
					javaInformations.isStackTraceEnabled());
			threadInformationsPanel.setVisible(false);
			final JLabel summaryLabel = new JLabel("<html><b>"
					+ I18N.getFormattedString("Threads_sur", javaInformations.getHost())
					+ ": </b>"
					+ I18N.getFormattedString("thread_count", javaInformations.getThreadCount(),
							javaInformations.getPeakThreadCount(),
							javaInformations.getTotalStartedThreadCount()));
			final MButton detailsButton = new MButton(I18N.getString(DETAILS_KEY), PLUS_ICON);
			detailsButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					threadInformationsPanel.setVisible(!threadInformationsPanel.isVisible());
					validate();
					changePlusMinusIcon(detailsButton);
				}
			});

			final JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			flowPanel.setOpaque(false);
			flowPanel.add(summaryLabel);
			flowPanel.add(detailsButton);

			add(flowPanel);
			addThreadDeadlocks(threadInformationsList);
			add(threadInformationsPanel);
		}
	}

	private void addThreadDeadlocks(List<ThreadInformations> threadInformationsList) {
		final List<ThreadInformations> deadlockedThreads = new ArrayList<>();
		for (final ThreadInformations thread : threadInformationsList) {
			if (thread.isDeadlocked()) {
				deadlockedThreads.add(thread);
			}
		}
		if (!deadlockedThreads.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			sb.append("  ");
			sb.append(I18N.getString("Threads_deadlocks"));
			String separator = " ";
			for (final ThreadInformations thread : deadlockedThreads) {
				sb.append(separator);
				sb.append(thread.getName());
				separator = ", ";
			}
			final JLabel label = new JLabel(sb.toString());
			label.setForeground(Color.RED);
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			// séparateur avec composants au-dessus
			label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			add(label);
		}
	}

	private void addCaches() {
		for (final JavaInformations javaInformations : javaInformationsList) {
			if (!javaInformations.isCacheEnabled()) {
				continue;
			}
			final List<CacheInformations> cacheInformationsList = javaInformations
					.getCacheInformationsList();
			final CacheInformationsPanel cacheInformationsPanel = new CacheInformationsPanel(
					getRemoteCollector(), cacheInformationsList);
			cacheInformationsPanel.setVisible(false);
			final JLabel summaryLabel = new JLabel("<html><b>"
					+ I18N.getFormattedString("caches_sur", cacheInformationsList.size(),
							javaInformations.getHost()) + "</b>");
			final MButton detailsButton = new MButton(I18N.getString(DETAILS_KEY), PLUS_ICON);
			detailsButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					cacheInformationsPanel.setVisible(!cacheInformationsPanel.isVisible());
					validate();
					changePlusMinusIcon(detailsButton);
				}
			});

			final JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			flowPanel.setOpaque(false);
			flowPanel.add(summaryLabel);
			flowPanel.add(detailsButton);

			add(flowPanel);
			add(cacheInformationsPanel);
		}
	}

	private void addJobs(Counter rangeJobCounter) {
		for (final JavaInformations javaInformations : javaInformationsList) {
			if (!javaInformations.isJobEnabled()) {
				continue;
			}
			final List<JobInformations> jobInformationsList = javaInformations
					.getJobInformationsList();
			final JobInformationsPanel jobInformationsPanel = new JobInformationsPanel(
					getRemoteCollector(), jobInformationsList, rangeJobCounter);
			jobInformationsPanel.setVisible(false);
			final JLabel summaryLabel = new JLabel("<html><b>"
					+ I18N.getFormattedString("jobs_sur", jobInformationsList.size(),
							javaInformations.getHost(),
							javaInformations.getCurrentlyExecutingJobCount()) + "</b>");
			final MButton detailsButton = new MButton(I18N.getString(DETAILS_KEY), PLUS_ICON);
			detailsButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					jobInformationsPanel.setVisible(!jobInformationsPanel.isVisible());
					validate();
					changePlusMinusIcon(detailsButton);
				}
			});

			final JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			flowPanel.setOpaque(false);
			flowPanel.add(summaryLabel);
			flowPanel.add(detailsButton);

			add(flowPanel);
			add(jobInformationsPanel);
		}
	}

	private void addDurationAndOverhead() {
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		final long displayDuration = System.currentTimeMillis() - start;
		final JLabel lastCollectDurationLabel = new JLabel(
				I18N.getString("temps_derniere_collecte") + ": "
						+ collector.getLastCollectDuration() + ' ' + I18N.getString("ms"));
		final JLabel displayDurationLabel = new JLabel(I18N.getString("temps_affichage") + ": "
				+ displayDuration + ' ' + I18N.getString("ms"));
		final JLabel overheadLabel = new JLabel(I18N.getString("Estimation_overhead_memoire")
				+ ": < " + (collector.getEstimatedMemorySize() / 1024 / 1024 + 1) + ' '
				+ I18N.getString("Mo"));
		final Font font = overheadLabel.getFont().deriveFont(10f);
		lastCollectDurationLabel.setFont(font);
		displayDurationLabel.setFont(font);
		overheadLabel.setFont(font);
		final MButton purgeObsoleteFilesButton = new MButton();
		purgeObsoleteFilesButton.setIcon(PURGE_OBSOLETE_FILES_ICON);
		purgeObsoleteFilesButton.setToolTipText(I18N.getString("Purger_les_fichiers_obsoletes"));
		purgeObsoleteFilesButton.setBorder(null);
		purgeObsoleteFilesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				purgeObsoleteFiles();
			}
		});
		panel.add(lastCollectDurationLabel);
		panel.add(displayDurationLabel);
		panel.add(overheadLabel);
		panel.add(purgeObsoleteFilesButton);
		if (Parameters.JAVAMELODY_VERSION != null) {
			panel.add(new JLabel(" "));
			final JLabel versionLabel = new JLabel("JavaMelody " + Parameters.JAVAMELODY_VERSION);
			versionLabel.setFont(font);
			panel.add(versionLabel);
		}
		panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		panel.setOpaque(false);
		add(panel);
	}

	final void purgeObsoleteFiles() {
		try {
			final String message = getRemoteCollector().executeActionAndCollectData(
					Action.PURGE_OBSOLETE_FILES, null, null, null, null);
			showMessage(message);
			MainPanel.refreshMainTabFromChild(this);
		} catch (final IOException ex) {
			showException(ex);
		}
	}

	final void changePlusMinusIcon(MButton detailsButton) {
		if (detailsButton.getIcon() == PLUS_ICON) {
			detailsButton.setIcon(MINUS_ICON);
		} else {
			detailsButton.setIcon(PLUS_ICON);
		}
	}

	private boolean isCacheEnabled() {
		for (final JavaInformations javaInformations : javaInformationsList) {
			if (javaInformations.isCacheEnabled()) {
				return true;
			}
		}
		return false;
	}

	private boolean isJobEnabled() {
		for (final JavaInformations javaInformations : javaInformationsList) {
			if (javaInformations.isJobEnabled()) {
				return true;
			}
		}
		return false;
	}

	private void addParagraphTitle(String title, String iconName) {
		final JLabel label = Utilities.createParagraphTitle(title, iconName);
		add(label);
	}
}
