/*
 * File    : TRTrackerServerTorrent.java
 * Created : 26-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerServerTorrentImpl 
	implements TRTrackerServerTorrent
{
	public static final long	SCRAPE_CACHE_PERIOD	= 5000;
	
	protected TRTrackerServerImpl	server;
	protected HashWrapper			hash;

	protected Map				peer_map 		= new HashMap();
	protected Map				peer_reuse_map	= new HashMap();
	protected List				peer_list		= new ArrayList();
	
	protected Random			random		= new Random( System.currentTimeMillis());
	
	protected long				last_scrape_calc_time;
	protected Map				last_scrape;
	
	protected TRTrackerServerTorrentStatsImpl	stats;
		
	protected
	TRTrackerServerTorrentImpl(
		TRTrackerServerImpl		_server,
		HashWrapper				_hash )
	{
		server		= _server;
		hash		= _hash;
		
		stats		= new TRTrackerServerTorrentStatsImpl( this );
	}
	
	
	public synchronized void
	peerContact(
		String		event,
		String		peer_id,
		int			port,
		String		ip_address,
		long		uploaded,
		long		downloaded,
		long		left,
		int			numwant,
		long		interval_requested )
	{
		boolean	stopped 	= event != null && event.equalsIgnoreCase("stopped");
		boolean	completed 	= event != null && event.equalsIgnoreCase("completed");
		
		TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)peer_map.get( peer_id );

		String	reuse_key = ip_address + ":" +port;
		
		if ( peer == null ){
			
			// check to see if this peer already has an entry against this torrent
			// and if so delete it (assumption is that the client has quit and
			// restarted with new peer id
			
			//System.out.println( "new peer" );
			
			
			TRTrackerServerPeerImpl old_peer	= (TRTrackerServerPeerImpl)peer_reuse_map.get( reuse_key );
			
			if ( old_peer != null ){
				
				removePeer( old_peer );
			}
			
			if ( !stopped ){			
				
				try{
					
					byte[]	peer_bytes = peer_id.getBytes( Constants.BYTE_ENCODING );
					
					peer = new TRTrackerServerPeerImpl( peer_bytes, ip_address.getBytes(), port );
					
					peer_map.put( peer_id, peer );
					
					peer_list.add( peer );
					
					peer_reuse_map.put( reuse_key, peer );
					
				}catch( UnsupportedEncodingException e){
					
					e.printStackTrace();
				}
			}
		}else{
			
			if ( stopped ){
				
				removePeer( peer );
			}
		}
		
		if ( peer != null ){
			
			peer.setTimeout( System.currentTimeMillis() + ( interval_requested * 1000 * TRTrackerServerImpl.CLIENT_TIMEOUT_MULTIPLIER ));
			
			peer.setStats( uploaded, downloaded, left, numwant );
		}
		
		stats.addAnnounce();
		
		if ( completed ){
			
			stats.addCompleted();
		}
	}
	
	protected void
	removePeer(
		TRTrackerServerPeerImpl	peer )
	{
		if ( peer_map.size() != peer_list.size() || peer_list.size() != peer_reuse_map.size()){
	
			Debug.out( "TRTrackerServerTorrent::removePeer: maps/list size different");	
		}
		
		try{
			Object o = peer_map.remove( new String( peer.getPeerId(), Constants.BYTE_ENCODING ));
			
			if ( o == null ){
				
				Debug.out(" TRTrackerServerTorrent::removePeer: peer_map doesn't contain peer");
			}
		}catch( UnsupportedEncodingException e ){
		}										
		
		if ( !peer_list.remove( peer )){
			
			Debug.out(" TRTrackerServerTorrent::removePeer: peer_list doesn't contain peer");
		}
		
		try{
			Object o = peer_reuse_map.remove( new String( peer.getIPAsRead(), Constants.BYTE_ENCODING ) + ":" + peer.getPort());
		
			if ( o == null ){
				
				Debug.out(" TRTrackerServerTorrent::removePeer: peer_reuse_map doesn't contain peer");
			}
			
		}catch( UnsupportedEncodingException e ){
		}										
	}
	
	protected void
	removePeer(
		Iterator				peer_map_iterator,
		TRTrackerServerPeerImpl	peer )
	{
		if ( peer_map.size() != peer_list.size() || peer_list.size() == peer_reuse_map.size()){
			
			Debug.out( "TRTrackerServerTorrent::removePeer: maps/list size different");	
		}
		
		peer_map_iterator.remove();
		
		if ( !peer_list.remove( peer )){
			
			Debug.out(" TRTrackerServerTorrent::removePeer: peer_list doesn't contain peer");
		}
		
		try{
			Object o = peer_reuse_map.remove( new String( peer.getIPAsRead(), Constants.BYTE_ENCODING ) + ":" + peer.getPort());
			
			if ( o == null ){
				
				Debug.out(" TRTrackerServerTorrent::removePeer: peer_reuse_map doesn't contain peer");
			}
			
		}catch( UnsupportedEncodingException e ){
		}										
	}
	
	public synchronized void
	exportPeersToMap(
		Map		map,
		int		num_want )
	{
		int		max_peers	= TRTrackerServerImpl.getMaxPeersToSend();
		int		total_peers	= peer_map.size();
		
			// num_want < 0 -> not supplied to give them max
		
		if ( num_want < 0 ){
			
			num_want = total_peers;
		}
		
			// trim back to max_peers if specified
		
		if ( max_peers > 0 && num_want > max_peers ){
			
			num_want	= max_peers;
		}
	
		// System.out.println( "exportPeersToMap: num_want = " + num_want + ", max = " + max_peers );
		
		long	now = System.currentTimeMillis();
		
		List	rep_peers = new ArrayList();
			
		map.put( "peers", rep_peers );
		
		boolean	send_peer_ids = TRTrackerServerImpl.getSendPeerIds();
		
			// if they want them all simply give them the set
		
		if ( num_want == 0 ){
			
			return;
			
		}else if ( num_want >= total_peers){
	
				// if they want them all simply give them the set
			
			Iterator	it = peer_map.values().iterator();
					
			while(it.hasNext()){
		
				TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)it.next();
								
				if ( now > peer.getTimeout()){
								
						// System.out.println( "removing timed out client '" + peer.getString());
					
					removePeer( it, peer );									
					
				}else{
									
					Map rep_peer = new HashMap();
		
					rep_peers.add( rep_peer );
											
					if ( send_peer_ids ){
						
						rep_peer.put( "peer id", peer.getPeerId() );
					}
					
					rep_peer.put( "ip", peer.getIPAsRead() );
					rep_peer.put( "port", new Long( peer.getPort()));
				}
			}
		}else{
			if ( num_want < total_peers*3 ){
				
					// too costly to randomise as below. use more efficient but slightly less accurate
					// approach
				
				int	limit 	= (num_want*3)/2;
				int	added	= 0;
				
				for (int i=0;i<limit && added < num_want;i++){
					
					int	index = random.nextInt(peer_list.size());
					
					TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)peer_list.get(index);
	
					if ( now > peer.getTimeout()){
						
						removePeer( peer );
						
					}else{
				
						added++;
						
						Map rep_peer = new HashMap();
						
						rep_peers.add( rep_peer );
						
						if ( send_peer_ids ){
							
							rep_peer.put( "peer id", peer.getPeerId() );
						}
						
						rep_peer.put( "ip", peer.getIPAsRead() );
						rep_peer.put( "port", new Long( peer.getPort()));					
					}
				}
				
			}else{
				
					// randomly select the peers to return
				
				LinkedList	peers = new LinkedList( peer_map.keySet());
				
				int	added = 0;
				
				while( added < num_want && peers.size() > 0 ){
					
					String	key = (String)peers.remove(random.nextInt(peers.size()));
									
					TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)peer_map.get(key);
					
					if ( now > peer.getTimeout()){
						
						removePeer( peer );
						
					}else{
						
						added++;
						
						Map rep_peer = new HashMap();
						
						rep_peers.add( rep_peer );
						
						if ( send_peer_ids ){
							
							rep_peer.put( "peer id", peer.getPeerId() );
						}
						
						rep_peer.put( "ip", peer.getIPAsRead() );
						rep_peer.put( "port", new Long( peer.getPort()));
					}
				}
			}
		}
	}
	
	public synchronized void
	checkTimeouts()
	{
		long	now = System.currentTimeMillis();
		
		Iterator	it = peer_map.values().iterator();
				
		while(it.hasNext()){
			
			TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)it.next();
			
			if ( now > peer.getTimeout()){
				
				removePeer( it, peer );
			}
		}
	}
	
	protected synchronized Map
	exportScrapeToMap()
	{
		long	now = System.currentTimeMillis();
		
		if ( last_scrape != null && now - last_scrape_calc_time < SCRAPE_CACHE_PERIOD ){
			
			return( last_scrape );
		}
		
		last_scrape 			= new HashMap();
		last_scrape_calc_time	= now;
		
		long	seeds 		= 0;
		long	non_seeds	= 0;
		
		for (int i=0;i<peer_list.size();i++){
			
			TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)peer_list.get(i);
			
			if ( peer.getAmountLeft() == 0 ){
				
				seeds++;
				
			}else{
				
				non_seeds++;
			}
		}
		
		last_scrape.put( "complete", new Long( seeds ));
		last_scrape.put( "incomplete", new Long( non_seeds ));
		last_scrape.put( "downloaded", new Long(stats.getCompletedCount()));
		
		return( last_scrape );
	}
	
	protected TRTrackerServerTorrentStats
	getStats()
	{
		return( stats );
	}
	
	public synchronized TRTrackerServerPeer[]
	getPeers()
	{
		TRTrackerServerPeer[]	res = new TRTrackerServerPeer[peer_map.size()];
		
		peer_map.values().toArray( res );
		
		return( res );
	}
	
	public HashWrapper
	getHash()
	{
		return( hash );
	}
}
