/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.util.Calendar;

public abstract class Utility
{
	private static long timeOffset;

	static
	{
		Calendar c = Calendar.getInstance();
		c.set(2000, 0, 1, 0, 0, 0);
		timeOffset = c.getTime().getTime();
	}

	private final static byte hex[] = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	public final static String readCString(byte d[], int off)
	{
		int pos = off;
		while (d[pos] != 0)
		{
			pos++;
			if (pos > d.length)
			{
				return null;
			}
		}
		return new String(d, off, pos-off);
	}

	public final static String readPString(byte d[], int off)
	{
		int len = d[off++] & 0xff;
		return new String(d, off, len);
	}

	public final static String readAFPString(byte d[], int off)
	{
		int len = ((d[off] & 0xff) << 8) | (d[off+1] & 0xff);
		return new String(d, off+2, len);
	}

	public final static void writePString(byte d[], int off, String str)
	{
		d[off] = (byte)str.length();
		System.arraycopy(str.getBytes(), 0, d, off+1, str.length());
	}

	public final static String[] readPStringArray(byte d[], int off)
	{
		String s[] = new String[d[off++]&0xff];
		for (int i=0; i<s.length; i++)
		{
			s[i] = readPString(d,off);
			off += s[i].length() + 1;
		}
		return s;
	}

	public final static void writePStringArray(byte d[], int off, String s[])
	{
		d[off++] = (byte)s.length;
		for (int i=0; i<s.length; i++)
		{
			writePString(d, off, s[i]);
			off += s[i].length() + 1;
		}
	}

	public final static int readInt2(byte b[], int off)
	{
		return ( ( (b[off+0]&0xff)<<8 ) | (b[off+1]&0xff) );
	}

	public final static int readInt4(byte b[], int off)
	{
		return (
			( (b[off+0]&0xff)<<24 ) | ( (b[off+1]&0xff)<<16 ) | 
			( (b[off+2]&0xff)<< 8 ) | ( (b[off+3]&0xff)<< 0 )
		);
	}

	public final static long readInt8(byte b[], int off)
	{
		return (
			( (b[off+0]&0xffl)<<56 ) | ( (b[off+1]&0xffl)<<48 ) | 
			( (b[off+2]&0xffl)<<40 ) | ( (b[off+3]&0xffl)<<32 ) | 
			( (b[off+4]&0xffl)<<24 ) | ( (b[off+5]&0xffl)<<16 ) | 
			( (b[off+6]&0xffl)<< 8 ) | ( (b[off+7]&0xffl)<< 0 )
		);
	}

	public final static void writeInt2(byte b[], int off, int val)
	{
		b[off+0] = (byte)((val >>> 8) & 0xff);
		b[off+1] = (byte)((val >>> 0) & 0xff);
	}

	public final static void writeInt4(byte b[], int off, int val)
	{
		b[off+0] = (byte)((val >>> 24) & 0xff);
		b[off+1] = (byte)((val >>> 16) & 0xff);
		b[off+2] = (byte)((val >>>  8) & 0xff);
		b[off+3] = (byte)((val >>>  0) & 0xff);
	}

	public final static void writeInt8(byte b[], int off, long val)
	{
		b[off+0] = (byte)((val >>> 56) & 0xff);
		b[off+1] = (byte)((val >>> 48) & 0xff);
		b[off+2] = (byte)((val >>> 40) & 0xff);
		b[off+3] = (byte)((val >>> 32) & 0xff);
		b[off+4] = (byte)((val >>> 24) & 0xff);
		b[off+5] = (byte)((val >>> 16) & 0xff);
		b[off+6] = (byte)((val >>>  8) & 0xff);
		b[off+7] = (byte)((val >>>  0) & 0xff);
	}

	public final static String hex(int v)
	{
		return "0x"+Integer.toHexString(v);
	}

	public final static String hex(long v)
	{
		return "0x"+Long.toHexString(v);
	}

	public final static String hex(byte b[])
	{
		StringBuffer sb = new StringBuffer("["+b.length+"] ");
		for (int i=0; i<b.length; i++)
		{
			sb.append((i>0 ? "," : "")+Integer.toHexString(b[i]&0xff));
		}
		return sb.toString();
	}

	public final static String bits(int value, int c)
	{
		StringBuffer sb = new StringBuffer();
		for (int i=c-1; i>=0; i--)
		{
			int bv = (value >>> (8*i)) & 0xff;
			sb.append('[');
			for (int j=7; j>=0; j--)
			{
				sb.append((bv & (1<<j)) > 0 ? '1' : '0');
			}
			sb.append(']');
		}
		return sb.toString();
	}

	public final static String list(String s[])
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<s.length; i++)
		{
			sb.append((i>0 ? "," : "")+s[i]);
		}
		return sb.toString();
	}

	public final static void dump(byte b[])
	{
		dump(b, b.length);
	}

	public final static void dump(String prefix, byte b[])
	{
		dump(prefix, b, b.length);
	}

	public final static void dump(byte b[], int len)
	{
		dump("+ ", b, len);
	}

	public final static void dump(String prefix, byte b[], int len)
	{
		len = Math.min(len, b.length);
		byte line[] = new byte[75];
		for (int i=0; i<75; i++) { line[i] = ' '; }
		for (int i=0; i<len; i+=16)
		{
			for (int j=0; j<16; j++)
			{
				if (i+j < len)
				{
					int d = b[i+j] & 0xff;
					line[j*3+0] = hex[d/16];
					line[j*3+1] = hex[d%16];
					line[j*3+2] = (byte)' ';
					line[j+51]  = (byte)(d>=32 && d<=126 ? d : '.');
				}
				else
				{
					line[j*3+0] = ' ';
					line[j*3+1] = ' ';
					line[j*3+2] = ' ';
					line[j+51]  = ' ';
				}
			}
			System.out.println(prefix+new String(line));
		}
	}

	public final static boolean hasBits(int flags, int bits)
	{
		return (flags & bits) == bits;
	}

	// ref: Inside Appletalk p. 340
	public final static boolean isValidLongName(String name, boolean isRoot)
	{
		int max = isRoot ? 27 : 31;
		if (name.length() > max)
		{
			return false;
		}
		byte str[] = name.getBytes();
		for (int i=0; i<str.length; i++)
		{
			int v = str[i] & 0xff;
			if (v < 0x20 || v > 0x70 || v == ':' || v == 0)
			{
				return false;
			}
		}
		return true;
	}

	public final static boolean empty(String str)
	{
		return str == null || str.length() == 0;
	}

	public final static int unix2afpTime(long ut)
	{
		int ret = (int)((ut-timeOffset)/1000);
		return ret;
	}

	public final static long afp2unixTime(int at)
	{
		long val = (at*1000l)+timeOffset;
		return val;
	}

	public final static void error(String msg)
	{
		throw new RuntimeException(msg);
	}
}

