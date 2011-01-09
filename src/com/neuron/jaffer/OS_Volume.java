/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.Iterator;

class OS_Volume extends AFP_Volume
{
	private final static int LOCK_BITS = AFP_CNode.MODE_WRITE_LOCK | AFP_CNode.MODE_READ_LOCK;

	private int nextID = 32;
	private String volName;
	private Hashtable allNodes;
	private boolean readonly;
	private String passwd;

	OS_Volume(String vname, File root, String password)
	{
		this.volName = vname;
		this.allNodes = new Hashtable();
		this.passwd = password;

		addNode(new VolumeNode(vname, root));
	}

	// custom methods
	public void setReadOnly(boolean ro)
	{
		this.readonly = ro;
	}

	private synchronized int getNextID()
	{
		return nextID++;
	}

	// global node (database) handling
	private OSNode addNode(OSNode node)
	{
		allNodes.put(new Integer(node.getNodeID()), node);
		addFileMap(node.file(), node);
		return node;
	}

	// return node if it's cached, otherwise create in cache
	private OSNode getNode(int pid, File file)
	{
		OSNode node = getNode(file);
		if (node != null)
		{
			return node;
		}
		if (file.exists())
		{
			return addNode(new OSNode(pid, getNextID(), file));
		}
		return null;
	}

	private OSNode getNode(int id)
	{
		return (OSNode)allNodes.get(new Integer(id));
	}

	private OSNode getNode(File file)
	{
		return (OSNode)allNodes.get(file);
	}

	private void delNode(int id)
	{
		OSNode node = getNode(id);
		if (node != null)
		{
			allNodes.remove(new Integer(id));
			delFileMap(node.file());
		}
	}

	private void delNode(File file)
	{
		OSNode node = getNode(file);
		if (node != null)
		{
			delFileMap(file);
			allNodes.remove(new Integer(node.getNodeID()));
		}
	}

	private void addFileMap(File file, OSNode node)
	{
		if (file != null)
		{
			allNodes.put(file, node);
		}
	}

	private void delFileMap(File file)
	{
		if (file != null)
		{
			allNodes.remove(file);
		}
	}

	@Override
	public String getName()
	{
		return volName;
	}

	@Override
	public int getCreateDate()
	{
		return 0xa;
	}

	@Override
	public int getModifiedDate()
	{
		return 0xb;
	}

	@Override
	public int getBackupDate()
	{
		return 0x80000000;
	}

	@Override
	public void setBackupDate(int d)
	{
	}

	@Override
	public void setModifiedDate(int d)
	{
	}

	@Override
	public int getAttributes()
	{
		return 
			(readonly              ? VOL_ATTR_READONLY : 0) |
			(getPassword() != null ? VOL_ATTR_PASSWORD : 0) |
			0;
	}

	@Override
	public void setAttributes(int attr)
	{
	}

	@Override
	public int getSignature()
	{
		return VOL_SIG_FIXED;
		//return VOL_SIG_VARIABLE;
	}

	@Override
	public int getBlockSize()
	{
		return 0x1000;
	}

	@Override
	public int getBytesFree()
	{
		return 0x55555555;
	}

	@Override
	public int getBytesTotal()
	{
		return 0x66666666;
	}

	@Override
	public long getExtBytesFree()
	{
		return 0x4545454545l;
	}

	@Override
	public long getExtBytesTotal()
	{
		return 0x4646464646l;
	}

	@Override
	public boolean hasUnixPrivs()
	{
		return false;
	}

	@Override
	public String getPassword()
	{
		return passwd;
	}

	@Override
	public AFP_CNode getCNode(int id)
	{
		return getNode(id);
	}

	// utility methods
	public String flagsToString(int flags)
	{
		if (Utility.hasBits(flags, AFP_CNode.MODE_WRITE) && !readonly)
		{
			return "rw";
		}
		else
		{
			return "r";
		}
	}

	// -------------------------------------------------------------------------

	private class VolumeNode extends OSNode
	{
		private OSNode root;

		VolumeNode(String volname, File volroot)
		{
			super(0, 1, null);
			root = addNode(new RootNode(volname, volroot));
		}

		public AFP_CNode getChild(String name)
		{
			return name.equals(root.name()) ? root : null;
		}
	}

	// -------------------------------------------------------------------------

	private class RootNode extends OSNode
	{
		private String name;

		RootNode(String name, File root)
		{
			super(1, 2, root);
			this.name = name;
		}

		public String name()
		{
			return name;
		}
	}

	// -------------------------------------------------------------------------

	// TODO: cache file[] until dir lastmodified changes w/ weak ref (dirs only)
	// TODO: move all lock info into a single locking class (files only)
	// TODO: posix locking. use our own RandomAccessFile replacement for this. (files only)
	private class OSNode extends AFP_CNode
	{
		private int pid;
		private File file;
		private int lockBits;      // file open lock bits
		private Range dataLocks;   // data fork byte range locks
		private Range rsrcLocks;   // rsrc fork byte range locks
		private long rsrcLength;   // cached resource fork len
		private byte[] finderInfo; // cached finder info

		OSNode(int pid, int id, File file)
		{
			super(id);
			this.pid = pid;
			this.file = file;
			this.rsrcLength = -1;
		}

		public String toString()
		{
			return "NODE["+getNodeID()+"]=P("+pid+")F("+file+")N("+name()+")";
		}

		// -- custom/utility methods --
		private File file()
		{
			return file;
		}

		/** protected to allow overriding in subclasses */
		protected String name()
		{
			return file != null ? file.getName() : "<null>";
		}

		private boolean hasLockBits(boolean data, int flags)
		{
			// range locks can prevent open locks
			if ((data && dataLocks != null) || (!data && rsrcLocks != null))
			{
				return true;
			}
			return (((flags & LOCK_BITS) << (data ? 8 : 0)) & lockBits) != 0;
		}

		private void setLockBits(boolean data, int flags)
		{
			flags = (flags & LOCK_BITS) << (data ? 8 : 0);
			lockBits |= flags;
		}

		private void clearLockBits(boolean data, int flags)
		{
			flags = (flags & LOCK_BITS) << (data ? 8 : 0);
			lockBits &= (~flags);
		}

		/*
		private boolean canOpen(boolean data, File file, int flags)
		{
			boolean r = _canOpen(data, file, flags);
			if (!r)
			{
				System.out.println(
					"can't open ("+file+
					") f=("+hex(flags)+
					") b=("+hex(lockBits)+
					") e=("+file.exists()+
					") r=("+file.canRead()+
					") w=("+file.canWrite()+")");
			}
			return r;
		}
		*/

		private boolean canOpen(boolean data, File file, int flags)
		{
			// can't open if requested lock bit is set
			if (hasLockBits(data, flags))
			{
				return false;
			}
			if (Utility.hasBits(flags, AFP_CNode.MODE_WRITE))
			{
				return file.exists() ? file.canWrite() : file.getParentFile().canWrite();
			}
			else
			{
				return file.exists() && file.canRead();
			}
		}

		public synchronized boolean forkLockRange(boolean data, long offset, long length)
		{
			Range r = (data ? dataLocks : rsrcLocks);
			if (r == null)
			{
				r = new Range(offset, length);
				if (data)
				{
					dataLocks = r;
				}
				else
				{
					rsrcLocks = r;
				}
				return false;
			}
			else
			{
				return r.lock(offset, length);
			}
		}

		public synchronized boolean forkUnlockRange(boolean data, long offset, long length)
		{
			Range r = (data ? dataLocks : rsrcLocks);
			if (r == null)
			{
				return false;
			}
			else
			{
				Range tmp = r;
				Range last = null;
				while (tmp != null)
				{
					if (tmp.unlock(offset, length))
					{
						if (last != null)
						{
							last.next = tmp.next;
						}
						else
						{
							if (data)
							{
								dataLocks = tmp.next;
							}
							else
							{
								rsrcLocks = tmp.next;
							}
						}
						return true;
					}
					else
					{
						last = tmp;
						tmp = tmp.next;
					}
				}
				return false;
			}
		}

		private void saveResourceInfo()
		{
			if (finderInfo == null && rsrcLength < 0)
			{
				return;
			}
			try
			{
				ResourceFork rf = getResourceFork(MODE_WRITE);
				if (rf == null)
				{
					return;
				}
				if (finderInfo != null)
				{
					rf.writeFinderInfo(finderInfo);
				}
				rf.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		private void cacheResourceInfo()
		{
			if (finderInfo != null && rsrcLength >= 0)
			{
				return;
			}
			try
			{
				ResourceFork rf = getResourceFork(MODE_READ);
				if (rf == null)
				{
					return;
				}
				if (finderInfo == null)
				{
					finderInfo = new byte[16];
				}
				rf.readFinderInfo(finderInfo);
				rsrcLength = rf.getLength();
				rf.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		private ResourceFork getResourceFork(int flags)
		{
			return (ResourceFork)openResourceFork(flags);
		}

		private File getResourceForkFile()
		{
			return new File(file.getParentFile(), "._"+name());
		}

		public boolean delete()
		{
			OSNode parent = getNode(getParentNodeID());
			if (parent != null && file.delete())
			{
				getResourceForkFile().delete();
				delNode(file);
				return true;
			}
			else
			{
				return false;
			}
		}

		@Override
		public boolean isDirectory() {
			return file.isDirectory();
		}
		
		@Override
		public int getParentNodeID()
		{
			return pid;
		}

		@Override
		public int getAttributes()
		{
			return 0;
		}

		@Override
		public void setAttributes(int attr)
		{
		}

		@Override
		public int getOwnerID()
		{
			return 0;
		}

		@Override
		public int getGroupID()
		{
			return 0;
		}

		@Override
		public int getCreateDate()
		{
			return Utility.unix2afpTime(file.lastModified());
		}

		@Override
		public void setCreateDate(int date)
		{
		}

		@Override
		public int getModifiedDate()
		{
			return Utility.unix2afpTime(file.lastModified());
		}

		@Override
		public void setModifiedDate(int date)
		{
			file.setLastModified(Utility.afp2unixTime(date));
		}

		@Override
		public int getBackupDate()
		{
			return 0x80000000;
		}

		@Override
		public void setBackupDate(int date)
		{
		}

		@Override
		public byte[] getFinderInfo()
		{
			cacheResourceInfo();
			return finderInfo;
		}

		@Override
		public void setFinderInfo(byte b[])
		{
			finderInfo = b;
			saveResourceInfo();
		}

		@Override
		public String getLongName()
		{
			// TODO: munge
			return name();
		}

		@Override
		public String getShortName()
		{
			// TODO: munge
			return name();
		}

		@Override
		public String getUTF8Name()
		{
			return name();
		}

		@Override
		public byte[] getUnixPrivs()
		{
			return null;
		}

		@Override
		public void setUnixPrivs(byte b[])
		{
		}

		@Override
		public int getAccessRights()
		{
			boolean read = file.canRead();
			boolean write = file.canWrite();
			//System.out.println("Access on "+file+" read="+read+" write="+write);
			return ACCESS_EVERYTHING ^ (
				(read  ? 0 : ACCESS_OWNER_READ  | ACCESS_GROUP_READ  | ACCESS_ALL_READ  | ACCESS_UA_READ ) |
				(write ? 0 : ACCESS_OWNER_WRITE | ACCESS_GROUP_WRITE | ACCESS_ALL_WRITE | ACCESS_UA_WRITE)
			);
		}

		@Override
		public synchronized boolean moveTo(AFP_CNode dir, String name)
		{
			if (!dir.isDirectory())
			{
				return false;
			}
			File targetFile = ((OSNode)dir).file;
			File newFile = empty(name) ? targetFile : new File(targetFile, name);
			if (file.renameTo(newFile))
			{
				OSNode parent = getNode(getParentNodeID());
				getResourceForkFile().renameTo(new File(newFile.getParentFile(), "._"+newFile.getName()));
				delFileMap(file);
				file = newFile;
				pid = empty(name) ? dir.getParentNodeID() : dir.getNodeID();
				addFileMap(file, this);
				return true;
			}
			else
			{
				return false;
			}
		}

		@Override
		public AFP_CNode createDirectory(String name)
		{
			AFP_CNode ndir = getChild(name);
			if (ndir != null)
			{
				return null;
			}
			File nfdir = new File(file, name);
			if (nfdir.mkdirs())
			{
				return getNode(getNodeID(), nfdir);
			}
			else
			{
				return null;
			}
		}

		@Override
		public AFP_CNode createFile(String name)
		{
			AFP_CNode ndir = getChild(name);
			if (ndir != null)
			{
				return null;
			}
			File nfile = new File(file, name);
			try
			{
				if (nfile.createNewFile())
				{
					return getNode(getNodeID(), nfile);
				}
				else
				{
					return null;
				}
			}
			catch (Exception ex)
			{
				return null;
			}
		}

		@Override
		public AFP_CNode getChild(String name)
		{
			File nfile = new File(file, name);
			if (nfile.exists())
			{
				return getNode(getNodeID(), nfile);
			}
			else
			{
				delFileMap(nfile);
				return null;
			}
		}

		@Override
		public Iterator<AFP_CNode> getChildren()
		{
			return new NodeIterator(file);
		}

		@Override
		public int getLaunchLimit()
		{
			return 0;
		}

		@Override
		public long getDataForkLen()
		{
			return file.length();
		}

		@Override
		public long getResourceForkLen()
		{
			cacheResourceInfo();
			return rsrcLength;
		}

		@Override
		public AFP_Fork openFileFork(int flags)
		{
			if (!canOpen(true, file, flags))
			{
				return null;
			}
			try
			{
				return new DataFork(file, flags);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return null;
			}
		}

		@Override
		public AFP_Fork openResourceFork(int flags)
		{
			// you can't create a resource fork if you don't have
			// the same priviledges on the data fork
			if (getNodeID() < 32 || !canOpen(false, getResourceForkFile(), flags) || !canOpen(true, file, flags))
			{
				return null;
			}
			try
			{
				return new ResourceFork(getResourceForkFile(), flags);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return null;
			}
		}

		// -------------------------------------------------------------------------

		private class NodeIterator implements Iterator<AFP_CNode>
		{
			private File files[];
			private int pos;

			NodeIterator(File dir)
			{
				this.files = dir.listFiles();
				findNext();
			}

			private void findNext()
			{
				while (pos < files.length)
				{
					File next = files[pos];
					if (next.exists() && !next.getName().startsWith("._"))
					{
						return;
					}
					pos++;
				}
			}

			public boolean hasNext()
			{
				return pos < files.length;
			}

			public AFP_CNode next()
			{
				if (pos >= files.length)
				{
					return null;
				}
				OSNode on = getNode(getNodeID(), files[pos++]);
				findNext();
				return on;
			}

			public void remove() {
			}
		}

		// -------------------------------------------------------------------------

		private class ResourceFork extends DataFork
		{
			private File file;

			ResourceFork(File file, int flags)
				throws IOException
			{
				super(new AppleDouble(file, flagsToString(flags)), flags);
				this.file = file;
			}

			public File file()
			{
				return file;
			}

			public void renameTo(File dst)
			{
				if (file.renameTo(dst))
				{
					file = dst;
				}
			}

			public Type getForkType()
			{
				return Type.RESOURCE;
			}

			public void readFinderInfo(byte b[])
				throws IOException
			{
				((AppleDouble)nativeFile()).readFinderInfo(b);
			}

			public void writeFinderInfo(byte b[])
				throws IOException
			{
				((AppleDouble)nativeFile()).writeFinderInfo(b);
			}
		}

		// -------------------------------------------------------------------------

		private class DataFork extends AFP_Fork
		{
			private RandomAccessFile file;
			private Range ranges;
			private int flags;

			DataFork(File file, int flags)
				throws IOException
			{
				this(new RandomAccessFile(file, flagsToString(flags)), flags);
			}

			DataFork(RandomAccessFile file, int flags)
				throws IOException
			{
				this.file = file;
				this.flags = flags;
				setLockBits(getForkType() == Type.DATA, flags);
			}

			public RandomAccessFile nativeFile()
			{
				return file;
			}

			public Type getForkType()
			{
				return Type.DATA;
			}

			public void readRange(long offset, long length, ByteWriter ww)
				throws IOException
			{
				length = Math.min(getLength() - offset, length);
				if (length < 0 || offset < 0)
				{
					throw new EOFException();
				}
				file.seek(offset);
				ww.readFromInput(file, (int)length);
			}

			public long writeRange(long offset, long length, ByteReader rr)
				throws IOException
			{
				file.seek(offset);
				return rr.writeToFile(file, length);
			}

			public boolean lockRange(long offset, long length)
			{
				//System.out.println("lock("+offset+","+length+")");
				if (forkLockRange(getForkType() == Type.DATA, offset, length))
				{
					Range nr = new Range(offset, length);
					nr.next = ranges;
					ranges = nr;
					return true;
				}
				return false;
			}

			public boolean unlockRange(long offset, long length)
			{
				//System.out.println("unlock("+offset+","+length+")");
				if (forkUnlockRange(getForkType() == Type.DATA, offset, length))
				{
					Range r = ranges;
					Range l = null;
					while (r != null)
					{
						if (r.offset == offset)
						{
							if (l != null)
							{
								l.next = r.next;
							}
							else
							{
								ranges = r.next;
							}
							break;
						}
					}
					return true;
				}
				return false;
			}

			public long getLength()
				throws IOException
			{
				return file.length();
			}

			public void setLength(long len)
				throws IOException
			{
				file.setLength(len);
			}

			public void flush()
				throws IOException
			{
				file.getFD().sync();
			}

			public synchronized void close()
			{
				try
				{
					if (file == null)
					{
						return;
					}
					clearLockBits(getForkType() == Type.DATA, flags);
					while (ranges != null)
					{
						forkUnlockRange(getForkType() == Type.DATA, ranges.offset, ranges.length);
						ranges = ranges.next;
					}
					file.close();
					file = null;
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}

			protected void finalize()
			{
				close();
			}
		}

		// -------------------------------------------------------------------------

		private class Range
		{
			private long offset;
			private long length;
			private Range next;

			public Range(long offset, long length)
			{
				this.offset = offset;
				this.length = length;
			}

			public boolean lock(long noff, long nlen)
			{
				long last = offset + length;
				long noff2 = noff + nlen;
				if ((noff >= offset && noff <= last) || (noff2 >= offset && noff2 <= last))
				{
					return false;
				}
				if (next == null)
				{
					next = new Range(noff, nlen);
					return true;
				}
				else
				{
					return next.lock(noff, nlen);
				}
			}

			public boolean unlock(long noff, long nlen)
			{
				return (noff == offset && nlen == length);
			}
		}
	}
}

