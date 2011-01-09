/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZIP_Volume extends AFP_Volume 
{

	public ZIP_Volume(String volname, ZipFile zip, String password) 
	{
		this.zip = zip;
		this.volname = volname;
		this.nodes = new HashMap<Integer,ZIPNode>();
		this.password = password;
		this.volNode = addNode(new VolumeNode());
		this.rootNode = addNode(new RootNode());
		for (Enumeration<? extends ZipEntry> es = zip.entries(); es.hasMoreElements();)
		{
			ZipEntry ze = es.nextElement();
			ZIPNode node = rootNode;
			StringTokenizer st = new StringTokenizer(ze.getName(), "/");
			while (st.hasMoreTokens()) 
			{
				String tok = st.nextToken();
				ZIPNode child = node.getChild(tok);
				if (child == null) 
				{
					if (st.hasMoreTokens())
					{
						child = addNode(new ZIPNode(node.getNodeID(), nodes.size()+16, tok, null));
					} 
					else 
					{
						child = addNode(new ZIPNode(node.getNodeID(), nodes.size()+16, tok, ze));
					}
					node.addChild(tok, child.getNodeID());
				}
				node = child;
			}
		}
	}
	
	private ZipFile zip;
	private String volname;
	private HashMap<Integer,ZIPNode> nodes;
	private ZIPNode volNode;
	private ZIPNode rootNode;
	private String password;
	
	private synchronized ZIPNode addNode(ZIPNode node) 
	{
		ZIPNode nnode = nodes.get(node.getNodeID());
		if (nnode == null) 
		{
			nodes.put(node.getNodeID(), node);
		} 
		else
		{
			node = nnode;
		}
		return node;
	}
	
	@Override
	public int getAttributes()
	{
		return 0;
	}

	@Override
	public int getBackupDate()
	{
		return 0;
	}

	@Override
	public int getBlockSize() 
	{
		return 0x1000;
	}

	@Override
	public int getBytesFree() 
	{
		return 0;
	}

	@Override
	public int getBytesTotal()
	{
		return 0;
	}

	@Override
	public synchronized ZIPNode getCNode(int id)
	{
		return nodes.get(id);
	}

	@Override
	public int getCreateDate()
	{
		return 0;
	}

	@Override
	public long getExtBytesFree() 
	{
		return 0;
	}

	@Override
	public long getExtBytesTotal() 
	{
		return 0;
	}

	@Override
	public int getModifiedDate()
	{
		return 0;
	}

	@Override
	public String getName() 
	{
		return volname;
	}

	@Override
	public String getPassword() 
	{
		return password;
	}

	@Override
	public int getSignature() 
	{
		return 0;
	}

	@Override
	public boolean hasUnixPrivs() 
	{
		return false;
	}

	@Override
	public void setAttributes(int attr) 
	{
	}

	@Override
	public void setBackupDate(int d) 
	{
	}

	@Override
	public void setModifiedDate(int d) 
	{
	}
	
	// -------------------------------------------------------------------------

	private class VolumeNode extends ZIPNode
	{
		VolumeNode()
		{
			super(0, 1, "noname", null);
		}

		public ZIPNode getChild(String name)
		{
			return name.equals(volname) ? rootNode : null;
		}
	}

	// -------------------------------------------------------------------------

	private class RootNode extends ZIPNode
	{
		RootNode()
		{
			super(1, 2, volname, null);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Implements nodes from a ZipFile
	 */
	private class ZIPNode extends AFP_CNode 
	{
		ZIPNode(int pid, int id, String name, ZipEntry entry) 
		{
			super(id);
			this.pid = pid;
			this.name = name;
			this.entry = entry;
			this.children = new HashMap<String, Integer>();
			assert(name != null);
		}

		public String toString() 
		{
			return getNodeID()+":"+name+":"+entry;
		}
		
		private int pid;
		private String name;
		private ZipEntry entry;
		private HashMap<String, Integer> children;
		
		protected void addChild(String name, Integer id) 
		{
			children.put(name, id);
		}
		
		@Override
		public AFP_CNode createDirectory(String name) 
		{
			return null;
		}

		@Override
		public AFP_CNode createFile(String name) 
		{
			return null;
		}

		@Override
		public boolean delete() 
		{
			return false;
		}
		
		@Override
		public boolean isDirectory()
		{
			return entry == null || entry.isDirectory();
		}

		@Override
		public int getAccessRights()
		{
			final boolean read = true;
			final boolean write = false;
			return ACCESS_EVERYTHING ^ (
					(read  ? 0 : ACCESS_OWNER_READ  | ACCESS_GROUP_READ  | ACCESS_ALL_READ  | ACCESS_UA_READ ) |
					(write ? 0 : ACCESS_OWNER_WRITE | ACCESS_GROUP_WRITE | ACCESS_ALL_WRITE | ACCESS_UA_WRITE)
				);
		}

		@Override
		public int getAttributes() 
		{
			return 0;
		}

		@Override
		public int getBackupDate() 
		{
			return 0;
		}

		@Override
		public int getCreateDate() 
		{
			return 0;
		}

		@Override
		public long getDataForkLen()
		{
			return entry.getSize();
		}

		@Override
		public byte[] getFinderInfo() 
		{
			return null;
		}

		@Override
		public int getGroupID() 
		{
			return 0;
		}

		@Override
		public int getLaunchLimit() 
		{
			return 0;
		}

		@Override
		public String getLongName()
		{
			return name;
		}

		@Override
		public int getModifiedDate() 
		{
			return entry != null ? unix2afpTime(entry.getTime()) : 0;
		}

		@Override
		public ZIPNode getChild(String name) 
		{
			Integer id = children.get(name);
			return id == null ? null : getCNode(id);
		}

		@Override
		public Iterator<AFP_CNode> getChildren() 
		{
			return new Iterator<AFP_CNode>()
			{
				Iterator<String> keys = children.keySet().iterator();
				
				public boolean hasNext()
				{
					return keys.hasNext();
				}

				public ZIPNode next() 
				{
					return getChild(keys.next());
				}

				public void remove()
				{
				}				
			};
		}

		@Override
		public int getOwnerID()
		{
			return 0;
		}

		@Override
		public int getParentNodeID() 
		{
			return pid;
		}

		@Override
		public long getResourceForkLen() 
		{
			return 0;
		}

		@Override
		public String getShortName()
		{
			return name;
		}

		@Override
		public String getUTF8Name() 
		{
			return name;
		}

		@Override
		public byte[] getUnixPrivs() 
		{
			return null;
		}

		@Override
		public boolean moveTo(AFP_CNode dir, String newName) 
		{
			return false;
		}

		@Override
		public AFP_Fork openFileFork(int flags)
		{
			return new ZIPFork();
		}

		@Override
		public AFP_Fork openResourceFork(int flags)
		{
			return null;
		}

		@Override
		public void setAttributes(int att)
		{
		}

		@Override
		public void setBackupDate(int date)
		{
		}

		@Override
		public void setCreateDate(int date) 
		{
		}

		@Override
		public void setFinderInfo(byte[] info) 
		{
		}

		@Override
		public void setModifiedDate(int date) 
		{
		}

		@Override
		public void setUnixPrivs(byte[] privs) 
		{
		}

		// -------------------------------------------------------------------------

		/**
		 * Implements resource forks for zip entries
		 */
		class ZIPFork extends AFP_Fork 
		{
			@Override
			public void close() 
			{
			}

			@Override
			public void flush() throws IOException
			{
			}

			@Override
			public Type getForkType()
			{
				return Type.DATA;
			}

			@Override
			public long getLength() throws IOException 
			{
				return entry.getSize();
			}

			@Override
			public boolean lockRange(long offset, long length) 
			{
				return false;
			}

			@Override
			public void readRange(long offset, long length, ByteWriter ww) throws IOException 
			{
				InputStream in = zip.getInputStream(entry);
				in.skip(offset);
				ww.readFromInput(in,(int)length);
			}

			@Override
			public void setLength(long length) throws IOException 
			{
				throw new IOException("ReadOnly");
			}

			@Override
			public boolean unlockRange(long offset, long length)
			{
				return false;
			}

			@Override
			public long writeRange(long offset, long length, ByteReader rr) throws IOException 
			{
				throw new IOException("ReadOnly");
			}
		}
	}
}

