/*
 * Created on Mar 19, 2008
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


package com.aelitis.azureus.plugins.net.buddy;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.security.CryptoManagerKeyChangeListener;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.net.buddy.swt.BuddyPluginView;

public class 
BuddyPlugin 
implements Plugin
{
	public static final String VIEW_ID = "azbuddy";

	private static final int	TIMER_PERIOD	= 60*1000;
	
	private static final int	BUDDY_STATUS_CHECK_PERIOD	= 60*1000;
	private static final int	BUDDY_STATUS_CHECK_TICKS	= BUDDY_STATUS_CHECK_PERIOD/TIMER_PERIOD;
	
	private static final int	STATUS_REPUBLISH_PERIOD		= 5*60*1000;
	private static final int	STATUS_REPUBLISH_TICKS		= STATUS_REPUBLISH_PERIOD/TIMER_PERIOD;

	
	private PluginInterface	plugin_interface;
	
	private LoggerChannel	logger;
	
	private ActionParameter test_button;
			
	private boolean			ready_to_publish;
	private publishDetails	current_publish		= new publishDetails();
	private publishDetails	latest_publish		= current_publish;
	
	
	private AsyncDispatcher	publish_dispatcher = new AsyncDispatcher();
	
	private	DistributedDatabase 	ddb;
	
	private CryptoHandler ecc_handler = CryptoManagerFactory.getSingleton().getECCHandler();

	private List	buddies = new ArrayList();
	
	private boolean	is_enabled;

	private CopyOnWriteList	listeners = new CopyOnWriteList();
	
	public void
	initialize(
		final PluginInterface		_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		String name_res = "Views.plugins." + VIEW_ID + ".title";
		
		String name = 
			plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( name_res );
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		name );

		if ( !Constants.isCVSVersion()){
			
			return;
		}
		
		logger = plugin_interface.getLogger().getChannel( "Buddy" );
		
		logger.setDiagnostic();
				
		BasicPluginConfigModel config = plugin_interface.getUIManager().createBasicPluginConfigModel( name_res );
				

		final BooleanParameter enabled = config.addBooleanParameter2( "enabled", "enabled", false );
		
		enabled.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						setEnabled( enabled.getValue());
					}
				});
		
		final StringParameter buddy_pk = config.addStringParameter2( "other buddy key", "other buddy key", "" );
		
		buddy_pk.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						String	value = buddy_pk.getValue().trim();
						
						byte[] bytes = Base32.decode( value );					
						
						test_button.setEnabled( ecc_handler.verifyPublicKey( bytes )); 
					}
				});
		
		test_button = config.addActionParameter2( "add the key", "do it!" );
		
		test_button.setEnabled( false );
		
		test_button.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					addBuddy( buddy_pk.getValue().trim());
				}
			});
		
		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						
						UISWTInstance swt_ui = (UISWTInstance)instance;
						
						BuddyPluginView view = new BuddyPluginView( BuddyPlugin.this );

						swt_ui.addView(	UISWTInstance.VIEW_MAIN, VIEW_ID, view );
						
						//swt_ui.openMainView( VIEW_ID, view, null );
					}
				}

				public void
				UIDetached(
					UIInstance		instance )
				{
				}
			});
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					new AEThread2( "NetstatusPlugin:init", true )
					{
						public void
						run()
						{
							loadBuddies();
							
							try{
								ddb = plugin_interface.getDistributedDatabase();
							
									// pick up initial values before enabling

								ddb.addListener(
									new DistributedDatabaseListener()
									{
										public void 
										event(
											DistributedDatabaseEvent event )
										{
											if ( event.getType() == DistributedDatabaseEvent.ET_LOCAL_CONTACT_CHANGED ){
												
												updateIP();
											}
										}
									});
										
								updateIP();
								
								COConfigurationManager.addAndFireParameterListeners(
										new String[]{
											"TCP.Listen.Port",
											"TCP.Listen.Port.Enable",
											"UDP.Listen.Port",
											"UDP.Listen.Port.Enable" },
										new org.gudy.azureus2.core3.config.ParameterListener()
										{
											public void 
											parameterChanged(
												String parameterName )
											{
												updateListenPorts();
											}
										});
								
								CryptoManagerFactory.getSingleton().addKeyChangeListener(
									new CryptoManagerKeyChangeListener()
									{
										public void 
										keyChanged(
											CryptoHandler handler ) 
										{
											updateKey();
										}
									});
								
								ready_to_publish	= true;
								
								setEnabled( enabled.getValue());
								
								checkBuddiesAndRepublish();
								
							}catch( Throwable e ){
							
								log( "Initialisation failed", e );
							}
						}
					}.start();
				}
				
				public void
				closedownInitiated()
				{				
				}
				
				public void
				closedownComplete()
				{				
				}
			});
	}
	
	protected void
	setEnabled(
		boolean		_enabled )
	{
		synchronized( this ){
			
			is_enabled	= _enabled;
			
			if ( latest_publish.isEnabled() != _enabled ){
				
				publishDetails new_publish = latest_publish.getCopy();
				
				new_publish.setEnabled( _enabled );
				
				updatePublish( new_publish );
			}
		}
	}
	
	protected void
	updateListenPorts()
	{
		synchronized( this ){

			int	tcp_port = COConfigurationManager.getIntParameter( "TCP.Listen.Port" );
			boolean	tcp_enabled = COConfigurationManager.getBooleanParameter( "TCP.Listen.Port.Enable" );
			int	udp_port = COConfigurationManager.getIntParameter("UDP.Listen.Port" );
			boolean	udp_enabled = COConfigurationManager.getBooleanParameter( "UDP.Listen.Port.Enable" );
				
			if ( !tcp_enabled ){
				
				tcp_port = 0;
			}
			
			if ( !udp_enabled ){
				
				udp_port = 0;
			}
			
			if ( 	latest_publish.getTCPPort() != tcp_port ||
					latest_publish.getUDPPort() != udp_port ){
				
				publishDetails new_publish = latest_publish.getCopy();
				
				new_publish.setTCPPort( tcp_port );
				new_publish.setUDPPort( udp_port );
				
				updatePublish( new_publish );
			}
		}
	}
	
	protected void
	updateIP()
	{
		if ( ddb == null ){
			
			return;
		}
				
		synchronized( this ){

			InetAddress public_ip = ddb.getLocalContact().getAddress().getAddress();
				
			if ( 	latest_publish.getIP() == null ||
					!latest_publish.getIP().equals( public_ip )){
					
				publishDetails new_publish = latest_publish.getCopy();
				
				new_publish.setIP( public_ip );
				
				updatePublish( new_publish );
			}
		}
	}
	
	protected void
	updateKey()
	{
		synchronized( this ){

			publishDetails new_publish = latest_publish.getCopy();
				
			new_publish.setPublicKey( null );
				
			updatePublish( new_publish );
		}
	}
	
	protected void
	updatePublish(
		final publishDetails	details )
	{
		latest_publish = details;
		
		if ( ddb == null || !ready_to_publish ){
			
			return;
		}
		
		publish_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
						// only execute the most recent publish
					
					if ( publish_dispatcher.getQueueSize() > 0 ){
						
						return;
					}
					
					updatePublishSupport( details );
				}
			});
	}
	
	protected void
	updatePublishSupport(
		publishDetails	details )
	{
		byte[]	key_to_remove = null;
		
		publishDetails	existing_details;
		
		boolean	was_published;
		
		synchronized( this ){

			was_published = current_publish.isPublished();
			
			existing_details = current_publish;
			
			if ( !details.isEnabled()){
				
				if ( current_publish.isPublished()){
					
					key_to_remove	= current_publish.getPublicKey();
				}
			}else{
								
				if ( details.getPublicKey() == null ){
					
					try{
						details.setPublicKey( ecc_handler.getPublicKey( "Creating online status key" ));
						
					}catch( Throwable e ){
						
						log( "Failed to publish details", e );
						
						return;
					}			
				}
				
				if ( current_publish.isPublished()){
					
					byte[]	existing_key = current_publish.getPublicKey();
				
					if ( !Arrays.equals( existing_key, details.getPublicKey())){
						
						key_to_remove = existing_key;
					}
				}
			}
			
			current_publish = details;
		}
		
		if ( key_to_remove != null ){
			
			log( "Removing old status publish: " + existing_details.getString());
			
			try{
				ddb.delete(
					new DistributedDatabaseListener()
					{
						public void
						event(
							DistributedDatabaseEvent		event )
						{
						}
					},
					getStatusKey( key_to_remove, "Buddy status de-registration for old key" ));
				
			}catch( Throwable e ){	
			
				log( "Failed to remove existing publish", e );
			}
		}
		
		if ( details.isEnabled()){
			
				// ensure we have a sensible ip
			
			InetAddress ip = details.getIP();
			
			if ( ip.isLoopbackAddress() || ip.isLinkLocalAddress() || ip.isSiteLocalAddress()){
				
				log( "Can't publish as ip address is invalid: " + details.getString());
				
				return;
			}
			
			log( "Publishing new status: " + details.getString());

			details.setPublished( true );
			
			Map	payload = new HashMap();
			
			if ( details.getTCPPort() > 0 ){
			
				payload.put( "t", new Long(  details.getTCPPort() ));
			}
			
			if (  details.getUDPPort() > 0 ){
				
				payload.put( "u", new Long( details.getUDPPort() ));
			}
						
			payload.put( "i", ip.getAddress());
			
			try{
				byte[] data = BEncoder.encode( payload );
										
				DistributedDatabaseKey	key = getStatusKey( details.getPublicKey(), "Buddy status registration" );
	
				byte[] signature = ecc_handler.sign( data, "Buddy online status" );
			
				byte[]	signed_payload = new byte[ 1 + signature.length + data.length ];
				
				signed_payload[0] = (byte)signature.length;
				
				System.arraycopy( signature, 0, signed_payload, 1, signature.length );
				System.arraycopy( data, 0, signed_payload, 1 + signature.length, data.length );		
				
				DistributedDatabaseValue	value = ddb.createValue( signed_payload );
				
				final AESemaphore	sem = new AESemaphore( "BuddyPlugin:reg" );
				
				if ( !was_published ){
					
					logMessage( "Publishing status starts" );
				}
				
				ddb.write(
					new DistributedDatabaseListener()
					{
						public void
						event(
							DistributedDatabaseEvent		event )
						{
							int	type = event.getType();
						
							if ( 	type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ||
									type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){

								sem.release();
							}
						}
					},
					key,
					value );
				
				sem.reserve();
				
				if ( !was_published ){
				
					logMessage( "Publishing status complete" );
				}
			}catch( Throwable e ){
				
				log( "Failed to publish online status", e );
			}
		}
	}
	
	protected DistributedDatabaseKey
	getStatusKey(
		byte[]	public_key,
		String	reason )
	
		throws Exception
	{
		byte[]	key_prefix = "azbuddy:status".getBytes();
		
		byte[]	key_bytes = new byte[ key_prefix.length + public_key.length ];
		
		System.arraycopy( key_prefix, 0, key_bytes, 0, key_prefix.length );
		System.arraycopy( public_key, 0, key_bytes, key_prefix.length, public_key.length );
		
		DistributedDatabaseKey key = ddb.createKey( key_bytes, reason );
		
		return( key );
	}

	protected void
	loadBuddies()
	{
		List buddies_config = plugin_interface.getPluginconfig().getPluginListParameter( "buddies", new ArrayList());

		for (int i=0;i<buddies_config.size();i++){
			
			Object o = buddies_config.get(i);

			if ( o instanceof Map ){
				
				Map	details = (Map)o;
				
				String	key = new String((byte[])details.get("pk"));
				
				BuddyPluginBuddy buddy = new BuddyPluginBuddy( this, key );
				
				logMessage( "Loaded buddy " + buddy.getString());
				
				buddies.add( buddy );
			}
		}
	}
	
	protected void
	addBuddy(
		String		key )
	{
		if ( key.length() == 0 ){
			
			return;
		}
				
		synchronized( this ){
			
			for (int i=0;i<buddies.size();i++){
				
				BuddyPluginBuddy buddy = (BuddyPluginBuddy)buddies.get(i);
				
				if ( buddy.getPublicKey().equals( key )){
					
					return;
				}
			}
			
			BuddyPluginBuddy buddy = new BuddyPluginBuddy( this, key );
			
			buddies.add( buddy );
			
			logMessage( "Added buddy " + buddy.getString());

			List buddies_config = plugin_interface.getPluginconfig().getPluginListParameter( "buddies", new ArrayList());

			Map	map = new HashMap();
			
			map.put( "pk", key );
			
			buddies_config.add( map );
				
			plugin_interface.getPluginconfig().setPluginListParameter( "buddies", buddies_config );
		}
	}
	
	protected void
	checkBuddiesAndRepublish()
	{
		plugin_interface.getUtilities().createTimer( "Buddy checker" ).addPeriodicEvent(
			TIMER_PERIOD,
			new UTTimerEventPerformer()
			{
				int	tick_count;
				
				public void 
				perform(
					UTTimerEvent event ) 
				{
					tick_count++;
					
					if ( !is_enabled ){
						
						return;
					}
					
					if ( tick_count % BUDDY_STATUS_CHECK_TICKS == 0 ){
												
						for (int i=0;i<buddies.size();i++){
							
							updateBuddyStatus((BuddyPluginBuddy)buddies.get(i));
						}
					}
					
					if ( tick_count % STATUS_REPUBLISH_TICKS == 0 ){

						synchronized( this ){
							
							if ( latest_publish.isEnabled()){
								
								updatePublish( latest_publish );
							}
						}
					}
				}
			});
	}
	
	protected void
	updateBuddyStatus(
		final BuddyPluginBuddy	buddy )
	{
		log( "Updating buddy status: " + buddy.getString());

		try{							
			final byte[]	public_key = Base32.decode( buddy.getPublicKey());

			DistributedDatabaseKey	key = 
				getStatusKey( public_key, "Buddy status registration" );
			
			ddb.read(
				new DistributedDatabaseListener()
				{
					private long	latest_time;
					private Map		status;
					
					public void
					event(
						DistributedDatabaseEvent		event )
					{
						int	type = event.getType();
						
						if ( type == DistributedDatabaseEvent.ET_VALUE_READ ){
							
							try{
								DistributedDatabaseValue value = event.getValue();
								
								long time = value.getCreationTime();
								
								if ( time > latest_time ){
								
									byte[] signed_payload = (byte[])value.getValue( byte[].class );
								
									int	signature_length = ((int)signed_payload[0])&0xff;
									
									byte[]	signature 	= new byte[ signature_length ];
									byte[]	data		= new byte[ signed_payload.length - 1 - signature_length];
									
									System.arraycopy( signed_payload, 1, signature, 0, signature_length );
									System.arraycopy( signed_payload, 1 + signature_length, data, 0, data.length );
											
									if ( ecc_handler.verify( public_key, data, signature )){													
	
										status = BDecoder.decode( data );
																																							
										latest_time = time;

									}else{
										
										log( "Verification failed" );
									}
								}
							}catch( Throwable e ){
								
								log( "Read failed", e );
							}
						}else if ( 	type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ||
									type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
							
							if ( status == null ){
																
								buddy.statusCheckFailed();
								
							}else{
								
								try{
									int	tcp_port = ((Long)status.get( "t" )).intValue();
									int udp_port = ((Long)status.get( "u" )).intValue();
									
									InetAddress ip = InetAddress.getByAddress((byte[])status.get("i"));
									
									buddy.updateStatus( latest_time, ip, tcp_port, udp_port );
									
								}catch( Throwable e ){
									
									log( "Status decode failed", e );
								}
							}
						}
					}
				},
				key,
				120*1000 );
			
		}catch( Throwable e ){
			
			log( "Buddy status update failed: " + buddy.getString(), e );
		}
	}
	
	public void
	addListener(
		BuddyPluginListener	listener )
	{
		listeners.add( listener );
	}
	
	public void
	logMessage(
		String		str )
	{
		log( str );
		
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			((BuddyPluginListener)it.next()).messageLogged( str );
		}
	}
	
	public void
	log(
		String		str )
	{
		logger.log( str );
	}
	
	public void
	log(
		String		str,
		Throwable	e )
	{
		logger.log( str );
		logger.log( e );
	}

	private class
	publishDetails
		implements Cloneable
	{
		private byte[]			public_key;
		private InetAddress		ip;
		private int				tcp_port;
		private int				udp_port;
		
		private boolean			enabled;
		private boolean			published;
		
		protected publishDetails
		getCopy()
		{
			try{
				publishDetails copy = (publishDetails)clone();
				
				copy.published = false;
				
				return( copy );
				
			}catch( Throwable e ){
				
				return( null);
			}
		}
		
		protected boolean
		isPublished()
		{
			return( published );
		}
		
		protected void
		setPublished(
			boolean		b )
		{
			published	= b;
		}
		
		protected boolean
		isEnabled()
		{
			return( enabled );
		}
		
		protected void
		setEnabled(
			boolean	_enabled )
		{
			enabled	= _enabled;
		}
		
		protected byte[]
		getPublicKey()
		{
			return( public_key );
		}
		
		protected void
		setPublicKey(
			byte[]		k )
		{
			public_key	= k;
		}
		
		protected InetAddress
		getIP()
		{
			return( ip );
		}
		
		protected void
		setIP(
			InetAddress	_ip )
		{
			ip	= _ip;
		}
		
		protected int
		getTCPPort()
		{
			return( tcp_port );
		}
		
		protected void
		setTCPPort(
			int		_port )
		{
			tcp_port = _port;
		}
		
		protected int
		getUDPPort()
		{
			return( udp_port );
		}
		
		protected void
		setUDPPort(
			int		_port )
		{
			udp_port = _port;
		}
		
		protected String
		getString()
		{
			return( "enabled=" + enabled + ",ip=" + ip + ",tcp=" + tcp_port + ",udp=" + udp_port + ",key=" + (public_key==null?"<none>":Base32.encode( public_key )));
		}
	}
}
