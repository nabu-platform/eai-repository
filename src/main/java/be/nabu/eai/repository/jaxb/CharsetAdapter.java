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

package be.nabu.eai.repository.jaxb;

import java.nio.charset.Charset;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class CharsetAdapter extends XmlAdapter<String, Charset> {

	@Override
	public Charset unmarshal(String v) throws Exception {
		return v == null ? null : Charset.forName(v);
	}

	@Override
	public String marshal(Charset v) throws Exception {
		return v == null ? null : v.name();
	}

}
