/*
 * Created on May 6, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package com.aelitis.azureus.core.metasearch;

import java.io.IOException;
import java.util.Map;

public interface 
MetaSearch 
{	
	public Engine
	importFromBEncodedMap(
		Map		map )
	
		throws IOException;
	
	public void 
	search(
		ResultListener 		listener,
		SearchParameter[] 	searchParameters,
		String				headers );
	
	public void 
	search(
		Engine				engine,
		ResultListener 		listener,
		SearchParameter[] 	searchParameters,
		String				headers );
	
	public Engine[] 
	getEngines(
		boolean		active_only,
		boolean		ensure_up_to_date );
	
	public Engine
	getEngine(
		long		id );
	
	public void 
	addEngine( 
		Engine 		engine );
	
	public Engine 
	addEngine( 
		long		id )
	
		throws MetaSearchException;
	
	public Engine
	createRSSEngine(
		String		url )
	
		throws MetaSearchException;
	
	public void 
	removeEngine(
		Engine 		engine );
	
	public void
	addListener(
		MetaSearchListener		listener );
	
	public void
	removeListener(
		MetaSearchListener		listener );
}
