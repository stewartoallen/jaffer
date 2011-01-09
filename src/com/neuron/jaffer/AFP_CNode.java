/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.util.Iterator;

public abstract class AFP_CNode extends Utility implements AFP_Constants, Iterable<AFP_CNode>
{
	public final static int NODE_UNKNOWN    = 0x00;
	public final static int NODE_FILE       = 0x01;
	public final static int NODE_DIRECTORY  = 0x02;
                                           
	public final static int MODE_READ       = 0x01; // bit 0
	public final static int MODE_WRITE      = 0x02; // bit 1
	public final static int MODE_READ_LOCK  = 0x10; // bit 4
	public final static int MODE_WRITE_LOCK = 0x20; // bit 5

	// -----------------------------------------------------------------------

	private final int id;

	AFP_CNode(int id)
	{
		this.id = id;
	}

	// -----------------------------------------------------------------------

	public final byte[] unixPrivs()
	{
		 return bounded(getUnixPrivs(),16);
	}

	public final byte[] finderInfo()
	{
		 return bounded(getFinderInfo(),32);
	}

	private byte[] bounded(byte b[], int len)
	{
		if (b == null)
		{
			return new byte[len];
		}
		if (b.length == len)
		{
			return b;
		}
		byte nb[] = new byte[len];
		System.arraycopy(b,0,nb,0,Math.min(len,b.length));
		return nb;
	}

	public String longName()
	{
		String nm = getLongName();
		if (nm.length() > 31)
		{
			return nm.substring(0,31);
		}
		return nm;
	}

	public String shortName()
	{
		String nm = getShortName();
		if (nm.length() > 12)
		{
			return nm.substring(0,12);
		}
		return nm;
	}

	public int countOffspring()
	{
		int count = 0;
		for (Iterator e = getChildren(); e != null && e.hasNext(); count++)
		{
			e.next();
		}
		return count;
	}

	public int getShortDataForkLen()
	{
		return (int)Math.min(getDataForkLen(), MAX_SHORT_FORK_LEN);
	}

	public int getShortResourceForkLen()
	{
		return (int)Math.min(getResourceForkLen(), MAX_SHORT_FORK_LEN);
	}

	public Iterator<AFP_CNode> iterator() {
		return getChildren();
	}
	
	// -----------------------------------------------------------------------

	public final int getNodeID()
	{
		return id;
	}

	public abstract boolean isDirectory()
		;
	
	public abstract int getParentNodeID()
		;

	public abstract int getAttributes()
		;

	public abstract void setAttributes(int att)
		;

	public abstract int getCreateDate()
		;

	public abstract void setCreateDate(int date)
		;

	public abstract int getModifiedDate()
		;

	public abstract void setModifiedDate(int date)
		;

	public abstract int getBackupDate()
		;

	public abstract void setBackupDate(int date)
		;

	public abstract byte[] getFinderInfo()
		;

	public abstract void setFinderInfo(byte info[])
		;

	public abstract String getLongName()
		;

	public abstract String getShortName()
		;

	public abstract String getUTF8Name()
		;

	public abstract boolean delete()
		;

	// dir MUST be a directory, newName can be null in which case
	// the file or directory retains it's original name
	public abstract boolean moveTo(AFP_CNode dir, String newName)
		;

	// see AFP3.1 p. 36
	public abstract byte[] getUnixPrivs()
		;

	public abstract void setUnixPrivs(byte privs[])
		;

	// files only
	public abstract int getLaunchLimit()
		;

	public abstract long getDataForkLen()
		;

	public abstract long getResourceForkLen()
		;

	public abstract AFP_Fork openFileFork(int flags)
		;

	public abstract AFP_Fork openResourceFork(int flags)
		;

	// directory only
	public abstract int getAccessRights()
		;

	public abstract int getOwnerID()
		;

	public abstract int getGroupID()
		;

	public abstract AFP_CNode createFile(String name)
		;

	public abstract AFP_CNode createDirectory(String name)
		;

	public abstract AFP_CNode getChild(String name)
		;

	public abstract Iterator<AFP_CNode> getChildren()
		;
}

