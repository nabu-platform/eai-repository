/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.repository.api;

public interface Page {
	// the current page you have retrieved
	public int getCurrent();
	// the total amount of pages available
	public int getTotal();
	// the amount of entries on each page (= limit)
	public long getPageSize();
	// the total amount of entries available
	public long getTotalRowCount();
	// the offset in rows (= offset)
	public long getRowOffset();
}
