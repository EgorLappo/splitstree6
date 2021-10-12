/*
 *  DataBlock.java Copyright (C) 2021 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package splitstree6.workflow;

import jloda.util.Basic;

/**
 * splitstree data block
 * Daniel Huson, 10.2021
 */
public abstract class DataBlock extends jloda.fx.workflow.DataBlock {

	public void clear() {
	}

	public abstract int size();

	public abstract String getInfo();

	public abstract DataTaxaFilter<? extends DataBlock, ? extends DataBlock> createTaxaDataFilter();

	/**
	 * creates a new instance
	 *
	 * @return new instance
	 */
	public DataBlock newInstance() {
		try {
			return getClass().getConstructor().newInstance();
		} catch (Exception e) {
			Basic.caught(e);
			return null;
		}
	}
}