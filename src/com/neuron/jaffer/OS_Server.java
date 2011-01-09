/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.IOException;

public class OS_Server extends AFP_Server
{
	private static boolean loaded = false;

	static
	{
		String libname = System.getProperty("os.name");
		if (libname.indexOf(' ') >= 0)
		{
			StringBuffer sb = new StringBuffer(libname.length());
			for (int i=0; i<libname.length(); i++)
			{
				if (!Character.isWhitespace(libname.charAt(i)))
				{
					sb.append(libname.charAt(i));
				}
			}
			libname = sb.toString();
		}
		if (LibHelper.load(libname, "lib/lib"+libname+".so"))
		{
			loaded = true;
		}
		else
		{
			System.out.println("Unable to load Unix Helper. Disabling authentication.");
		}
	}

	public OS_Server(String rname, int port)
		throws IOException
	{
		super(rname, port);
	}

	public OS_Server(String rname, String bind, int port)
		throws IOException
	{
		super(rname, bind, port);
	}

	private String guest;

	public void setGuestUser(String user)
	{
		this.guest = user;
	}

	// AFP_Server abstract methods

	public boolean hasUser(String user)
	{
		//System.out.println("--> CHECK USER ("+user+") <--");
		if (user == null)
		{
			return false;
		}
		return loaded ? validUser(CString(user)) : true;
	}

	public boolean checkPassword(String user, String pass)
	{
		//System.out.println("--> CHECK AUTH ("+user+","+pass+") <--");
		if (user == null || pass == null)
		{
			return false;
		}
		return loaded ? validPassword(CString(user), CString(pass)) : true;
	}

	public boolean setThreadOwner(String user)
	{
		if (user == null)
		{
			return false;
		}
		return loaded ? switchUser(CString(user)) : true;
	}

	public boolean hasCleartextPasswords()
	{
		return false;
	}

	public String getPassword(String userName)
	{
		return null;
	}

	public String getGuestUser()
	{
		return guest;
	}

	// native helpers

	private static byte[] CString(String s)
	{
		return (s+"\0").getBytes();
	}

	private static native boolean validUser(byte u[])
		;
	
	private static native boolean validPassword(byte u[], byte p[])
		;

	private static native boolean switchUser(byte u[])
		;
}

