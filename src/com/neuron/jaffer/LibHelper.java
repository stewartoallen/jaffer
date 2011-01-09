/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

final class LibHelper
{
	final static boolean load(String lib, String jarpath)
	{
		try
		{
			Runtime.getRuntime().loadLibrary(lib);
			return true;
		}
		catch (Throwable ex) { }
		try
		{
			URL u = LibHelper.class.getProtectionDomain().getCodeSource().getLocation();
			InputStream is = u.openStream();
			JarInputStream jar = new JarInputStream(is);
			JarEntry next = null;
			while ((next = jar.getNextJarEntry()) != null)
			{
				if (next.getName().equals(jarpath))
				{
					break;
				}
			}
			if (next == null)
			{
				return false;
			}
			File tmp = File.createTempFile("jaffer-","-lib");
			tmp.deleteOnExit();
			FileOutputStream os = new FileOutputStream(tmp);
			byte b[] = new byte[4096];
			int read = 0;
			while ( (read = jar.read(b)) > 0)
			{
				os.write(b, 0, read);
			}
			os.close();
			if (tmp != null)
			{
				Runtime.getRuntime().load(tmp.getAbsolutePath());
			}
			return true;
		}
		catch (Throwable ex)
		{
			ex.printStackTrace();
			return false;
		}
	}

	public static String unURL(String url)
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<url.length(); i++)
		{
			if (url.charAt(i) == '%' && i<url.length()-3)
			{
				sb.append((char)Integer.parseInt(url.substring(i+1, i+3),16));
				i += 2;
			}
			else
			{
				sb.append(url.charAt(i));
			}
		}
		return sb.toString();
	}
}

