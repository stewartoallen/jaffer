/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

import com.strangeberry.rendezvous.Rendezvous;
import com.strangeberry.rendezvous.ServiceInfo;

/* TODO: request/reply queues
 *   * command 0x7a - client going to sleep?
 *
 * AFP BUGS:
 *   * sending truncated list for GetVolParams bitmap results in kernel panic / crash
 *   * Login (0x12) is *not* sending pad byte after command
 *   * failure to reply to DSI_TICKLE results in kernel panic
 *   * AFP31 p. 32 - offspring count is 2 bytes, NOT 4 bytes
 *   * sending TICKLE with 'reply' flag panics kernel
 *   * failing to complete an enumerate_ext2 call will panic/hang the system
 *   * responding to DSI_WRITE with mirror reply panics kernel
 */
public abstract class AFP_Server implements AFP_Constants, Runnable
{
	static boolean DEBUG_DEBUG = true;
	static boolean DEBUG_DSI = true;
	static boolean DEBUG_DSI_REQUEST = true && DEBUG_DSI;
	static boolean DEBUG_DSI_REPLY = true && DEBUG_DSI;
	static boolean DEBUG_DSI_LINE = true && DEBUG_DSI;
	public static boolean DEBUG_PRINT = DEBUG_DEBUG | DEBUG_DSI;

	private final static String[] protoStrings = {
		"AFP3.1" ,
		"AFP2.3"
	};

	private int port;
	private String bind;
	private ServerSocket socket;
	private String serverName;
	private Thread thread;
	private int nextVolID = 1;
	private Hashtable volumesByID;
	private Hashtable volumesByName;
	private Rendezvous rendezvous;
	private AFP_ServerInfo serverInfo;

	public AFP_Server()
		throws IOException
	{
		this(TCP_PORT);
	}

	public AFP_Server(int port)
		throws IOException
	{
		this(null, port);
	}

	public AFP_Server(String rname, int port)
		throws IOException
	{
		this(rname, null, port);
	}

	public AFP_Server(String rname, String bind, int port)
		throws IOException
	{
		this.bind = bind;
		this.port = port;
		this.rendezvous = new Rendezvous();
		this.volumesByID = new Hashtable();
		this.volumesByName = new Hashtable();
		// set debug level
		String dl = System.getProperty("debug.afp");
		if (dl != null && dl.length() > 0)
		{
			setDebugLevel(Integer.parseInt(dl));
		}
		// register server with Rendezvous
		InetAddress addr = InetAddress.getLocalHost();
		serverName = rname != null ? rname : addr.getHostName();
		if (rname == null && serverName.indexOf('.') > 0)
		{
			serverName = serverName.substring(0, serverName.indexOf('.'));
		}
		rendezvous.registerService(new ServiceInfo(
			"_afpovertcp._tcp.local.", serverName+"._afpovertcp._tcp.local.", addr, port, 1, 1, "Java AFP Server"
		));
	}

	public void setDebugLevel(int lvl)
	{
		DEBUG_DEBUG = true;
		DEBUG_DSI = true;
		DEBUG_DSI_REQUEST = DEBUG_DSI;
		DEBUG_DSI_REPLY = DEBUG_DSI;
		DEBUG_DSI_LINE = DEBUG_DSI;
		DEBUG_PRINT = DEBUG_DEBUG | DEBUG_DSI;

		switch (lvl)
		{
			case 0:
				DEBUG_DEBUG=false;
				DEBUG_PRINT=false;
				DEBUG_DSI=false;
				break;
			case 1:
				DEBUG_DSI_REQUEST=false;
				DEBUG_DSI_REPLY=false;
				break;
			default:
				break;
		}

		DEBUG_DSI_REQUEST &= DEBUG_DSI;
		DEBUG_DSI_REPLY &= DEBUG_DSI;
		DEBUG_DSI_LINE &= DEBUG_DSI;
	}

	public synchronized int addVolume(AFP_Volume vol)
	{
		int id = nextVolID++;
		volumesByID.put(new Integer(id), vol);
		volumesByName.put(vol.getName(), vol);
		vol.setID(id);
		return id;
	}

	public AFP_Volume getVolume(int vid)
	{
		return (AFP_Volume)volumesByID.get(new Integer(vid));
	}

	public AFP_Volume getVolume(String vname)
	{
		return (AFP_Volume)volumesByName.get(vname);
	}

	public synchronized AFP_Volume[] getVolumes()
	{
		Object k[] = volumesByName.keySet().toArray();
		AFP_Volume v[] = new AFP_Volume[k.length];
		for (int i=0; i<k.length; i++)
		{
			v[i] = (AFP_Volume)volumesByName.get(k[i]);
		}
		return v;
	}

	public synchronized void delVolume(AFP_Volume vol)
	{
		if (vol == null)
		{
			return;
		}
		volumesByName.remove(vol.getName());
		volumesByID.remove(new Integer(vol.getID()));
	}

	public void delVolume(String vname)
	{
		delVolume((AFP_Volume)volumesByName.get(vname));
	}

	public void delVolume(int vid)
	{
		delVolume((AFP_Volume)volumesByID.get(new Integer(vid)));
	}

	public synchronized void start()
		throws IOException
	{
		if (thread != null)
		{
			return;
		}
		socket = bind != null ?
			new ServerSocket(port, 10, InetAddress.getByName(bind)) :
			new ServerSocket(port);
		thread = new Thread(this, "AFP Server");
		thread.start();
	}

	public void run()
	{
		try
		{
			System.out.println(
				"Jaffer AFP/TCP Server v"+Main.VERSION+
				" ready on port "+socket.getLocalPort()+" as '"+serverName+"'");
			while (true)
			{
				acceptConnection();
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public abstract boolean hasCleartextPasswords()
		;

	public abstract boolean hasUser(String userName)
		;

	public abstract boolean checkPassword(String userName, String password)
		;

	public abstract boolean setThreadOwner(String userName)
		;

	public abstract String getPassword(String userName)
		;

	public abstract String getGuestUser()
		;

	public AFP_ServerInfo getServerInfo()
	{
		if (serverInfo == null)
		{
			serverInfo = new AFP_ServerInfo(serverName, protoStrings,
				hasCleartextPasswords() ?
					new String[] { UAM_STR_GUEST, UAM_STR_CLEARTEXT, UAM_STR_RANDOM_NUM1, UAM_STR_DHX_128 } :
					new String[] { UAM_STR_GUEST, UAM_STR_CLEARTEXT, UAM_STR_DHX_128 },
				0
			);
		}
		return serverInfo;
	}

	private void acceptConnection()
		throws IOException
	{
		Socket s = socket.accept();
		s.setTcpNoDelay(true);
		System.out.println("AFP_Server: connect from "+s.getInetAddress());
		AFP_Session session = new AFP_Session(this, s);
		session.start();
	}
}

