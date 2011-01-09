/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// TODO : payload becomes common to ByteReader/ByteWriter
class DSI_Packet extends Utility implements DSI_Constants
{
	private static int maxR = 0;
	private static int maxW = 0;

	private int flags;
	private int command;
	private int requestID;
	private int errDataOff;
	private int dataLength;
	private int reserved;
	private byte payload[];
	private byte header[];
	private ByteWriter writer;

	DSI_Packet(int bufsize)
	{
		payload = new byte[bufsize+128];
		header = new byte[16];
		writer = new ByteWriter(payload);
	}

	DSI_Packet(InputStream is)
		throws IOException
	{
		this(0x8000);
		read(is);
	}

	DSI_Packet(int f, int c, int id, byte d[])
		throws IOException
	{
		this(d.length);
		flags = f;
		command = c;
		requestID = id;
		errDataOff = 0;
		dataLength = 0;
		reserved = 0;
		writer.writeBytes(d);
	}

	public void reset()
	{
		writer = new ByteWriter(payload);
	}

	public int getBufferSize()
	{
		return payload.length - 128;
	}

	public void dumpRecvPayload(String prefix)
	{
		dump(prefix, payload, dataLength);
	}

	public void dumpSendPayload(String prefix)
	{
		dump(prefix, payload, writer.getSize());
	}

	public ByteReader getReader()
	{
		return new ByteReader(payload, dataLength);
	}

	public ByteWriter getWriter()
	{
		return writer;
	}

	public int getFlags()
	{
		return flags;
	}

	public int getCommand()
	{
		return command;
	}

	public int getRequestID()
	{
		return requestID;
	}

	public void setRequestID(int id)
	{
		requestID = id;
	}

	public boolean isRequest()
	{
		return (flags & 0x1) == DSI_REQUEST;
	}

	public boolean isReply()
	{
		return (flags & 0x1) == DSI_REPLY;
	}

	public void setRequest()
	{
		flags = DSI_REQUEST;
	}

	public void setReply()
	{
		flags = DSI_REPLY;
	}

	public void setErrorCode(int ec)
	{
		errDataOff = ec;
	}

	public void setDataOffset(int ec)
	{
		errDataOff = ec;
	}

	private void readData(InputStream is, byte b[], int len)
		throws IOException
	{
		if (len > b.length)
		{
			throw new IOException("Read Request ("+len+") Exceeds Buffer Size ("+b.length+")!");
		}
		int read = 0;
		while (read < len)
		{
			int got = is.read(b, read, len - read);
			if (got < 0)
			{
				throw new EOFException();
			}
			read += got;
		}
	}

	public void read(InputStream is)
		throws IOException
	{
		readData(is, header, header.length);
		flags = header[0];
		command = header[1];
		requestID = readInt2(header, 2);
		errDataOff = readInt4(header, 4);
		dataLength = readInt4(header, 8);
		reserved = readInt4(header, 12);
		readData(is, payload, dataLength);
		//if (dataLength > maxR) { maxR = dataLength; System.out.println("max read now "+maxR); }
	}

	public void write(OutputStream os)
		throws IOException
	{
		header[0] = (byte)flags;
		header[1] = (byte)command;
		writeInt2(header, 2, requestID);
		writeInt4(header, 4, errDataOff);
		writeInt4(header, 8, writer.getSize());
		writeInt4(header,12, reserved);
		os.write(header);
		writer.writeTo(os);
		//dataLength = writer.getSize();
		os.flush();
		//if (dataLength > maxW) { maxW = dataLength; System.out.println("max write now "+maxW); }
	}

	public String toString()
	{
		if (command == CMD_COMMAND)
		{
			int cmd = isRequest() ? payload[0] & 0xff : 0;
			if (cmd >= AFP_Constants.COMMAND.length || cmd < 0) { cmd = 0; }
			return
				"f="+hex(flags)+",c="+hex(command)+",id="+hex(requestID)+
				",edo="+hex(errDataOff)+",rd="+hex(dataLength)+",sd="+hex(writer.getSize())+
				",cmd="+AFP_Constants.COMMAND[cmd];
		}
		else
		{
			return
				"f="+hex(flags)+",c="+hex(command)+",id="+hex(requestID)+
				",edo="+hex(errDataOff)+",rl="+hex(dataLength)+",sd="+hex(writer.getSize())+
				",dsi="+COMMAND[command];
		}
	}
}

