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

import javax.faces.component.ActionSource2;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

/**
 * ActionListener JSF RI (Mojarra) pour avoir les temps moyens des actions JSF.
 * @author Emeric Vernat
 */
public class JsfActionListener implements ActionListener {
	private static final Counter JSF_COUNTER = MonitoringProxy.getJsfCounter();
	private static final boolean COUNTER_HIDDEN = Parameters.isCounterHidden(JSF_COUNTER.getName());
	private static final boolean DISABLED = Boolean.parseBoolean(Parameters
			.getParameter(Parameter.DISABLED));
	private final ActionListener delegateActionListener;

	/**
	 * Constructeur.
	 * @param delegateActionListener ActionListener
	 */
	public JsfActionListener(ActionListener delegateActionListener) {
		super();
		// quand cet ActionListener est utilisé, le compteur est affiché
		// sauf si le paramètre displayed-counters dit le contraire
		JSF_COUNTER.setDisplayed(!COUNTER_HIDDEN);
		JSF_COUNTER.setUsed(true);
		LOG.debug("jsf action listener initialized");
		this.delegateActionListener = delegateActionListener;
	}

	/** {@inheritDoc} */
	@Override
	public void processAction(ActionEvent event) { // throws FacesException
		// cette méthode est appelée par JSF RI (Mojarra)
		if (DISABLED || !JSF_COUNTER.isDisplayed()) {
			delegateActionListener.processAction(event);
			return;
		}

		boolean systemError = false;
		try {
			final String actionName = getRequestName(event);

			JSF_COUNTER.bindContextIncludingCpu(actionName);

			delegateActionListener.processAction(event);
		} catch (final Error e) {
			// on catche Error pour avoir les erreurs systèmes
			// mais pas Exception qui sont fonctionnelles en général
			systemError = true;
			throw e;
		} finally {
			// on enregistre la requête dans les statistiques
			JSF_COUNTER.addRequestForCurrentContext(systemError);
		}
	}

	protected String getRequestName(ActionEvent event) {
		final String actionName;
		if (event.getComponent() instanceof ActionSource2) {
			// actionSource est une UICommand en général
			final ActionSource2 actionSource = (ActionSource2) event.getComponent();
			if (actionSource.getActionExpression() != null) {
				actionName = actionSource.getActionExpression().getExpressionString();
			} else {
				actionName = actionSource.getClass().getName();
			}
		} else {
			actionName = event.getComponent().getClass().getName();
		}
		return actionName;
	}
}
