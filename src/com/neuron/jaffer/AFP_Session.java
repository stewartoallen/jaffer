/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Stack;

/*
 * The session server is currently comprised of a listener thread,
 * a command-handler thread and (soon) a sender thread. This should be
 * re-written (or abstracted) at some point to take advantage of Java 1.4
 * NIO facilities.
 * 
 * TODO: session: nio, locking
 * TODO: osvolume: eliminate most file caching. solve dir id's w/ id db.
 * TODO: open/close dir deprecated. perhaps remove support.
 * TODO: formalize debugging. log to file, etc.
 * TODO: utf8 file names and file name munging (long/short/samba/unix)
 */

final class AFP_Session extends Utility implements AFP_Constants
{
	private final static boolean printOnlyUnknown = System.getProperty("debug.pou") != null;
	private final static boolean nothreads = System.getProperty("nothreads") != null;
	private final static Random random = new Random();;
	private final static BigInteger bigOne = new BigInteger("1");
	private final static BigInteger bigMask = new BigInteger("ffffffffffffffff", 16);
	private final static BigInteger serverPrivate = new BigInteger(256, random);
	private final static BigInteger serverPublic = DHX_G.modPow(serverPrivate, DHX_P);
	private final static byte[] DHX_Encode = "CJalbert".getBytes();
	private final static byte[] DHX_Decode = "LWallace".getBytes();

	private final static int MODE_OLD  = 0;
	private final static int MODE_EXT  = 1;
	private final static int MODE_NONE = 2;

	private AFP_Server server;
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	private Thread recvThread;
	private Thread cmmdThread;
	private Thread sendThread;
	private int maxAttnQuantum;
	private int nextReqID = 0x1;
	private int nextForkID = 0x1;
	private int maxPacket = 0x8000;
	private Hashtable openForks;
	private CommandQueue cmmdQueue;
	private CommandQueue sendQueue;
	private boolean running;
	private boolean validated;
	private String userName;
	private Stack packets;
	// for UAMs
	private long randNum;
	private int loginType;
	private BigInteger nonce;
	private BigInteger sessionKey;
	private BigInteger clientPublic;

	// ----------------------------------------------------------------------------------------

	AFP_Session(AFP_Server server, Socket socket)
	{
		this.server = server;
		this.socket = socket;
		this.openForks = new Hashtable();
		this.cmmdQueue = new CommandLoop();
		this.sendQueue = new SendLoop();
		this.packets = new Stack();
	}

	public synchronized void start()
	{
		if (running)
		{
			return;
		}
		recvThread = new Thread(new ReceiveLoop(), "AFP Session ["+getSessionID()+"] Receiver");
		cmmdThread = new Thread(cmmdQueue, "AFP Session ["+getSessionID()+"] Command Dispatch");
		sendThread = new Thread(sendQueue, "AFP Session ["+getSessionID()+"] Sender");
		recvThread.start();
		cmmdThread.start();
		sendThread.start();
		running = true;
	}

	private void print(String msg)
	{
		if (server.DEBUG_PRINT)
		{
			System.out.println(msg);
		}
	}

	private void debug(String msg)
	{
		if (server.DEBUG_DEBUG)
		{
			print("-- "+msg);
		}
	}

	private void error(boolean value, String msg)
		throws Exception
	{
		if (value)
		{
			throw new Exception(msg);
		}
	}

	private void printPacket(DSI_Packet dp)
	{
		synchronized (socket)
		{
			if (dp.isRequest())
			{
				if (server.DEBUG_DSI_LINE)
				{
					print("---["+getSessionID()+"]-----------------------------------------------------------");
				}
				if (server.DEBUG_DSI)
				{
					print("<< client "+dp);
				}
				if (server.DEBUG_DSI_REQUEST)
				{
					dp.dumpRecvPayload(" < ");
				}
			}
			else
			{
				if (server.DEBUG_DSI)
				{
					print(">> server "+dp);
				}
				if (server.DEBUG_DSI_REPLY)
				{
					dp.dumpSendPayload(" > ");
				}
			}
		}
	}

	private DSI_Packet recvPacket()
		throws IOException
	{
		DSI_Packet dp = null;
		synchronized (packets)
		{
			if (packets.empty())
			{
				dp = new DSI_Packet(maxPacket);
			}
			else
			{
				dp = (DSI_Packet)packets.pop();
			}
		}
		dp.reset();
		synchronized (input)
		{
			dp.read(input);
		}
		return dp;
	}

	private void sendPacket(DSI_Packet dp)
		throws IOException
	{
		synchronized (output)
		{
			dp.write(output);
		}
		// do not reuse packets that are too small
		if (dp.getBufferSize() >= maxPacket)
		{
			synchronized (packets)
			{
				packets.push(dp);
			}
		}
		if (!printOnlyUnknown)
		{
			printPacket(dp);
		}
	}

	private int getSessionID()
	{
		return socket.getPort();
	}

	private int nextRequestID()
	{
		nextReqID++;
		if (nextReqID > 0xffff)
		{
			nextReqID = 0;
		}
		return nextReqID;
	}

	private int nextForkID()
	{
		nextForkID++;
		if (nextForkID > 0xffff)
		{
			nextForkID = 0;
		}
		return nextForkID;
	}

	private synchronized void terminateSession()
	{
		if (!running)
		{
			return;
		}
		//debug("["+getSessionID()+"] Session Terminating");
		print("session ["+getSessionID()+"] terminating");
		recvThread.interrupt();
		cmmdThread.interrupt();
		synchronized (cmmdThread)
		{
			cmmdThread.notify();
		}
		sendThread.interrupt();
		synchronized (sendThread)
		{
			sendThread.notify();
		}
		try
		{
			socket.getInputStream().close();
			socket.getOutputStream().close();
			socket.close();
		}
		catch (Exception ex)
		{
			//ex.printStackTrace();
		}
		running = false;
	}

	// ----------------------------------------------------------------------------------------

	private abstract class CommandQueue extends Queue implements Runnable
	{
		public void run()
		{
			while (true)
			{
				DSI_Packet dp = (DSI_Packet)dequeue();
				if (dp == null)
				{
					break;
				}
				try
				{
					handleCommand(dp);
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
					break;
				}
			}
			//debug("["+getSessionID()+"] Loop ("+getClass()+") Terminated");
			terminateSession();
		}

		public abstract void handleCommand(DSI_Packet dp)
			throws IOException
			;
	}

	// ----------------------------------------------------------------------------------------

	private class ReceiveLoop implements Runnable
	{
		public void run()
		{
			try
			{
				input = socket.getInputStream();
				output = socket.getOutputStream();
				print("session ["+getSessionID()+"] started");
				boolean valid = true;
				while (valid)
				{
					DSI_Packet dp = recvPacket();
					if (!printOnlyUnknown)
					{
						printPacket(dp);
					}
					if (dp.isReply())
					{
						continue;
					}
					switch (dp.getCommand())
					{
						case DSI_Constants.CMD_GET_STATUS:
							dp.setReply();
							server.getServerInfo().write(dp.getWriter());
							//dp.getWriter().writeBytes(server.getServerInfo().encode());
							sendPacket(dp);
							break;
						case DSI_Constants.CMD_OPEN_SESSION:
							ByteReader rr = dp.getReader();
							int cmd = rr.readUnsignedByte();
							int opt = rr.readUnsignedByte();
							int qnt = rr.readInt();
							switch (cmd)
							{
								// sent by a client
								case DSI_Constants.OPT_ATTN_QUANT:
									error(opt != 4, "Option length != 4");
									maxAttnQuantum = qnt;
									dp.setReply();
									ByteWriter ww = dp.getWriter();
									ww.writeByte(DSI_Constants.OPT_SERV_QUANT);
									ww.writeByte(4);        // length
									ww.writeInt(maxPacket); // server quantum (max client request)
									sendPacket(dp);
									break;
								// sent by a server
								case DSI_Constants.OPT_SERV_QUANT:
									print("We are not a client");
									terminateSession();
									break;
								default:
									print("Unknown type: "+hex(cmd));
									terminateSession();
									break;
							}
							break;
						case DSI_Constants.CMD_WRITE:
						case DSI_Constants.CMD_COMMAND:
							if (nothreads)
							{
								cmmdQueue.handleCommand(dp);
							}
							else
							{
								cmmdQueue.enqueue(dp);
							}
							break;
						case DSI_Constants.CMD_CLOSE_SESSION:
							valid = false;
							break;
						case DSI_Constants.CMD_ATTENTION:
							// we are not a client
							break;
						case DSI_Constants.CMD_TICKLE:
							dp.setRequestID(nextRequestID());
							sendPacket(dp);
							break;
						default:
							print("!!!!! Invalid DSI command: "+hex(dp.getCommand())+" !!!!!");
							dp.setReply();
							sendPacket(dp);
							return;
					}
				}
			}
			catch (Exception ex)
			{
				print("session ["+getSessionID()+"] error '"+ex+"'");
				//ex.printStackTrace();
			}
			//print("** ["+getSessionID()+"] Receive Loop Exited");
			terminateSession();
		}
	}

	// ----------------------------------------------------------------------------------------

	private class CommandLoop extends CommandQueue
	{
		public void handleCommand(DSI_Packet dp)
			throws IOException
		{
			ByteReader rr = dp.getReader();
			ByteWriter ww = dp.getWriter();
			int err = ERR_NO_ERR;
			int cmd = rr.readUnsignedByte();
			try
			{
				// auth pre-check check
				switch (cmd)
				{
					case CMD_LOGIN:
					case CMD_LOGIN_EXT:
					case CMD_LOGIN_CONT:
						break;
					default:
						if (!validated)
						{
							throw new AFP_Error(ERR_PARAM_ERR);
						}
						break;
				}
				// command dispatch
				switch (cmd)
				{
					case CMD_LOGIN:               err = cmdLogin(rr, ww);            break;
					case CMD_LOGIN_EXT:           err = cmdLoginExt(rr, ww);         break;
					case CMD_LOGIN_CONT:          err = cmdLoginCont(rr, ww);        break;
					case CMD_LOGOUT:              err = cmdLogout(rr, ww);           break;
					case CMD_MAP_ID:              err = cmdMapID(rr, ww);            break;
					case CMD_CREATE_DIR:          err = cmdCreateDir(rr, ww);        break;
					case CMD_CREATE_FILE:         err = cmdCreateFile(rr, ww);       break;
					case CMD_CREATE_ID:           err = ERR_PARAM_ERR;               break; // TODO
					case CMD_GET_USER_INFO:       err = cmdGetUserInfo(rr, ww);      break;
					case CMD_GET_SRVR_PARMS:      err = cmdGetServerParams(rr, ww);  break;
					case CMD_GET_VOL_PARMS:       err = cmdGetVolumeParams(rr, ww);  break;
					case CMD_GET_FORK_PARMS:      err = cmdGetForkParams(rr, ww);    break; // TODO
					case CMD_GET_FILE_DIR_PARMS:  err = cmdGetFileDirParams(rr, ww); break;
					case CMD_GET_SESSION_TOKEN:   err = cmdGetSessionToken(rr, ww);  break;
					case CMD_SET_VOL_PARMS:       err = ERR_NO_ERR;                  break; // TODO
					case CMD_SET_DIR_PARMS:       err = cmdSetDirParams(rr, ww);     break;
					case CMD_SET_FORK_PARMS:      err = cmdSetForkParams(rr, ww);    break;
					case CMD_SET_FILE_PARMS:      err = cmdSetFileParams(rr, ww);    break;
					case CMD_SET_FILE_DIR_PARMS:  err = cmdSetFileDirParams(rr, ww); break;
					case CMD_OPEN_DT:             err = ERR_MISC_ERR;                break; // TODO
					case CMD_OPEN_VOL:            err = cmdOpenVolume(rr, ww);       break;
					case CMD_OPEN_DIR:            err = cmdOpenDir(rr, ww);          break;
					case CMD_OPEN_FORK:           err = cmdOpenFork(rr, ww);         break;
					case CMD_BYTE_RANGE_LOCK:     err = cmdByteRangeLock(rr, ww);    break;
					case CMD_BYTE_RANGE_LOCK_EXT: err = cmdByteRangeLockExt(rr, ww); break;
					case CMD_ENUMERATE:           err = cmdEnumerate(rr, ww);        break;
					case CMD_ENUMERATE_EXT2:      err = cmdEnumerateExt2(rr, ww);    break;
					case CMD_READ:                err = cmdRead(rr, ww);             break;
					case CMD_READ_EXT:            err = cmdReadExt(rr, ww);          break;
					case CMD_WRITE:               err = cmdWrite(rr, ww);            break;
					case CMD_WRITE_EXT:           err = cmdWriteExt(rr, ww);         break;
					case CMD_FLUSH_FORK:          err = cmdFlushFork(rr, ww);        break;
					case CMD_CLOSE_VOL:           err = cmdCloseVolume(rr, ww);      break;
					case CMD_CLOSE_DIR:           err = cmdCloseDir(rr, ww);         break;
					case CMD_CLOSE_FORK:          err = cmdCloseFork(rr, ww);        break;
					case CMD_MOVE_AND_RENAME:     err = cmdMoveAndRename(rr, ww);    break;
					case CMD_RENAME:              err = cmdRename(rr, ww);           break;
					case CMD_DELETE:              err = cmdDelete(rr, ww);           break;
					default:
						print("!!!!! Unhandled AFP command: "+hex(cmd)+" !!!!!");
						if (printOnlyUnknown)
						{
							printPacket(dp);
						}
						err = ERR_CALL_NOT_SUPPORTED;
						break;
				}
			}
			catch (AFP_Error ae)
			{
				err = ae.getError();
				print("caught: "+ae);
				//ae.printStackTrace();
			}
			catch (IOException ex)
			{
				err = ERR_MISC_ERR;
				ex.printStackTrace();
			}
			catch (Exception ex)
			{
				// TODO: we should perhaps shut down the session here
				err = ERR_MISC_ERR;
				ex.printStackTrace();
			}
			dp.setErrorCode(err);
			dp.setReply();
			if (nothreads)
			{
				sendPacket(dp);
			}
			else
			{
				sendQueue.enqueue(dp);
			}
		}
	}

	// ----------------------------------------------------------------------------------------

	private class SendLoop extends CommandQueue
	{
		public void handleCommand(DSI_Packet dp)
			throws IOException
		{
			sendPacket(dp);
		}
	}

	// ----------------------------------------------------------------------------------------

	// CMD_LOGIN
	private int cmdLogin(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		//note: pad byte not being sent from OSX per spec
		String afp = rr.readPString();
		String uam = rr.readPString();
		debug("login afp="+afp+",uam="+uam+",ssn="+getSessionID());
		return loginCommon(uam, null, rr, ww);
	}

	// CMD_LOGIN_EXT
	private int cmdLoginExt(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int flags = rr.readUnsignedShort();
		String afp = rr.readPString();
		String uam = rr.readPString();
		String usName = rr.readTypedString();
		String paName = rr.readTypedString();
		rr.skipBytes(rr.getPosition()%2);
		debug("loginext afp="+afp+",uam="+uam+",user="+usName+",path="+paName+",ssn="+getSessionID());
		return loginCommon(uam, usName, rr, ww);
	}

	// login common helper method
	private int loginCommon(String uam, String user, ByteReader rr, ByteWriter ww)
		throws IOException
	{
		ww.writeShort(getSessionID());
		loginType = getUAM(uam);
		switch (loginType)
		{
			case UAM_GUEST:
				user = server.getGuestUser();
				if (user != null && server.hasUser(user))
				{
					userName = user;
					validated = true;
				}
				else
				{
					return ERR_USER_NOT_AUTH;
				}
				break;
			case UAM_CLEARTEXT:
				if (user == null)
				{
					user = rr.readPString();
				}
				rr.skip(rr.getPosition() % 2);
				String pass = rr.readCString(8);
				if (!server.checkPassword(user, pass))
				{
					return ERR_USER_NOT_AUTH;
				}
				userName = user;
				server.setThreadOwner(userName);
				validated = true;
				break;
			case UAM_RANDOM_NUM1:
				if (!server.hasCleartextPasswords())
				{
					return ERR_BAD_UAM;
				}
				if (user == null)
				{
					user = rr.readPString();
				}
				if (!server.hasUser(user))
				{
					return ERR_USER_NOT_AUTH;
				}
				userName = user;
				randNum = random.nextLong();
				ww.writeLong(randNum);
				return ERR_AUTH_CONTINUE;
			case UAM_DHX_128:
				if (user == null)
				{
					user = rr.readPString();
				}
				if (!server.hasUser(user))
				{
					return ERR_USER_NOT_AUTH;
				}
				rr.skip(rr.getPosition() % 2);
				clientPublic = new BigInteger(1, rr.readBytes(16));
				sessionKey = clientPublic.modPow(serverPrivate, DHX_P);
				nonce = new BigInteger(128, random).abs();
				byte inbuf[] = new byte[32];
				byte outbuf[] = new byte[32];
				System.arraycopy(keyBytes(nonce),0,inbuf,0,16);
				new CAST128(keyBytes(sessionKey)).encrypt(inbuf, 0, outbuf, 0, 32, DHX_Encode);
				ww.writeBytes(keyBytes(serverPublic));
				ww.writeBytes(outbuf);
				userName = user;
				return ERR_AUTH_CONTINUE;
			case UAM_UNKNOWN:
			default:
				return ERR_BAD_UAM;
		}
		return ERR_NO_ERR;
	}

	// CMD_LOGIN_CONT
	private int cmdLoginCont(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int context = rr.readUnsignedShort();
		debug("logincont context="+hex(context));
		switch (loginType)
		{
			case UAM_RANDOM_NUM1:
				if (!server.hasCleartextPasswords())
				{
					return ERR_BAD_UAM;
				}
				byte[] cryptRand = rr.readBytes(8);
				debug("crypted random = "+hex(cryptRand));
				String pass = server.getPassword(userName);
				if (pass != null)
				{
					byte pb[] = new byte[8];
					System.arraycopy(pass.getBytes(), 0, pb, 0, pass.length());
					new DES(pb).decrypt(cryptRand);
					if (readInt8(cryptRand, 0) != randNum)
					{
						return ERR_USER_NOT_AUTH;
					}
				}
				server.setThreadOwner(userName);
				validated = true;
				break;
			case UAM_DHX_128:
				byte inbuf[] = new byte[16+64];
				byte outbuf[] = new byte[16+64];
				rr.readBytes(inbuf);
				CAST128 c = new CAST128(keyBytes(sessionKey));
				c.decrypt(inbuf, 0, outbuf, 0, 32, DHX_Decode);
				byte nonceb[] = new byte[16];
				System.arraycopy(outbuf, 0, nonceb, 0, 16);
				BigInteger noncep1 = new BigInteger(1, nonceb);
				//TODO: it appears that after the first auth, only the
				// last 8 bytes are incremented by the client. thus the 'and'
				if (!noncep1.subtract(nonce).and(bigMask).equals(bigOne))
				{
					return ERR_PARAM_ERR;
				}
				pass = readCString(outbuf, 16);
				if (!server.checkPassword(userName, pass))
				{
					return ERR_USER_NOT_AUTH;
				}
				server.setThreadOwner(userName);
				validated = true;
				break;
			case UAM_UNKNOWN:
			case UAM_GUEST:
			case UAM_CLEARTEXT:
			default:
				return ERR_BAD_UAM;
		}
		return ERR_NO_ERR;
	}

	// CMD_LOGOUT
	private int cmdLogout(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		return ERR_NO_ERR;
	}

	// CMD_GET_USER_INFO
	private int cmdGetUserInfo(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		int thisUser = rr.readUnsignedByte();
		int uid = rr.readInt();
		int flags = rr.readUnsignedShort(); // bits: 0=user, 1=group
		debug("getuserinfo this="+thisUser+",uid="+uid+",flags="+hex(flags));
		ww.writeShort(flags);
		if (hasBits(flags, 0x1)) { ww.writeInt(0x00000000); } // user = guest
		if (hasBits(flags, 0x2)) { ww.writeInt(0x00000000); } // group = guest
		return ERR_NO_ERR;
	}

	// CMD_GET_SRVR_PARMS
	private int cmdGetServerParams(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		AFP_Volume v[] = server.getVolumes();
		ww.writeInt(unix2afpTime(System.currentTimeMillis()));
		ww.writeByte(v.length); 
		for (int i=0; i<v.length; i++)
		{
			ww.writeByte( ((v[i].hasUnixPrivs() ? 0x1 : 0) | (v[i].getPassword() != null ? 0x80 : 0)) );
			ww.writePString(v[i].getName());
		}
		return ERR_NO_ERR;
	}

	// CMD_OPEN_VOL
	private int cmdOpenVolume(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int flags = rr.readUnsignedShort();
		String volName = rr.readPString();
		/* required pad out to even bytes */
		if (volName.length() % 2 == 0) 
		{
			rr.skip(1);
		}
		String volPass = rr.readCString(8);
		debug("openvol name=("+volName+") pass=("+volPass+")");
		AFP_Volume vol = server.getVolume(volName);
		if (vol == null)
		{
			return ERR_OBJECT_NOT_FOUND;
		}
		String passCheck = vol.getPassword();
		if (passCheck != null && (volPass == null || !passCheck.equals(volPass)))
		{
			return ERR_ACCESS_DENIED;
		}
		sendVolumeInfo(ww, vol, flags);
		return ERR_NO_ERR;
	}

	// CMD_DELETE
	private int cmdDelete(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		String pathName = rr.readTypedString();
		debug("delete vol="+volID+",dir="+dirID+",path="+pathName);
		AFP_CNode node = openPath(volID, dirID, pathName).getNode();
		if (node.isDirectory() && node.getChildren().hasNext())
		{
			return ERR_DIR_NOT_EMPTY;
		}
		if (node.delete())
		{
			return ERR_NO_ERR;
		}
		else
		{
			return ERR_ACCESS_DENIED;
		}
	}

	// CMD_GET_FILE_DIR_PARMS
	private int cmdGetFileDirParams(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		int fileFlags = rr.readUnsignedShort();
		int dirFlags = rr.readUnsignedShort();
		String pathName = rr.readTypedString();
		debug("getparm vol="+volID+",dir="+dirID+",ff="+hex(fileFlags)+",df="+hex(dirFlags)+",path="+pathName);
		AFP_CNode node = openPath(volID, dirID, pathName).getNode();
		ww.writeShort(fileFlags);
		ww.writeShort(dirFlags);
		if (node.isDirectory())
		{
			sendDirectoryInfo(ww, node, dirFlags, MODE_EXT);
		}
		else
		{
			sendFileInfo(ww, node, fileFlags, MODE_EXT);
		}
		return ERR_NO_ERR;
	}

	// CMD_GET_FORK_PARMS (AFP2.x only)
	private int cmdGetForkParams(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int forkRef = rr.readUnsignedShort();
		int flags = rr.readUnsignedShort();
		debug("getforkparms fork="+forkRef+",flags="+hex(flags));
		// TODO -- validate fork type and make sure other flags aren't set.
		ww.writeShort(flags);
		ww.writeInt((int)Math.min(getFork(forkRef).getLength(), MAX_SHORT_FORK_LEN));
		return ERR_NO_ERR;
	}

	// CMD_SET_FORK_PARMS (AFP2.x only)
	private int cmdSetForkParams(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int forkRef = rr.readUnsignedShort();
		int flags = rr.readUnsignedShort();
		int length = rr.readInt();
		debug("setforkparms fork="+forkRef+",flags="+hex(flags)+",len="+hex(length));
		// TODO -- validate fork type and make sure other flags aren't set.
		getFork(forkRef).setLength(length);
		return ERR_NO_ERR;
	}

	// CMD_SET_FILE_PARMS
	private int cmdSetFileParams(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		int fileFlags = rr.readUnsignedShort();
		String pathName = rr.readTypedString();
		debug("setfileparm vol="+volID+",dir="+dirID+",ff="+hex(fileFlags)+",path="+pathName);
		AFP_CNode node = openPath(volID, dirID, pathName).getNode();
		recvFileInfo(rr, node, fileFlags);
		return ERR_NO_ERR;
	}

	// CMD_SET_DIR_PARMS
	private int cmdSetDirParams(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		int dirFlags = rr.readUnsignedShort();
		String pathName = rr.readTypedString();
		debug("setdirparm vol="+volID+",dir="+dirID+",df="+hex(dirFlags)+",path="+pathName);
		AFP_CNode node = openPath(volID, dirID, pathName).getNode();
		recvDirectoryInfo(rr, node, dirFlags);
		return ERR_NO_ERR;
	}

	// CMD_SET_FILE_DIR_PARMS
	private int cmdSetFileDirParams(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		int flags = rr.readUnsignedShort();
		String pathName = rr.readTypedString();
		debug("setfiledirparm vol="+volID+",dir="+dirID+",df="+hex(flags)+",path="+pathName);
		AFP_CNode node = openPath(volID, dirID, pathName).getNode();
		if (node.isDirectory())
		{
			recvDirectoryInfo(rr, node, flags);
		}
		else
		{
			recvFileInfo(rr, node, flags);
		}
		return ERR_NO_ERR;
	}

	// CMD_GET_VOL_PARMS
	private int cmdGetVolumeParams(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int flags = rr.readUnsignedShort();
		AFP_Volume vol = server.getVolume(volID);
		if (vol == null)
		{
			return ERR_PARAM_ERR;
		}
		sendVolumeInfo(ww, vol, flags);
		return ERR_NO_ERR;
	}

	// CMD_GET_SESSION_TOKEN
	private int cmdGetSessionToken(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int type = rr.readUnsignedShort();
		int dlen = rr.readInt();
		ww.writeShort(type);
		ww.writeInt(4);
		ww.writeInt(0x77665544);
		return ERR_NO_ERR;
	}

	// CMD_CLOSE_VOL
	private int cmdCloseVolume(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		return ERR_NO_ERR;
	}

	// CMD_MAP_ID
	private int cmdMapID(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int userID = rr.readInt();
		ww.writePString("doodleman");
		return ERR_NO_ERR;
	}

	// CMD_CREATE_DIR
	private int cmdCreateDir(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		String pathName = rr.readTypedString();
		debug("createdir vol="+volID+",dir="+dirID+",path="+pathName);
		ww.writeInt(createDirPath(volID, dirID, pathName).getNode().getNodeID());
		return ERR_NO_ERR;
	}

	// CMD_CREATE_FILE
	private int cmdCreateFile(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		String pathName = rr.readTypedString();
		debug("createfile vol="+volID+",dir="+dirID+",path="+pathName);
		createFilePath(volID, dirID, pathName);
		return ERR_NO_ERR;
	}

	// CMD_OPEN_DIR
	private int cmdOpenDir(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		String pathName = rr.readTypedString();
		debug("opendir vol="+hex(volID)+",dir="+hex(dirID)+",path="+pathName);
		AFP_CNode node = openPath(volID, dirID, pathName).open().getNode();
		if (!node.isDirectory())
		{
			return ERR_OBJECT_TYPE_ERR;
		}
		ww.writeInt(node.getNodeID());
		return ERR_NO_ERR;
	}

	// CMD_OPEN_FORK
	private int cmdOpenFork(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		int which = rr.readUnsignedByte();  // bits: 7=fork (0=file, 1=resource)
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		int flags = rr.readUnsignedShort(); // bits: getFileParams bits
		int mode = rr.readUnsignedShort();  // bits: 0=read, 1=write, 4=readlock, 5=writelock
		String pathName = rr.readTypedString();
		debug("openfork flags="+hex(flags)+",which="+hex(which)+",mode="+hex(mode)+",path="+pathName);
		AFP_CNode node = openPath(volID, dirID, pathName).getNode();
		if (node.isDirectory())
		{
			return ERR_OBJECT_TYPE_ERR;
		}
		int fid = nextForkID();
		AFP_Fork fork = hasBits(which,0x80) ? node.openResourceFork(mode) : node.openFileFork(mode);
		if (fork == null)
		{
			return ERR_ACCESS_DENIED;
		}
		debug("openfork ref="+hex(fid));
		openForks.put(new Integer(fid), fork);
		ww.writeShort(flags);
		ww.writeShort(fid);
		sendFileInfo(ww, node, flags, MODE_NONE);
		return ERR_NO_ERR;
	}

	// CMD_RENAME
	private int cmdRename(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		String path = rr.readTypedString();
		String newName = rr.readTypedString();
		debug("rename vol="+volID+",dir="+dirID+",path="+path+",newName="+newName);
		Path dir = openPath(volID, dirID, path);
		if (!dir.getNode().moveTo(dir.getNode(), newName))
		{
			return ERR_ACCESS_DENIED;
		}
		return ERR_NO_ERR;
	}

	// CMD_MOVE_AND_RENAME
	private int cmdMoveAndRename(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int srcDirID = rr.readInt();
		int dstDirID = rr.readInt();
		String srcPath = rr.readTypedString();
		String dstPath = rr.readTypedString();
		String newName = rr.readTypedString();
		debug("move/rename vol="+volID+",srcDir="+srcDirID+",dstDir="+dstDirID+",srcPath="+srcPath+",dstPath="+dstPath+",name="+newName);
		Path src = openPath(volID, srcDirID, srcPath);
		Path dst = openPath(volID, dstDirID, dstPath);
		if (!src.getNode().moveTo(dst.getNode(), newName))
		{
			return ERR_ACCESS_DENIED;
		}
		return ERR_NO_ERR;
	}

	// CMD_BYTE_RANGE_LOCK
	private int cmdByteRangeLock(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		// bits: 0=mode (0=lock, 1=unlock)
		// bits: 7=relative to (0=from start, 1=from end)
		int flags = rr.readUnsignedByte();
		int forkRef = rr.readUnsignedShort();
		long offset = rr.readInt();
		long length = rr.readInt();
		debug("range lock fork="+hex(forkRef)+",flags="+hex(flags)+"off="+hex(offset)+",len="+hex(length));
		AFP_Fork fork = getFork(forkRef);
		if (hasBits(flags,0x1))
		{
			if (!fork.unlockRange(offset, length))
			{
				return ERR_RANGE_NOT_LOCKED;
			}
		}
		else
		{
			if (offset < 0)
			{
				offset = fork.getLength() - offset;
			}
			if (flags == 0x80)
			{
				offset += fork.getLength();
			}
			if (!fork.lockRange(offset, length))
			{
				return ERR_LOCK_ERR;
			}
		}
		ww.writeLong(offset);
		return ERR_NO_ERR;
	}

	// CMD_BYTE_RANGE_LOCK_EXT
	private int cmdByteRangeLockExt(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		// bits: 0=mode (0=lock, 1=unlock)
		// bits: 7=relative to (0=from start, 1=from end)
		int flags = rr.readUnsignedByte();
		int forkRef = rr.readUnsignedShort();
		long offset = rr.readLong();
		long length = rr.readLong();
		debug("range lockx fork="+hex(forkRef)+",flags="+hex(flags)+"off="+hex(offset)+",len="+hex(length));
		AFP_Fork fork = getFork(forkRef);
		if (hasBits(flags,0x1))
		{
			if (!fork.unlockRange(offset, length))
			{
				return ERR_RANGE_NOT_LOCKED;
			}
		}
		else
		{
			if (offset < 0)
			{
				offset = fork.getLength() - offset;
			}
			if (flags == 0x80)
			{
				offset += fork.getLength();
			}
			if (!fork.lockRange(offset, length))
			{
				return ERR_LOCK_ERR;
			}
		}
		ww.writeLong(offset);
		return ERR_NO_ERR;
	}

	// CMD_READ
	private int cmdRead(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int forkRef = rr.readUnsignedShort();
		long offset = rr.readInt();
		long length = rr.readInt();
		int nlMask = rr.readUnsignedByte();
		int nlChar = rr.readUnsignedByte();
		debug("read fork="+hex(forkRef)+",off="+hex(offset)+",len="+hex(length)+",nlm="+hex(nlMask)+",nlc="+hex(nlChar));
		getFork(forkRef).readRange(offset, length, ww);
		return ERR_NO_ERR;
	}

	// CMD_READ_EXT
	private int cmdReadExt(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int forkRef = rr.readUnsignedShort();
		long offset = rr.readLong();
		long length = rr.readLong();
		debug("readx fork="+hex(forkRef)+",off="+hex(offset)+",len="+hex(length));
		getFork(forkRef).readRange(offset, length, ww);
		return ERR_NO_ERR;
	}

	// CMD_WRITE
	private int cmdWrite(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		int flag = rr.readUnsignedByte(); // bits: 7=relative to (0=from start, 1=from end)
		int forkRef = rr.readUnsignedShort();
		long offset = rr.readInt();
		long length = rr.readInt();
		debug("write fork="+hex(forkRef)+",off="+hex(offset)+",len="+hex(length));
		AFP_Fork fork = getFork(forkRef);
		if (offset < 0)
		{
			offset = fork.getLength() - offset;
		}
		if (flag == 0x80)
		{
			offset += fork.getLength();
		}
		long wrote = fork.writeRange(offset, length, rr);
		ww.writeInt((int)(offset + wrote));
		return ERR_NO_ERR;
	}

	// CMD_WRITE_EXT
	private int cmdWriteExt(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		int flag = rr.readUnsignedByte(); // bits: 7=relative to (0=from start, 1=from end)
		int forkRef = rr.readUnsignedShort();
		long offset = rr.readLong();
		long length = rr.readLong();
		debug("writex fork="+hex(forkRef)+",off="+hex(offset)+",len="+hex(length));
		AFP_Fork fork = getFork(forkRef);
		if (offset < 0)
		{
			offset = fork.getLength() - offset;
		}
		if (flag == 0x80)
		{
			offset += fork.getLength();
		}
		long wrote = fork.writeRange(offset, length, rr);
		ww.writeLong(offset + wrote);
		return ERR_NO_ERR;
	}

	// CMD_FLUSH_FORK
	private int cmdFlushFork(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int forkRef = rr.readUnsignedShort();
		debug("flush fork="+hex(forkRef));
		getFork(forkRef).flush();
		return ERR_NO_ERR;
	}

	// CMD_CLOSE_DIR
	private int cmdCloseDir(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		debug("closedir vol="+hex(volID)+",dir="+hex(dirID));
		return ERR_NO_ERR;
	}

	// CMD_CLOSE_FORK
	private int cmdCloseFork(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int forkRef = rr.readUnsignedShort();
		AFP_Fork fork = getFork(forkRef);
		openForks.remove(new Integer(forkRef));
		debug("closefork ref="+hex(forkRef)+" fork="+fork);
		fork.close();
		return ERR_NO_ERR;
	}

	// TODO: uses too many byte writers. optimize byte writers and re-use.
	// CMD_ENUMERATE
	private int cmdEnumerate(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		int fileFlags = rr.readUnsignedShort();
		int dirFlags = rr.readUnsignedShort();
		int maxRecords = rr.readUnsignedShort();
		int startIndex = (rr.readUnsignedShort() - 1);
		int maxReply = rr.readUnsignedShort();
		String pathName = rr.readTypedString();

		debug("enum vol="+volID+",dir="+dirID+",ff="+hex(fileFlags)+",df="+hex(dirFlags)+",xrec="+hex(maxRecords)+",idx="+hex(startIndex)+",xrep="+hex(maxReply)+",path="+pathName);

		AFP_CNode node = openPath(volID, dirID, pathName).getNode();
		ww.writeShort(fileFlags);
		ww.writeShort(dirFlags);
		Iterator<AFP_CNode> en = node.getChildren();
		if (en == null)
		{
			return ERR_MISC_ERR;
		}
		if (!en.hasNext())
		{
			return ERR_OBJECT_NOT_FOUND;
		}
		int sent = 0;
		ByteWriter w3 = new ByteWriter(maxReply);
		for (int i=0; sent < maxRecords && en.hasNext(); i++)
		{
			AFP_CNode next = en.next();
			if (i < startIndex)
			{
				if (!en.hasNext())
				{
					return ERR_OBJECT_NOT_FOUND;
				}
				continue;
			}
			ByteWriter w2 = new ByteWriter(128);
			if (next.isDirectory())
			{
				sendDirectoryInfo(w2, next, dirFlags, MODE_OLD);
			}
			else
			{
				sendFileInfo(w2, next, fileFlags, MODE_OLD);
			}
			byte data[] = w2.toByteArray();
			if (data.length % 2 == 0)
			{
				w3.writeByte(data.length+2);
				w3.writeBytes(data);
				w3.writeByte(0);
			}
			else
			{
				w3.writeByte(data.length+1);
				w3.writeBytes(data);
			}
			sent++;
			if (w3.getSize() > maxReply - 128)
			{
				break;
			}
		}

		ww.writeShort(sent);
		ww.writeBytes(w3.toByteArray());
		return ERR_NO_ERR;
	}

	// TODO: uses too many byte writers. optimize byte writers and re-use.
	// CMD_ENUMERATE_EXT2
	private int cmdEnumerateExt2(ByteReader rr, ByteWriter ww)
		throws IOException
	{
		rr.skip(1);
		int volID = rr.readUnsignedShort();
		int dirID = rr.readInt();
		int fileFlags = rr.readUnsignedShort();
		int dirFlags = rr.readUnsignedShort();
		int maxRecords = rr.readUnsignedShort();
		int startIndex = (rr.readInt() - 1);
		int maxReply = rr.readInt();
		String pathName = rr.readTypedString();
		debug("enum vol="+volID+",dir="+dirID+",ff="+hex(fileFlags)+",df="+hex(dirFlags)+",xrec="+hex(maxRecords)+",idx="+hex(startIndex)+",xrep="+hex(maxReply)+",path="+pathName);
		AFP_CNode node = openPath(volID, dirID, pathName).getNode();
		ww.writeShort(fileFlags);
		ww.writeShort(dirFlags);
		Iterator<AFP_CNode> en = node.getChildren();
		if (en == null)
		{
			return ERR_MISC_ERR;
		}
		if (!en.hasNext())
		{
			return ERR_OBJECT_NOT_FOUND;
		}
		int sent = 0;
		ByteWriter w3 = new ByteWriter(maxReply);
		for (int i=0; sent < maxRecords && en.hasNext(); i++)
		{
			AFP_CNode next = en.next();
			if (i < startIndex)
			{
				if (!en.hasNext())
				{
					return ERR_OBJECT_NOT_FOUND;
				}
				continue;
			}
			ByteWriter w2 = new ByteWriter(128);
			if (next.isDirectory())
			{
				sendDirectoryInfo(w2, next, dirFlags, MODE_EXT);
			}
			else
			{
				sendFileInfo(w2, next, fileFlags, MODE_EXT);
			}
			byte data[] = w2.toByteArray();
			if (data.length % 2 == 0)
			{
				w3.writeShort(data.length+2);
				w3.writeBytes(data);
			}
			else
			{
				w3.writeShort(data.length+3);
				w3.writeBytes(data);
				w3.writeByte(0);
			}
			sent++;
			if (w3.getSize() > maxReply - 128)
			{
				break;
			}
		}

		ww.writeShort(sent);
		ww.writeBytes(w3.toByteArray());
		return ERR_NO_ERR;
	}

	// ----------------------------------------------------------------------------------------

	// USED BY: OpenVolume and GetVolumeParams
	private void sendVolumeInfo(ByteWriter ww, AFP_Volume vol, int flags)
		throws IOException
	{
		ww.writeShort(flags);
		ww.markDeferredOffset();
		if (hasBits(flags, VOL_BIT_ATTRIBUTE))    { ww.writeShort(vol.getAttributes());     }
		if (hasBits(flags, VOL_BIT_SIGNATURE))    { ww.writeShort(vol.getSignature());      }
		if (hasBits(flags, VOL_BIT_CREATE_DATE))  { ww.writeInt(vol.getCreateDate());       }
		if (hasBits(flags, VOL_BIT_MOD_DATE))     { ww.writeInt(vol.getModifiedDate());     }
		if (hasBits(flags, VOL_BIT_BACKUP_DATE))  { ww.writeInt(vol.getBackupDate());       }
		if (hasBits(flags, VOL_BIT_ID))           { ww.writeShort(vol.getID());             }
		if (hasBits(flags, VOL_BIT_BYTES_FREE))   { ww.writeInt(vol.getBytesFree());        }
		if (hasBits(flags, VOL_BIT_BYTES_TOTAL))  { ww.writeInt(vol.getBytesTotal());       }
		if (hasBits(flags, VOL_BIT_NAME))         { ww.writePStringDeferred(vol.getName()); }
		if (hasBits(flags, VOL_BIT_XBYTES_FREE))  { ww.writeLong(vol.getExtBytesFree());    }
		if (hasBits(flags, VOL_BIT_XBYTES_TOTAL)) { ww.writeLong(vol.getExtBytesTotal());   }
		if (hasBits(flags, VOL_BIT_BLOCK_SIZE))   { ww.writeInt(vol.getBlockSize());        }
	}

	// USED BY: GetFileDirParams
	private void sendDirectoryInfo(ByteWriter ww, AFP_CNode node, int flags, int mode)
		throws IOException
	{
		// 0x80 = directory + 0x00 = pad
		switch (mode)
		{
			case MODE_OLD: ww.writeByte(0x80); break;
			case MODE_EXT: ww.writeShort(0x8000); break;
		}
		ww.markDeferredOffset();
		if (hasBits(flags, DIR_BIT_ATTRIBUTE))       { ww.writeShort(node.getAttributes());           }
		if (hasBits(flags, DIR_BIT_PARENT_DIR_ID))   { ww.writeInt(node.getParentNodeID());           }
		if (hasBits(flags, DIR_BIT_CREATE_DATE))     { ww.writeInt(node.getCreateDate());             }
		if (hasBits(flags, DIR_BIT_MOD_DATE))        { ww.writeInt(node.getModifiedDate());           }
		if (hasBits(flags, DIR_BIT_BACKUP_DATE))     { ww.writeInt(node.getBackupDate());             }
		if (hasBits(flags, DIR_BIT_FINDER_INFO))     { ww.writeBytes(node.finderInfo());              }
		if (hasBits(flags, DIR_BIT_LONG_NAME))       { ww.writePStringDeferred(node.longName());      }
		if (hasBits(flags, DIR_BIT_SHORT_NAME))      { ww.writePStringDeferred(node.shortName());     }
		if (hasBits(flags, DIR_BIT_NODE_ID))         { ww.writeInt(node.getNodeID());                 }
		if (hasBits(flags, DIR_BIT_OFFSPRING_COUNT)) { ww.writeShort(node.countOffspring());          }
		if (hasBits(flags, DIR_BIT_OWNER_ID))        { ww.writeInt(node.getOwnerID());                }
		if (hasBits(flags, DIR_BIT_GROUP_ID))        { ww.writeInt(node.getGroupID());                }
		if (hasBits(flags, DIR_BIT_ACCESS_RIGHTS))   { ww.writeInt(node.getAccessRights());           }
		if (hasBits(flags, DIR_BIT_UTF8_NAME))       { ww.writeAFPStringDeferred(node.getUTF8Name()); }
		if (hasBits(flags, DIR_BIT_UNIX_PRIVS))      { ww.writeBytes(node.unixPrivs());               }
	}

	// USED BY: SetDirParams
	private void recvDirectoryInfo(ByteReader rr, AFP_CNode node, int flags)
		throws IOException
	{
		rr.skipBytes(rr.getPosition() % 2);
		if (hasBits(flags,  DIR_BIT_ATTRIBUTE))      { node.setAttributes(rr.readShort());            }
		if (hasBits(flags,  DIR_BIT_CREATE_DATE))    { node.setCreateDate(rr.readInt());              }
		if (hasBits(flags,  DIR_BIT_MOD_DATE))       { node.setModifiedDate(rr.readInt());            }
		if (hasBits(flags,  DIR_BIT_BACKUP_DATE))    { node.setBackupDate(rr.readInt());              }
		if (hasBits(flags,  DIR_BIT_FINDER_INFO))    { node.setFinderInfo(rr.readBytes(32));          }
		if (hasBits(flags,  DIR_BIT_UNIX_PRIVS))     { node.setUnixPrivs(rr.readBytes(16));           }
		if (hasBits(flags,  DIR_BIT_PARENT_DIR_ID |
						    DIR_BIT_LONG_NAME |
						    DIR_BIT_SHORT_NAME |
						    DIR_BIT_NODE_ID |
						    DIR_BIT_OFFSPRING_COUNT |
						    DIR_BIT_OWNER_ID |
						    DIR_BIT_GROUP_ID |
						    DIR_BIT_ACCESS_RIGHTS |
						    DIR_BIT_UTF8_NAME))      { throw new AFP_Error(ERR_BITMAP_ERR);           }
	}

	// USED BY: GetFileDirParams, EnumerateExt2
	private void sendFileInfo(ByteWriter ww, AFP_CNode node, int flags, int mode)
		throws IOException
	{
		// 0x00 = file + 0x00 = pad
		switch (mode)
		{
			case MODE_OLD: ww.writeByte(0x00); break;
			case MODE_EXT: ww.writeShort(0x0000); break;
		}
		ww.markDeferredOffset();
		if (hasBits(flags, FILE_BIT_ATTRIBUTE))      { ww.writeShort(node.getAttributes());           }
		if (hasBits(flags, FILE_BIT_PARENT_DIR_ID))  { ww.writeInt(node.getParentNodeID());           }
		if (hasBits(flags, FILE_BIT_CREATE_DATE))    { ww.writeInt(node.getCreateDate());             }
		if (hasBits(flags, FILE_BIT_MOD_DATE))       { ww.writeInt(node.getModifiedDate());           }
		if (hasBits(flags, FILE_BIT_BACKUP_DATE))    { ww.writeInt(node.getBackupDate());             }
		if (hasBits(flags, FILE_BIT_FINDER_INFO))    { ww.writeBytes(node.finderInfo());              }
		if (hasBits(flags, FILE_BIT_LONG_NAME))      { ww.writePStringDeferred(node.longName());      }
		if (hasBits(flags, FILE_BIT_SHORT_NAME))     { ww.writePStringDeferred(node.shortName());     }
		if (hasBits(flags, FILE_BIT_NODE_ID))        { ww.writeInt(node.getNodeID());                 }
		if (hasBits(flags, FILE_BIT_DATA_FORK_LEN))  { ww.writeInt(node.getShortDataForkLen());       }
		if (hasBits(flags, FILE_BIT_RSRC_FORK_LEN))  { ww.writeInt(node.getShortResourceForkLen());   }
		if (hasBits(flags, FILE_BIT_XDATA_FORK_LEN)) { ww.writeLong(node.getDataForkLen());           }
		if (hasBits(flags, FILE_BIT_LAUNCH_LIMIT))   { ww.writeShort(node.getLaunchLimit());          }
		if (hasBits(flags, FILE_BIT_UTF8_NAME))      { ww.writeAFPStringDeferred(node.getUTF8Name()); }
		if (hasBits(flags, FILE_BIT_XRSRC_FORK_LEN)) { ww.writeLong(node.getResourceForkLen());       }
		if (hasBits(flags, FILE_BIT_UNIX_PRIVS))     { ww.writeBytes(node.unixPrivs());               }
	}

	// USED BY: SetFileParams
	private void recvFileInfo(ByteReader rr, AFP_CNode node, int flags)
		throws IOException
	{
		rr.skipBytes(rr.getPosition() % 2);
		if (hasBits(flags, FILE_BIT_ATTRIBUTE))      { node.setAttributes(rr.readShort());            }
		if (hasBits(flags, FILE_BIT_CREATE_DATE))    { node.setCreateDate(rr.readInt());              }
		if (hasBits(flags, FILE_BIT_MOD_DATE))       { node.setModifiedDate(rr.readInt());            }
		if (hasBits(flags, FILE_BIT_BACKUP_DATE))    { node.setBackupDate(rr.readInt());              }
		if (hasBits(flags, FILE_BIT_FINDER_INFO))    { node.setFinderInfo(rr.readBytes(32));          }
		if (hasBits(flags, FILE_BIT_UNIX_PRIVS))     { node.setUnixPrivs(rr.readBytes(16));           }
		if (hasBits(flags, FILE_BIT_PARENT_DIR_ID |
						   FILE_BIT_LONG_NAME |
						   FILE_BIT_SHORT_NAME |
						   FILE_BIT_NODE_ID |
						   FILE_BIT_DATA_FORK_LEN |
						   FILE_BIT_RSRC_FORK_LEN |
						   FILE_BIT_XDATA_FORK_LEN |
						   FILE_BIT_LAUNCH_LIMIT |
						   FILE_BIT_UTF8_NAME |
						   FILE_BIT_XRSRC_FORK_LEN)) { throw new AFP_Error(ERR_BITMAP_ERR);           }
	}
	
	private void print(String t, int v) { debug("("+t+") = ("+v+")"); }
	private void print(String t, long v) { debug("("+t+") = ("+v+")"); }

	// ----------------------------------------------------------------------------------------

	private AFP_Fork getFork(int forkRef)
		throws AFP_Error
	{
		AFP_Fork fork = (AFP_Fork)openForks.get(new Integer(forkRef));
		if (fork == null)
		{
			throw new AFP_Error(ERR_PARAM_ERR);
		}
		return fork;
	}

	private Path openPath(int volID, int dirID, String pathName)
		throws AFP_Error
	{
		return new Path(volID, dirID, pathName).open();
	}

	private Path createDirPath(int volID, int dirID, String pathName)
		throws AFP_Error
	{
		return new Path(volID, dirID, pathName).createDir();
	}

	private Path createFilePath(int volID, int dirID, String pathName)
		throws AFP_Error
	{
		return new Path(volID, dirID, pathName).createFile();
	}

	// TODO: does not parse/handle null navigation bytes
	private class Path
	{
		private AFP_Volume volume;
		private AFP_CNode node;
		private String path;

		Path(int volID, int dirID, String pathName)
			throws AFP_Error
		{
			volume = server.getVolume(volID);
			if (volume == null)
			{
				throw new AFP_Error(ERR_PARAM_ERR);
			}
			node = volume.getCNode(dirID);
			if (node == null)
			{
				throw new AFP_Error(ERR_OBJECT_NOT_FOUND);
			}
			path = pathName;
		}

		AFP_CNode getNode()
		{
			return node;
		}

		AFP_Volume getVolume()
		{
			return volume;
		}
		
		Path open()
			throws AFP_Error
		{
			if (path.length() > 0)
			{
				node = node.getChild(path);
			}
			if (node == null)
			{
				throw new AFP_Error(ERR_OBJECT_NOT_FOUND);
			}
			return this;
		}

		Path createDir()
			throws AFP_Error
		{
			return create(true);
		}

		Path createFile()
			throws AFP_Error
		{
			return create(false);
		}

		Path create(boolean dir)
			throws AFP_Error
		{
			if (path == null || path.length() == 0)
			{
				throw new AFP_Error(ERR_OBJECT_EXISTS);
			}
			AFP_CNode nnode = dir ? node.createDirectory(path) : node.createFile(path);
			if (nnode == null)
			{
				throw new AFP_Error(ERR_ACCESS_DENIED);
			}
			node = nnode;
			return this;
		}
	}

	// ----------------------------------------------------------------------------------------
	
	private static int getUAM(String uam)
	{
		if (empty(uam))                      { return UAM_UNKNOWN; }
		if (uam.equals(UAM_STR_GUEST))       { return UAM_GUEST; }
		if (uam.equals(UAM_STR_CLEARTEXT))   { return UAM_CLEARTEXT; }
		if (uam.equals(UAM_STR_RANDOM_NUM1)) { return UAM_RANDOM_NUM1; }
		if (uam.equals(UAM_STR_RANDOM_NUM2)) { return UAM_RANDOM_NUM2; }
		if (uam.equals(UAM_STR_DHX_128))     { return UAM_DHX_128; }
		if (uam.equals(UAM_STR_DHX_DYNAMIC)) { return UAM_DHX_DYNAMIC; }
		if (uam.equals(UAM_STR_KERBEROS))    { return UAM_KERBEROS; }
		return UAM_UNKNOWN;
	}

	private byte[] keyBytes(BigInteger bi)
	{
		return keyBytes(bi, 16);
	}

	private byte[] keyBytes(BigInteger bi, int len)
	{
		byte b[] = new byte[len];
		byte k[] = bi.toByteArray();
		int spos = 0;
		while (k[spos] == 0 && spos < k.length)
		{
			spos++;
		}
		if (spos == k.length)
		{
			return b;
		}
		System.arraycopy(k,spos,b,0,k.length-spos);
		return b;
	}

}

