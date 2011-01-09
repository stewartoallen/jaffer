/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.IOException;
import java.io.RandomAccessFile;

// TODO: implement readAFPString correctly (unicode)
public final class ByteReader extends Utility
{
	private byte data[];
	private int pos;
	private int max;

	ByteReader(byte b[])
	{
		this(b, b.length);
	}

	ByteReader(byte b[], int max)
	{
		this.max = max;
		this.data = b;
		this.pos = 0;
	}

	public long writeToFile(RandomAccessFile file, long length)
		throws IOException
	{
		int max = (int)Math.min(length, (long)(data.length - pos));
		file.write(data, pos, max);
		return (long)max;
	}

	public int getPosition()
	{
		return pos;
	}

	public boolean hasMoreData()
	{
		return pos < max;
	}

	public int getAvailable()
	{
		return max - pos;
	}

	public void seek(int pos)
	{
		this.pos = pos;
	}

	public int skip(int len)
	{
		pos += len;
		return len;
	}

	public int skipBytes(int len)
	{
		pos += len;
		return len;
	}

	public byte[] readBytes(int len)
	{
		byte d[] = new byte[len];
		readBytes(d);
		return d;
	}

	public void readBytes(byte b[])
	{
		System.arraycopy(data,pos,b,0,b.length);
		pos += b.length;
	}

	public void readBytes(byte b[], int off, int len)
	{
		System.arraycopy(data,pos,b,off,len);
		pos += len;
	}

	public String readString(int len)
	{
		byte b[] = new byte[len];
		readBytes(b);
		return new String(b);
	}

	public String readCString(int max)
	{
		int scan = 0;
		while (data[pos+scan] != 0)
		{
			scan++;
			if (scan == max)
			{
				return null;
			}
		}
		String s = new String(data, pos, scan);
		pos += scan;
		return s;
	}

	public String readPString()
	{
		String s = readPString(data,pos);
		pos += s.length() + 1;
		return s;
	}

	public String[] readPStringArray()
	{
		String s[] = readPStringArray(data,pos);
		for (int i=0; i<s.length; i++)
		{
			pos += s[i].length() + 1;
		}
		return s;
	}

	// read type byte. return PString for 1,2 and AFPString for 3
	public String readTypedString()
	{
		if (readUnsignedByte() == 3)
		{
			return readAFPString();
		}
		else
		{
			return readPString();
		}
	}
	
	// unicode?
	public String readAFPString()
	{
		String s = readAFPString(data,pos);
		pos += s.length() + 2;
		return s;
	}
	
	public byte readByte()
	{
		return data[pos++];
	}

	public int readUnsignedByte()
	{
		return data[pos++] & 0xff;
	}

	public char readChar()
	{
		return (char)(((data[pos++]&0xff)<<8)|(data[pos++]&0xff)) ;
	}

	public int readInt()
	{
		return (int)(
			((data[pos++]&0xff)<<24)|((data[pos++]&0xff)<<16)|
			((data[pos++]&0xff)<<8)|(data[pos++]&0xff)
		);
	}

	public long readLong()
	{
		return (long)(
			((long)(data[pos++]&0xff)<<56)|((long)(data[pos++]&0xff)<<48)|
			((long)(data[pos++]&0xff)<<40)|((long)(data[pos++]&0xff)<<32)|
			((long)(data[pos++]&0xff)<<24)|((long)(data[pos++]&0xff)<<16)|
			((long)(data[pos++]&0xff)<<8) | (long)(data[pos++]&0xff)
		);
	}

	public short readShort()
	{
		return (short)(((data[pos++]&0xff)<<8)|(data[pos++]&0xff));
	}

	public int readUnsignedShort()
	{
		return (readShort() & 0xffff);
	}
}

