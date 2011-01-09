/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

public final class ByteWriter extends Utility
{
	private Vector deferred;
	private int deferredOffset;
	private byte data[];
	private int pos;

	private final static int TYPE_PSTRING = 1;
	private final static int TYPE_AFPSTRING = 2;
	private final static int TYPE_PSTRING_ARR = 3;
	private final static int TYPE_AFPSTRING_ARR = 4;
	private final static int TYPE_BYTE_ARR = 5;

	ByteWriter(int size)
	{
		this(new byte[size]);
	}

	ByteWriter(byte data[])
	{
		this.data = data;
	}

	// non-write helper methods

	public void readFromInput(InputStream in, int len)
	throws IOException
{
	int read = 0;
	while (read < len)
	{
		int got = in.read(data, pos, len - read);
		if (got < 0)
		{
			throw new EOFException();
		}
		read += got;
	}
	pos += len;
}

	public void readFromInput(DataInput file, int length)
		throws IOException
	{
		file.readFully(data, pos, (int)length);
		pos += length;
	}

	public byte[] toByteArray()
		throws IOException
	{
		flushDeferred();
		byte ndata[] = new byte[pos];
		System.arraycopy(data, 0, ndata, 0, pos);
		return ndata;
	}

	public void writeTo(OutputStream os)
		throws IOException
	{
		flushDeferred();
		os.write(data, 0, pos);
	}

	private void flushDeferred()
	{
		if (deferred != null)
		{
			for (int i=0; i<deferred.size(); i++)
			{
				((Deferred)deferred.get(i)).writeString();
				((Deferred)deferred.get(i)).writePtr(data);
			}
			deferred = null;
		}
	}

	public int getOffset()
	{
		return pos;
	}

	public int getSize()
	{
		flushDeferred();
		return pos;
	}

	// deferred write methods

	public void markDeferredOffset()
	{
		deferredOffset = pos;
	}

	public void writeAFPStringDeferred(String s)
	{
		writeDeferred(s, TYPE_AFPSTRING);
	}

	public void writePStringDeferred(String s)
	{
		writeDeferred(s, TYPE_PSTRING);
	}

	public void writePStringArrayDeferred(String s[])
	{
		writeDeferred(s, TYPE_PSTRING_ARR);
	}

	public void writeBytesDeferred(byte b[])
	{
		writeDeferred(b, TYPE_BYTE_ARR);
	}

	private void writeDeferred(Object s, int type)
	{
		if (deferred == null)
		{
			deferred = new Vector(3);
		}
		deferred.add(new Deferred(s, type));
		writeShort(0);
	}

	// immediate write methods

	public void writePString(String s)
	{
		writeByte(s.length());
		writeBytes(s.getBytes());
	}

	public void writePStringArray(String s[])
	{
		writeByte(s.length);
		for (int i=0; i<s.length; i++)
		{
			writePString(s[i]);
		}
	}

	public void writeAFPString(String s)
	{
		writeShort(s.length());
		writeBytes(s.getBytes());
	}

	public void writeByte(int i)
	{
		data[pos++] = (byte)(i & 0xff);
	}

	public void writeBytes(byte b[])
	{
		System.arraycopy(b, 0, data, pos, b.length);
		pos += b.length;
	}

	public void writeBytes(byte b[], int off, int len)
	{
		System.arraycopy(b, off, data, pos, len);
		pos += len;
	}

	public void writeShortAtPos(int i, int pos)
	{
		writeInt2(data, pos, i);
	}

	public void writeIntAtPos(int i, int pos)
	{
		writeInt4(data, pos, i);
	}

	public void writeShort(int i)
	{
		writeInt2(data, pos, i);
		pos += 2;
	}

	public void writeInt(int i)
	{
		writeInt4(data, pos, i);
		pos += 4;
	}

	public void writeLong(long l)
	{
		writeInt8(data, pos, l);
		pos += 8;
	}

	public void write(int v)
	{
		writeByte(v);
	}

	public void writeBytes(String s)
	{
		writeBytes(s.getBytes());
	}

	// deferred write helper class

	private class Deferred
	{
		private int type;
		private int ptrPos;
		private int arrPos;
		private Object object;

		Deferred(Object o, int t)
		{
			this.object = o;
			this.type = t;
			this.ptrPos = pos;
		}

		void writeString()
		{
			this.arrPos = pos - deferredOffset;
			switch (type)
			{
				case TYPE_PSTRING: writePString((String)object); break;
				case TYPE_AFPSTRING: writeAFPString((String)object); break;
				case TYPE_PSTRING_ARR: writePStringArray((String[])object); break;
				case TYPE_BYTE_ARR: writeBytes((byte[])object); break;
				default: error("unknown deferred object type: "+type); break;
			}
		}

		void writePtr(byte b[])
		{
			writeInt2(b, ptrPos, arrPos);
		}
	}

}

