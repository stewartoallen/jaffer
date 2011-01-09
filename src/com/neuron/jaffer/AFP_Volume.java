/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

public abstract class AFP_Volume implements AFP_Constants
{
	/** assigned by the AFP server */
	private int id;

	public abstract String getName()
		;

	public abstract int getCreateDate()
		;

	public abstract int getBackupDate()
		;

	public abstract int getModifiedDate()
		;

	public abstract void setBackupDate(int d)
		;

	public abstract void setModifiedDate(int d)
		;

	// flat, fixed, variable
	public abstract int getSignature()
		;

	public abstract int getAttributes()
		;

	public abstract void setAttributes(int attr)
		;

	public abstract int getBlockSize()
		;

	public abstract int getBytesFree()
		;

	public abstract int getBytesTotal()
		;

	public abstract long getExtBytesFree()
		;

	public abstract long getExtBytesTotal()
		;

	public abstract String getPassword()
		;

	public abstract boolean hasUnixPrivs()
		;

	/**
	 * IDs 0-16 are reserved.  0 is invalid.  1 is the
	 * id of the Volume Node.  The Volumne node has only
	 * one child and that's the Root Node (id 2).  The
	 * Root node's name must be the same as the Volume
	 * name or mounts will fail.
	 */
	public abstract AFP_CNode getCNode(int id)
		;

	/**
	 * @return volume ID (assigned by the AFS server). 
	 */
	public final int getID()
	{
		return id;
	}

	/**
	 * @param id set volume ID (assigned by the AFS server). 
	 */
	public final void setID(int id)
	{
		this.id = id;
	}
}
