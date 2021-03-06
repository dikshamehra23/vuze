/*
 * File    : TypeItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.tracker;

import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.tracker.TrackerPeerSource;


public class 
IntervalItem
	extends CoreTableColumn 
    implements TableCellRefreshListener
{
	public 
	IntervalItem()
	{
		super( "interval", ALIGN_CENTER, POSITION_LAST, 75, TableManager.TABLE_TORRENT_TRACKERS );
    
		setRefreshInterval( INTERVAL_GRAPHIC );
	}

	public void 
	fillTableColumnInfo(
		TableColumnInfo info ) 
	{
		info.addCategories( new String[]{
			CAT_ESSENTIAL,
		});
	}

	public void 
	refresh(
		TableCell cell ) 
	{
		TrackerPeerSource ps = (TrackerPeerSource)cell.getDataSource();
    
		long	interval 		= 0;
		long	min_interval 	= 0;

		if ( ps != null ){
			
			interval		= ps.getInterval();
			min_interval	= ps.getMinInterval();
		}
		
		long	sort = ( interval<<31 ) | min_interval;
		
		if (!cell.setSortValue(sort) && cell.isValid()){
		
			return;
		}

		String	str;
		
		if ( interval <= 0 && min_interval <= 0 ){
			
			str = "";
			
		}else if ( interval <= 0 ){
			
			str = "(" + min_interval + ")";
			
		}else if ( min_interval <= 0 ){
		
			str = String.valueOf( interval );
			
		}else{
		
			str = interval + " (" + min_interval + ")";
		}
		
		cell.setText( str );
	}
}
