/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class AppleDouble extends RandomAccessFile
{
	public final static int MAGIC         = 0x51607;
	public final static int VERSION       = 0x20000;
	public final static int ENTRIES       = 0x02;
	public final static int OFF_MAGIC     = 0x00;
	public final static int OFF_VERSION   = 0x04;
	public final static int OFF_ENTRIES   = 0x18;
	public final static int OFF_FINDER    = 0x32;
	public final static int OFF_RESOURCE  = 0x52;
	public final static int OFF_RES_LEN   = 0x2e;
	public final static int LEN_FINDER    = 0x20;
	public final static int ID_FINDER     = 0x09;
	public final static int ID_RESOURCE   = 0x02;

	AppleDouble(File file, String mode)
		throws IOException
	{
		super(file, mode);
		if (realLength() > 0)
		{
			int val = -1;
			realSeek(OFF_MAGIC);
			if ((val=readInt()) != MAGIC)
			{
				throw new IOException("Not a valid AppleDouble file "+val);
			}
			realSeek(OFF_VERSION);
			if (readInt() != VERSION)
			{
				throw new IOException("AppleDouble version not supported");
			}
			realSeek(OFF_ENTRIES);
			if (readUnsignedShort() != ENTRIES)
			{
				throw new IOException("Expected two AppleDouble entries");
			}
		}
		if (mode.equalsIgnoreCase("r"))
		{
			return;
		}
		// magic + version
		realSeek(OFF_MAGIC);
		writeInt(MAGIC);
		writeInt(VERSION);
		realSeek(OFF_ENTRIES);
		// number of entries
		writeShort(ENTRIES);
		// finder info entry
		writeInt(ID_FINDER);
		writeInt(OFF_FINDER);
		writeInt(LEN_FINDER);
		// resource fork entry
		writeInt(ID_RESOURCE);
		writeInt(OFF_RESOURCE);
		// length must extend to at least resource fork offset
		if (length() < 0)
		{
			writeInt(0);
			setLength(0);
		}
	}

	public void writeFinderInfo(byte b[])
		throws IOException
	{
		super.seek(OFF_FINDER);
		write(b);
	}

	public void readFinderInfo(byte b[])
		throws IOException
	{
		super.seek(OFF_FINDER);
		readFully(b);
	}

	public void realSeek(long off)
		throws IOException
	{
		super.seek(off);
	}

	public long realLength()
		throws IOException
	{
		return super.length();
	}

	public void seek(long off)
		throws IOException
	{
		super.seek(OFF_RESOURCE + off);
	}

	public long length()
		throws IOException
	{
		return super.length() - OFF_RESOURCE;
	}

	public void write(byte data[], int off, int len)
		throws IOException
	{
		super.write(data, off, len);
	}

	public void setLength(long len)
		throws IOException
	{
		long opos = getFilePointer(); 
		super.seek(OFF_RES_LEN);
		writeInt((int)len);
		super.setLength(OFF_RESOURCE + len);
		super.seek(opos);
	}

	public void close()
		throws IOException
	{
		try
		{
			super.seek(OFF_RES_LEN);
			writeInt((int)length());
		}
		catch (Exception ex) { }
		super.close();
	}
}

