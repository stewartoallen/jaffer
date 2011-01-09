/* 
 * Copyright (c) 2003 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

// Reference: Appletalk Filing Protocol Version 2.1 and 2.2
public interface DSI_Constants
{
	public final static int DSI_REQUEST         = 0x00;
	public final static int DSI_REPLY           = 0x01;
                                               
	public final static int OPT_SERV_QUANT      = 0x00;
	public final static int OPT_ATTN_QUANT      = 0x01;
                                               
	public final static int CMD_CLOSE_SESSION   = 0x01; // client & server
	public final static int CMD_COMMAND         = 0x02; // client
	public final static int CMD_GET_STATUS      = 0x03; // client
	public final static int CMD_OPEN_SESSION    = 0x04; // client
	public final static int CMD_TICKLE          = 0x05; // client & server
	public final static int CMD_WRITE           = 0x06; // client
	public final static int CMD_ATTENTION       = 0x08; // server

	public final static String[] COMMAND = {
		null,
		"CLOSE_SESSION",
		"COMMAND",
		"GET_STATUS",
		"OPEN_SESSION",
		"TICKLE",
		"WRITE",
		null,
		"ATTENTION"
	};
}

