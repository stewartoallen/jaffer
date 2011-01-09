/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

class FinderInfo
{
	public final static int FLAG_ON_DESK     = 0x0001; // unused/reserved
	public final static int FLAG_COLOR       = 0x000e; // 3 color bits
	public final static int FLAG_SHARED      = 0x0040; // can be executed by mutiple users
	public final static int FLAG_INITIALIZED = 0x0100; // file info is in desktop database
	public final static int FLAG_CUSTOM_ICON = 0x0400; // has a custom icon
	public final static int FLAG_STATIONARY  = 0x0800; // file is stationary pad
	public final static int FLAG_NAME_LOCKED = 0x1000; // name/icon cannot be changed
	public final static int FLAG_HAS_BUNDLE  = 0x2000; // has resource bundle
	public final static int FLAG_INVISIBLE   = 0x4000; // is invisible
	public final static int FLAG_ALIAS       = 0x000e; // is an alias file

	/*
	// Finder info for Files

	private int  type_file;      // 4 bytes
	private int  type_creator;   // 4 bytes
	private int  finder_flags;   // 2 bytes
	private int  file_loc;       // 4 bytes (position in window)
	private int  dir_id;         // 2 bytes (id of parent directory)
                
	private int  icon_id;        // 2 bytes
	private long unused;         // 6 bytes
	private int  script_flags;   // 1 byte
	private int  finder_xflags;  // 1 byte  (unused)
	private int  comment_id;     // 2 bytes
	private int  homedir_id;     // 4 bytes

	// Finder info for Directories

	private long folder_bounds;  // 8 bytes (window's rectangle)
	private int  dir_flags;      // 2 bytes
	private int  folder_loc;     // 4 bytes
	private int  folder_view;    // 2 bytes (position in window)
                
	private int  scroll_pos;     // 4 bytes
	private int  open_chain_id;  // 4 bytes
	private int  script_flags;   // 1 byte
	private int  finder_xflags;  // 1 byte  (unused)
	private int  comment_id;     // 2 bytes
	private int  homedir_id;     // 4 bytes
	*/
}

