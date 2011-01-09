/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;

public class Main extends Utility implements AFP_Constants, DSI_Constants
{
	public final static double VERSION = 0.07;

	public static void main(String args[])
		throws Exception
	{
		if (args.length < 1)
		{
			usage();
		}
		else
		if (args[0].equals("-config") && args.length >= 2)
		{
			config(args[1]);
		}
		else
		if (args[0].equals("-proxy") && args.length >= 4)
		{
			int lport = args.length > 1 ? Integer.parseInt(args[1]) : TCP_PORT;
			String target = args.length > 2 ? args[2] : "localhost";
			int tport = args.length > 3 ? Integer.parseInt(args[3]) : TCP_PORT;
			proxy(lport, target, tport);
		}
		else
		if (args[0].equals("-server") && args.length >= 3)
		{
			int port = args.length > 1 ? Integer.parseInt(args[1]) : TCP_PORT;
			String vname = args.length > 2 ? args[2] : "test";
			String vdir = args.length > 3 ? args[3] : ".";
			String rname = args.length > 4 ? args[4] : null; // default hostname
			OS_Server server = new OS_Server(rname, port);
			server.addVolume(new OS_Volume(vname, new File(vdir), null));
			server.start();
		}
		else
		if (args[0].equals("-client") && args.length >= 3)
		{
			String target = args.length > 1 ? args[1] : "localhost";
			int port = args.length > 2 ? Integer.parseInt(args[2]) : TCP_PORT;
			client(target, port);
		}
		else
		{
			usage();
		}
	}

	private final static void usage()
	{
		System.out.println("Jaffer Server v"+VERSION+" Stewart Allen (stewart@neuron.com)\n");
		System.out.println("usage: jaffer -server [lport] [volume-name] [directory] [rendezvous-name]");
		System.out.println("   or: jaffer -proxy  [lport] [rhost] [rport]");
		System.out.println("   or: jaffer -config [config-file]");
		System.exit(1);
	}

	private final static void config(String cfg)
		throws Exception
	{
		File config = new File(cfg);
		if (!config.exists())
		{
			throw new FileNotFoundException("'"+config.getAbsolutePath()+"'");
		}
		FileReader fr = new FileReader(config);
		BufferedReader rr = new BufferedReader(fr);
		Config current = null;
		Config global = new Config(null);
		Config volumes = new Config(global);
		Config ports = new Config(global);
		String line = null;
		while ( (line = rr.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0 || line.startsWith("#"))
			{
				continue;
			}
			if (line.startsWith("[") && line.endsWith("]"))
			{
				String section = line.substring(1,line.length()-1);
				if (section.equals("global"))
				{
					current = global;
				}
				else
				if (section.startsWith("port:"))
				{
					Integer pkey = Integer.valueOf(section.substring(5,section.length()));
					current = ports.configValue(pkey);
					if (current == null)
					{
						current = new Config(global);
						ports.put(pkey, current);
					}
				}
				else
				{
					current = new Config(global);
					volumes.put(section, current);
				}
				current.put("#NAME#", section);
			}
			else
			if (current != null)
			{
				StringTokenizer st = new StringTokenizer(line, "=");
				if (st.countTokens() != 2)
				{
					continue;
				}
				String key = st.nextToken().trim();
				String value = st.nextToken().trim();
				current.put(key, value);
			}
		}
		for (Enumeration e = volumes.keys(); e.hasMoreElements(); )
		{
			Config vol = volumes.configValue(e.nextElement());
			int port = vol.intValue("port", AFP_Constants.TCP_PORT);
			String name = vol.stringValue("#NAME#");
			String path = vol.stringValue("path");
			String ro = vol.stringValue("read only");
			String pass = vol.stringValue("password");
			if (path == null)
			{
				System.out.println("Path not specified in '"+name+"'");
				continue;
			}
			File root = new File(path);
			if (!root.exists())
			{
				System.out.println("Path '"+root.getAbsolutePath()+"' does not exist for '"+name+"'");
				continue;
			}
			if (root.isDirectory()) {
				OS_Volume nvol = new OS_Volume(name, root, pass);
				nvol.setReadOnly(ro != null && ro.equalsIgnoreCase("true"));
				ports.append(new Integer(port), nvol);
			}
			else if (root.getName().toLowerCase().endsWith(".zip") || root.getName().toLowerCase().endsWith(".jar")) 
			{
				ports.append(new Integer(port), new ZIP_Volume(name, new ZipFile(root), pass));
			}
			else
			{
				System.out.println("Path '"+root.getAbsolutePath()+"' is invalid for '"+name+"'");
				continue;
			}
		}
		for (Enumeration e = ports.keys(); e.hasMoreElements(); )
		{
			Integer pkey = (Integer)e.nextElement();
			Config port = ports.configValue(pkey);
			OS_Server server = new OS_Server(
				port.stringValue("zeroconf name"),
				port.stringValue("interface"), pkey.intValue());
			server.setDebugLevel(ports.intValue("debug", 1));
			for (Enumeration v = port.keys(); v.hasMoreElements(); )
			{
				Object nv = v.nextElement();
				if (!(nv instanceof AFP_Volume))
				{
					continue;
				}
				server.addVolume((AFP_Volume)nv);
			}
			server.setGuestUser(port.stringValue("guest"));
			server.start();
		}
	}

	private final static void dummy_server()
		throws Exception
	{
		ServerSocket ss = new ServerSocket(TCP_PORT);
		Socket s = ss.accept();
		DSI_Packet dp = new DSI_Packet(new DataInputStream(s.getInputStream()));
		System.out.println("recv=("+dp+")");
		dp.setReply();
		dp.getWriter().writeBytes("unix".getBytes());
		System.out.println("send=("+dp+")");
		dp.write(s.getOutputStream());
		s.close();
		ss.close();
	}

	private final static void client(String host, int port)
		throws Exception
	{
		int reqID = 1;

		Socket s = new Socket(host, port);
		OutputStream dos = s.getOutputStream();
		InputStream dis = s.getInputStream();

		DSI_Packet dp = new DSI_Packet(DSI_REQUEST, CMD_GET_STATUS, reqID++, new byte[] { 0, 0 });
		clientSendRecv(dis, dos, dp);
		AFP_ServerInfo si = new AFP_ServerInfo(dp.getReader());
		System.out.println("srvinfo=("+si+")");
		s.close();

		s = new Socket(host, port);
		dos = s.getOutputStream();
		dis = s.getInputStream();

		dp = new DSI_Packet(DSI_REQUEST, CMD_OPEN_SESSION, reqID++, new byte[] { 1, 4, 0, 0, 4, 0 });
		clientSendRecv(dis, dos, dp);

		ByteWriter ww = new ByteWriter(128);
		ww.writeByte(CMD_LOGIN_EXT);
		ww.writeByte(0);
		ww.writeShort(1);
		ww.writePString("AFP3.1");
		ww.writePString("Cleartxt passwrd");
		ww.writeByte(3);
		ww.writeAFPString("stewart");
		ww.writeByte(3);
		ww.writeAFPString("");
		dp = new DSI_Packet(DSI_REQUEST, CMD_COMMAND, reqID++, ww.toByteArray());
		clientSendRecv(dis, dos, dp);
		
		s.close();
	}

	private final static void proxy(int lport, String host, int hport)
		throws Exception
	{
		final ServerSocket ss = new ServerSocket(lport);
		final Object lock = new Object();
		while (true)
		{
			final Socket client = ss.accept();
			final Socket server = new Socket(host, hport);
			final int ssnid = client.getPort();
			synchronized (lock) {
				System.out.println(ssnid+"] *** new connection *** "+client.getInetAddress()+" ***");
			}

			final OutputStream rcos = client.getOutputStream();
			final InputStream rcis = client.getInputStream();
			final OutputStream rsos = server.getOutputStream();
			final InputStream rsis = server.getInputStream();

			try
			{
				new Thread() { public void run() { try {
				while (true)
				{
					DSI_Packet cr = new DSI_Packet(rcis);
					synchronized (lock) {
						System.out.println(ssnid+"] -------------------------------------------------");
						System.out.println("<< client "+cr);
						cr.dumpRecvPayload(" < ");
					}
					cr.write(rsos);
				}
				} catch (Exception ex) {
					ex.printStackTrace();
					try { client.close(); } catch (Exception xx) { }
					try { server.close(); } catch (Exception xx) { }
					try { rcis.close(); } catch (Exception xx) { }
					try { rcos.close(); } catch (Exception xx) { }
					try { rsis.close(); } catch (Exception xx) { }
					try { rsos.close(); } catch (Exception xx) { }
					synchronized (lock) { System.out.println(ssnid+"] +++ terminated client->server +++"); }
				}
					synchronized (lock) { System.out.println(ssnid+"] *** client->server loop ended ***"); }
				} }.start();

				new Thread() { public void run() { try {
				while (true)
				{
					DSI_Packet sr = new DSI_Packet(rsis);
					synchronized (lock) {
						System.out.println(ssnid+"] -------------------------------------------------");
						System.out.println(">> server "+sr);
						sr.dumpSendPayload(" > ");
					}
					sr.write(rcos);
				}
				} catch (Exception ex) {
					ex.printStackTrace();
					try { client.close(); } catch (Exception xx) { }
					try { server.close(); } catch (Exception xx) { }
					try { rcis.close(); } catch (Exception xx) { }
					try { rcos.close(); } catch (Exception xx) { }
					try { rsis.close(); } catch (Exception xx) { }
					try { rsos.close(); } catch (Exception xx) { }
					synchronized (lock) { System.out.println(ssnid+"] +++ terminated server->client +++"); }
				}
					synchronized (lock) { System.out.println(ssnid+"] *** server->client loop ended ***"); }
				} }.start();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	private final static void clientSendRecv(InputStream i, OutputStream o, DSI_Packet dp)
		throws IOException
	{
		dp.write(o);
		System.out.println("# -----------------------------------------------");
		System.out.println("send=("+dp+")");
		dp.dumpSendPayload("> ");
		dp.read(i);
		System.out.println("recv=("+dp+")");
		dp.dumpRecvPayload("< ");
	}

	// config helper class

	static class Config extends Properties
	{
		private Config global;

		Config(Config global)
		{
			this.global = global;
		}

		public boolean has(String key)
		{
			return value(key) != null;
		}

		public Object value(Object key)
		{
			Object val = get(key);
			if (val == null && global != null)
			{
				val = global.value(key);
			}
			return val;
		}

		public void append(Object key, Object value)
		{
			Config cfg = configValue(key);
			if (cfg == null)
			{
				cfg = new Config(this);
				put(key, cfg);
			}
			cfg.put(value, "");
		}

		public Config configValue(Object key)
		{
			return (Config)value(key);
		}

		public String stringValue(String key)
		{
			return (String)value(key);
		}

		public int intValue(String key, int def)
		{
			try
			{
				return has(key) ? Integer.parseInt(stringValue(key)) : def;
			}
			catch (Exception ex)
			{
				return def;
			}
		}
	}

}

