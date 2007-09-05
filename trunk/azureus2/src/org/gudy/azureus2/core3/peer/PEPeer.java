/*
 * File    : PEPeerSocket.java
 * Created : 15-Oct-2003
 * By      : Olivier
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

/*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core3.peer;


import org.gudy.azureus2.plugins.network.Connection;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;


/**
 * @author Olivier
 * @author MjrTom
 *			2005/Oct/08: lastPiece handling
 *
 */

public interface 
PEPeer 
{
	public final static int CONNECTING 		= 10;
	public final static int HANDSHAKING 	= 20;
	public final static int TRANSFERING 	= 30;
	public final static int CLOSING      	= 40;
	public final static int DISCONNECTED 	= 50;
	
	
	// these should be maintained to match above list ordinals /10
	// if they don't than some debug info won't work right (not too big of a deal)
	public final static String[] StateNames = { "Twinkle",
		"Connecting", "Handshaking", "Transfering", "Closing", "Disconnected"
	};
	
	public static final int MESSAGING_BT_ONLY = 1;
	public static final int MESSAGING_AZMP = 2;
	public static final int MESSAGING_LTEP = 3;
	//used for plugins, such es webseeds
	public static final int MESSAGING_EXTERN = 4;
	
	
  /**
   * Add peer listener.
   * @param listener
   */
  public void addListener( PEPeerListener listener );
  
  
  /**
   * Remove peer listener.
   * @param listener
   */
  public void removeListener( PEPeerListener listener );
  
  
	public int getPeerState();	// from above set
  
	public PEPeerManager
	getManager();

	public String
	getPeerSource();
	
	public byte[] getId();

	public String getIp();
  
  /**
   * Get the peer's local TCP connection port.
   * @return local port
   */
  public int getPort();
 
	/**
	 * Gets the host name for the IP, if possible, IP as string otherwise
   * @return hostname or IP
   */
	public String getIPHostName();

  
  /**
   * Get the TCP port this peer is listening for incoming connections on.
   * @return TCP port, or 0 if port is unknown
   */
  public int getTCPListenPort();
  
  /**
   * Get the UDP port this peer is listening for incoming connections on.
   * @return UDP port, or 0 if port is unknown
   */
  public int getUDPListenPort();

  /**
   * Get the UDP port this peer is listening on for non-data connections
   * @return
   */
  
  public int getUDPNonDataListenPort();
  
	public BitFlags getAvailable();
	/**
	 * @param pieceNumber int
	 * @return true if this peers makes this piece available
	 */
	public boolean isPieceAvailable(int pieceNumber);

	public boolean
	transferAvailable();
	
	public void setSnubbed(boolean b);	// explicit un-snub
  
  /**
   * Is the peer choking me.
   * @return true if I am choked by the peer, false if not
   */
	public boolean isChokingMe();

  /**
   * Am I choking the peer.
   * @return true if the peer is choked, false if not
   */
	public boolean isChokedByMe();

  /**
   * Am I Interested in the peer.
   * @return true if peer is interesting, false if not
   */
	public boolean isInteresting();

  /**
   * Is the peer Interested in me.
   * @return true if the peer is interested in me, false if not
   */
	public boolean isInterested();

	/**
	 * checks several factors within the object so the caller wouldn't need to
	 * for convienience and speed.
	 * @return true if none of several criteria indicate a request can't be made of the peer  
	 */
	public boolean isDownloadPossible();
	
	public boolean isSeed();
 
	public boolean isSnubbed();
	
	public long getSnubbedTime();
 
	public PEPeerStats getStats();
 	
	public boolean isIncoming();

	public boolean hasReceivedBitField();
	
  /**
   * Get the peer's torrent completion percentage in thousand-notation,
   * i.e. 53.7% is returned as the value 0537.
   * @return the percentage the peer has complete
   */
	public int getPercentDoneInThousandNotation();

  
	public String getClient();

	public boolean isOptimisticUnchoke();
  public void setOptimisticUnchoke( boolean is_optimistic );
	
	//Used in super-seed mode
	//The lower the better
	public void setUploadHint(int timeToSpread);
	
	public int getUploadHint();
	
	public void setUniqueAnnounce(int uniquePieceNumber);
	
	public int getUniqueAnnounce();
   
	public int getConsecutiveNoRequestCount();
	public void setConsecutiveNoRequestCount( int num );

	public void setUploadRateLimitBytesPerSecond( int bytes );
	public void setDownloadRateLimitBytesPerSecond( int bytes );
	public int getUploadRateLimitBytesPerSecond();
	public int getDownloadRateLimitBytesPerSecond();
	
	public void
	addRateLimiter(
		LimitedRateGroup	limiter,
		boolean				upload );
	
	public void
	removeRateLimiter(
		LimitedRateGroup	limiter,
		boolean				upload );
	
  /** To retreive arbitrary objects against a peer. */
  public Object getData (String key);
  /** To store arbitrary objects against a peer. */
  public void setData (String key, Object value);
  
  
  /**
   * Get the connection that backs this peer.
   * @return connection
   */
  
  public Connection getPluginConnection();
  
  
  /**
   * Whether or not this peer supports the advanced messaging API.
   * @return true if extended messaging is supported, false if not
   */
  public boolean supportsMessaging();
  
  /**
   * @Return the handshaked messaging type, {@link PEPeer} constants
   */ 
  public int getMessagingMode();

  
  
  /**
   * Returns name of encryption used by the peer
   * @return
   */
  public String
  getEncryption();
  
  /**
   * Get the list of messages that this peer and us both understand.
   * @return messages available for use, or null of supported is yet unknown or unavailable
   */
  public Message[] getSupportedMessages();
  
  /**
   * Sets the reserved piece for piece picking by this peer
   */
  public void setReservedPieceNumber(int pieceNumber);
  
  /**
   * Get the reserved piece for piece picking by this peer
   */
  public int getReservedPieceNumber();
  
  public int getIncomingRequestCount();
  public int getOutgoingRequestCount();
  
  /**
   * amount of data queued for delivery to peer
   * @return
   */
  
  public int getOutboundDataQueueSize();
  
  /**
   * get a list of piece numbers the peer has requested
   * @return list of Long() representing the piece number requested, in order
   */
  public int[] getIncomingRequestedPieceNumbers();
  
  /**
   * get a list of piece numbers the we have requested from peer
   * @return list of Long() representing the piece number requested, oldest
   *          to newest
   */
  public int[] getOutgoingRequestedPieceNumbers();
  
  public int
  getPercentDoneOfCurrentIncomingRequest();
  
  public int
  getPercentDoneOfCurrentOutgoingRequest();
  
  /**
   * Get the time since this connection was first established.
   * NOTE: This method will always return 0 at any time before
   * the underlying transport is fully connected, i.e. before
   * handshaking begins.
   * @return time count in ms
   */
  public long getTimeSinceConnectionEstablished();

	public void setLastPiece(int i);
	public int getLastPiece();
	
	public boolean
	isLANLocal();
	
		/**
		 * Send a request hint to the peer. 
		 * @param piece_number
		 * @param offset
		 * @param length
		 * @param life
		 * @return true if sent, false otherwise
		 */
	
	public boolean
	sendRequestHint(
		int		piece_number,
		int		offset,
		int		length,
		int		life );
	
		/**
		 * Get current request hint for a given piece for this peer. 
		 * @return null if no hint int[]{ piece_number, offset, length } if hint found
		 */
	
	public int[]
	getRequestHint();
	        		 
	public void
	clearRequestHint();
	
	public void
	setHaveAggregationEnabled(
		boolean		enabled );
	
	public byte[] getHandshakeReservedBytes();
	
	public String getClientNameFromPeerID();
	public String getClientNameFromExtensionHandshake();
}