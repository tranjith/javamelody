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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

/**
 * Test unitaire de la classe SamplingProfiler.
 * @author Emeric Vernat
 */
public class TestSamplingProfiler {
	private static final int NB_ROWS = 100;

	/**
	 * Test.
	 */
	@Test
	public void test1() {
		final SamplingProfiler samplingProfiler = new SamplingProfiler();
		assertTrue(samplingProfiler.getHotspots(NB_ROWS).isEmpty());
		samplingProfiler.update();
	}

	/**
	 * Test.
	 */
	@Test
	public void test2() {
		final SamplingProfiler samplingProfiler = new SamplingProfiler(new ArrayList<String>());
		assertTrue(samplingProfiler.getHotspots(NB_ROWS).isEmpty());
		samplingProfiler.update();
		samplingProfiler.clear();
		assertTrue(samplingProfiler.getHotspots(NB_ROWS).isEmpty());
	}
}
