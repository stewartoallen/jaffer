/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.IOException;

// Reference: Inside Appletalk p. 442
// TODO: icon/mask support
class AFP_ServerInfo extends Utility
{
	private String serverName;
	private String afpVersions[];
	private String uamModules[];
	private Object icon;
	private int    flags;

	AFP_ServerInfo(String sn, String av[], String ua[], int flags)
	{
		this.serverName = sn;
		this.afpVersions = av;
		this.uamModules = ua;
		this.flags = flags;
	}

	AFP_ServerInfo(ByteReader rr)
		throws IOException
	{
		serverName = readPString(rr, readInt2(rr, 0));
		afpVersions = readPStringArray(rr, readInt2(rr, 2));
		uamModules  = readPStringArray(rr, readInt2(rr, 4));
		flags       = readInt2(rr, 8);
	}

	private int readInt2(ByteReader rr, int pos)
		throws IOException
	{
		rr.seek(pos);
		return rr.readUnsignedShort();
	}

	private String readPString(ByteReader rr, int pos)
		throws IOException
	{
		rr.seek(pos);
		return rr.readPString();
	}

	private String[] readPStringArray(ByteReader rr, int pos)
		throws IOException
	{
		rr.seek(pos);
		return rr.readPStringArray();
	}

	public void write(ByteWriter ww)
	{
		ww.writePStringDeferred("Jaffer");
		ww.writePStringArrayDeferred(afpVersions);
		ww.writePStringArrayDeferred(uamModules);
		ww.writeShort(0); // icon/mask offset
		ww.writeShort(flags);
		ww.writePString(serverName); // server name
		if (ww.getOffset() % 2 == 1)
		{
			ww.writeByte(0);
		}
		ww.writeShort(0); // server signature offset (not implemented)
		ww.writeShort(0); // network address offset (TODO)
		ww.writeShort(0); // directory names offset (not implemented)
		//ww.writePString(serverName); // server name in UTF8 (TODO)
	}

	private final static int codedLength(String s[])
	{
		int len = 1;
		for (int i=0; s != null && i<s.length; i++)
		{
			len += (s[i].length()+1);
		}
		return len;
	}

	public String toString()
	{
		return
			"sn="+serverName+",av="+list(afpVersions)+",uam="+list(uamModules)+",fl="+bits(flags,2);
	}
}

