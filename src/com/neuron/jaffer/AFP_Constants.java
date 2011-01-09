/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.math.BigInteger;

// directory id rules (!0, 1=root parent, 2=root) : IA p. 342

public interface AFP_Constants
{
	// default listening port
	public final static int  TCP_PORT               = 548;
	public final static long MAX_SHORT_FORK_LEN     = 0xFFFFFFFFL;

	public final static String[] COMMAND = {
		"",                      //   ----   (0)   invalid
		"BYTE_RANGE_LOCK",       //   0x01   (1)
		"CLOSE_VOL",             //   0x02   (2)
		"CLOSE_DIR",             //   0x03   (3)
		"CLOSE_FORK",            //   0x04   (4)
		"COPY_FILE",             //   0x05   (5)
		"CREATE_DIR",            //   0x06   (6)
		"CREATE_FILE",           //   0x07   (7)
		"DELETE",                //   0x08   (8)
		"ENUMERATE",             //   0x09   (9)
		"FLUSH",                 //   0x0a   (10)
		"FLUSH_FORK",            //   0x0b   (11)
		null,                    //   ----   (12)  invalid
		null,                    //   ----   (13)  invalid
		"GET_FORK_PARMS",        //   0x0e   (14)
		"GET_SRVR_INFO",         //   0x0f   (15)
		"GET_SRVR_PARMS",        //   0x10   (16)
		"GET_VOL_PARMS",         //   0x11   (17)
		"LOGIN",                 //   0x12   (18)
		"LOGIN_CONT",            //   0x13   (19)
		"LOGOUT",                //   0x14   (20)
		"MAP_ID",                //   0x15   (21)
		"MAP_NAME",              //   0x16   (22)
		"MOVE_AND_RENAME",       //   0x17   (23)
		"OPEN_VOL",              //   0x18   (24)
		"OPEN_DIR",              //   0x19   (25)
		"OPEN_FORK",             //   0x1a   (26)
		"READ",                  //   0x1b   (27)
		"RENAME",                //   0x1c   (28)
		"SET_DIR_PARMS",         //   0x1d   (29)
		"SET_FILE_PARMS",        //   0x1e   (30)
		"SET_FORK_PARMS",        //   0x1f   (31)
		"SET_VOL_PARMS",         //   0x20   (32)
		"WRITE",                 //   0x21   (33)
		"GET_FILE_DIR_PARMS",    //   0x22   (34)
		"SET_FILE_DIR_PARMS",    //   0x23   (35)
		"CHANGE_PASSWORD",       //   0x24   (36)
		"GET_USER_INFO",         //   0x25   (37)
		"GET_SRVR_MSG",          //   0x26   (38)
		"CREATE_ID",             //   0x27   (39)
		"DELETE_ID",             //   0x28   (40)
		"RESOLVE_ID",            //   0x29   (41)
		"EXCHANGE_FILES",        //   0x2a   (42)
		"CAT_SEARCH",            //   0x2b   (43)
		null,                    //   ----   (44)  invalid
		null,                    //   ----   (45)  invalid
		null,                    //   ----   (46)  invalid
		null,                    //   ----   (47)  invalid
		"OPEN_DT",               //   0x30   (48)
		"CLOSE_DT",              //   0x31   (49)
		null,                    //   ----   (50)  invalid
		"GET_ICON",              //   0x33   (51)
		"GET_ICON_INFO",         //   0x34   (52)
		"ADD_APPL",              //   0x35   (53)
		"RMV_APPL",              //   0x36   (54)
		"GET_APPL",              //   0x37   (55)
		"ADD_COMMENT",           //   0x38   (56)
		"RMV_COMMENT",           //   0x39   (57)
		"GET_COMMENT",           //   0x3a   (58)
		"BYTE_RANGE_LOCK_EXT",   //   0x3b   (59)
		"READ_EXT",              //   0x3c   (60)
		null,                    //          (61)
		null,                    //          (62)
		"LOGIN_EXT",             //   0x3F   (63)
		"GET_SESSION_TOKEN",     //   0x40   (64)
		null,                    //          (63)
		"ENUMERATE_EXT",         //   0x42   (66)
		null,                    //          (67)
		"ENUMERATE_EXT2",        //   0x44   (68)
		null,                    //   0x     (  )
		null,                    //   0x     (  )
		null,                    //   0x     (  )
		null,                    //   0x     (  )
		null,                    //   0x     (  )
	};

	// Command Set
	public final static int CMD_BYTE_RANGE_LOCK     = 0x01; // (1)
	public final static int CMD_CLOSE_VOL           = 0x02; // (2)
	public final static int CMD_CLOSE_DIR           = 0x03; // (3)
	public final static int CMD_CLOSE_FORK          = 0x04; // (4)
	public final static int CMD_COPY_FILE           = 0x05; // (5)
	public final static int CMD_CREATE_DIR          = 0x06; // (6)
	public final static int CMD_CREATE_FILE         = 0x07; // (7)
	public final static int CMD_DELETE              = 0x08; // (8)
	public final static int CMD_ENUMERATE           = 0x09; // (9)  AFP3.1 p. 150
	public final static int CMD_FLUSH               = 0x0a; // (10)
	public final static int CMD_FLUSH_FORK          = 0x0b; // (11)
	public final static int CMD_GET_FORK_PARMS      = 0x0e; // (14)
	public final static int CMD_GET_SRVR_INFO       = 0x0f; // (15)
	public final static int CMD_GET_SRVR_PARMS      = 0x10; // (16)
	public final static int CMD_GET_VOL_PARMS       = 0x11; // (17)
	public final static int CMD_LOGIN               = 0x12; // (18)
	public final static int CMD_LOGIN_CONT          = 0x13; // (19)
	public final static int CMD_LOGOUT              = 0x14; // (20)
	public final static int CMD_MAP_ID              = 0x15; // (21)
	public final static int CMD_MAP_NAME            = 0x16; // (22)
	public final static int CMD_MOVE_AND_RENAME     = 0x17; // (23)
	public final static int CMD_OPEN_VOL            = 0x18; // (24)
	public final static int CMD_OPEN_DIR            = 0x19; // (25)
	public final static int CMD_OPEN_FORK           = 0x1a; // (26)
	public final static int CMD_READ                = 0x1b; // (27)
	public final static int CMD_RENAME              = 0x1c; // (28)
	public final static int CMD_SET_DIR_PARMS       = 0x1d; // (29)
	public final static int CMD_SET_FILE_PARMS      = 0x1e; // (30)
	public final static int CMD_SET_FORK_PARMS      = 0x1f; // (31)
	public final static int CMD_SET_VOL_PARMS       = 0x20; // (32)
	public final static int CMD_WRITE               = 0x21; // (33)
	public final static int CMD_GET_FILE_DIR_PARMS  = 0x22; // (34) AFP31 p. 179
	public final static int CMD_SET_FILE_DIR_PARMS  = 0x23; // (35)
	public final static int CMD_CHANGE_PASSWORD     = 0x24; // (36)
	public final static int CMD_GET_USER_INFO       = 0x25; // (37) AFP31 p. 204
	public final static int CMD_GET_SRVR_MSG        = 0x26; // (38)
	public final static int CMD_CREATE_ID           = 0x27; // (39)
	public final static int CMD_DELETE_ID           = 0x28; // (40)
	public final static int CMD_RESOLVE_ID          = 0x29; // (41)
	public final static int CMD_EXCHANGE_FILES      = 0x2a; // (42)
	public final static int CMD_CAT_SEARCH          = 0x2b; // (43)
	public final static int CMD_OPEN_DT             = 0x30; // (48)
	public final static int CMD_CLOSE_DT            = 0x31; // (49)
	public final static int CMD_GET_ICON            = 0x33; // (51)
	public final static int CMD_GET_ICON_INFO       = 0x34; // (52)
	public final static int CMD_ADD_APPL            = 0x35; // (53)
	public final static int CMD_RMV_APPL            = 0x36; // (54)
	public final static int CMD_GET_APPL            = 0x37; // (55)
	public final static int CMD_ADD_COMMENT         = 0x38; // (56)
	public final static int CMD_RMV_COMMENT         = 0x39; // (57)
	public final static int CMD_GET_COMMENT         = 0x3a; // (58)
	public final static int CMD_BYTE_RANGE_LOCK_EXT = 0x3b; // (59) AFP31 p. 106
	public final static int CMD_READ_EXT            = 0x3c; // (60) AFP31 p. 242
	public final static int CMD_WRITE_EXT           = 0x3d; // (61) AFP31 p. 274
	public final static int CMD_LOGIN_EXT           = 0x3F; // (63) AFP31 p. 215
	public final static int CMD_GET_SESSION_TOKEN   = 0x40; // (64) AFP31 p. 191
	public final static int CMD_ENUMERATE_EXT       = 0x42; // (66) AFP31 p. 155
	public final static int CMD_ENUMERATE_EXT2      = 0x44; // (68) AFP31 p. 160
	public final static int CMD_FPZZZ               = 0x7a; // (122) undoc'd, sleep tickle, ret max time b4 client timeout
	public final static int CMD_ADD_ICON            = 0xc0; // (192)

	// Error Codes
	public final static int ERR_NO_ERR              = 0x00000000; // (0)
	public final static int ERR_ACCESS_DENIED       = 0xffffec78; // (-5000)
	public final static int ERR_AUTH_CONTINUE       = 0xffffec77; // (-5001)
	public final static int ERR_BAD_UAM             = 0xffffec76; // (-5002)
	public final static int ERR_BAD_VERS_NUM        = 0xffffec75; // (-5003)
	public final static int ERR_BITMAP_ERR          = 0xffffec74; // (-5004)
	public final static int ERR_CANT_MOVE           = 0xffffec73; // (-5005)
	public final static int ERR_DENY_CONFLICT       = 0xffffec72; // (-5006)
	public final static int ERR_DIR_NOT_EMPTY       = 0xffffec71; // (-5007)
	public final static int ERR_DISK_FULL           = 0xffffec70; // (-5008)
	public final static int ERR_EOF_ERR             = 0xffffec6f; // (-5009)
	public final static int ERR_FILE_BUSY           = 0xffffec6e; // (-5010)
	public final static int ERR_FLAT_VOL            = 0xffffec6d; // (-5011)
	public final static int ERR_ITEM_NOT_FOUND      = 0xffffec6c; // (-5012)
	public final static int ERR_LOCK_ERR            = 0xffffec6b; // (-5013)
	public final static int ERR_MISC_ERR            = 0xffffec6a; // (-5014)
	public final static int ERR_NO_MORE_LOCKS       = 0xffffec69; // (-5015)
	public final static int ERR_NO_SERVER           = 0xffffec68; // (-5016)
	public final static int ERR_OBJECT_EXISTS       = 0xffffec67; // (-5017)
	public final static int ERR_OBJECT_NOT_FOUND    = 0xffffec66; // (-5018)
	public final static int ERR_PARAM_ERR           = 0xffffec65; // (-5019)
	public final static int ERR_RANGE_NOT_LOCKED    = 0xffffec64; // (-5020)
	public final static int ERR_RANGE_OVERLAP       = 0xffffec63; // (-5021)
	public final static int ERR_SESS_CLOSED         = 0xffffec62; // (-5022)
	public final static int ERR_USER_NOT_AUTH       = 0xffffec61; // (-5023)
	public final static int ERR_CALL_NOT_SUPPORTED  = 0xffffec60; // (-5024)
	public final static int ERR_OBJECT_TYPE_ERR     = 0xffffec5f; // (-5025)
	public final static int ERR_TOO_MANY_FILES_OPEN = 0xffffec5e; // (-5026)
	public final static int ERR_SERVER_GOING_DOWN   = 0xffffec5d; // (-5027)
	public final static int ERR_CANT_RENAME         = 0xffffec5c; // (-5028)
	public final static int ERR_DIR_NOT_FOUND       = 0xffffec5b; // (-5029)
	public final static int ERR_ICON_TYPE_ERROR     = 0xffffec5a; // (-5030)
	public final static int ERR_VOL_LOCKED          = 0xffffec59; // (-5031)
	public final static int ERR_OBJECT_LOCKED       = 0xffffec58; // (-5032)

	// Volume Signature Values (ref: Inside Appletalk p. 337)
	public final static int VOL_SIG_FLAT            = 0x01; // flat
	public final static int VOL_SIG_FIXED           = 0x02; // tree
	public final static int VOL_SIG_VARIABLE        = 0x03; // tree

	// Volume Bitmap (ref: AFP31 p. 284)
	public final static int VOL_BIT_ATTRIBUTE       = 0x001;
	public final static int VOL_BIT_SIGNATURE       = 0x002;
	public final static int VOL_BIT_CREATE_DATE     = 0x004;
	public final static int VOL_BIT_MOD_DATE        = 0x008;
	public final static int VOL_BIT_BACKUP_DATE     = 0x010;
	public final static int VOL_BIT_ID              = 0x020;
	public final static int VOL_BIT_BYTES_FREE      = 0x040;
	public final static int VOL_BIT_BYTES_TOTAL     = 0x080;
	public final static int VOL_BIT_NAME            = 0x100;
	public final static int VOL_BIT_XBYTES_FREE     = 0x200;
	public final static int VOL_BIT_XBYTES_TOTAL    = 0x400;
	public final static int VOL_BIT_BLOCK_SIZE      = 0x800;

	// Volume Attributes (ref: AFP31 p. 21)
	public final static int VOL_ATTR_READONLY       = 0x01;
	public final static int VOL_ATTR_PASSWORD       = 0x02;
	public final static int VOL_ATTR_FILE_IDS       = 0x04;
	public final static int VOL_ATTR_CAT_SEARCH     = 0x08;
	public final static int VOL_ATTR_BLANK_PRIVS    = 0x10;
	public final static int VOL_ATTR_UNIX_PRIVS     = 0x20;
	public final static int VOL_ATTR_UTF8_NAMES     = 0x40;
	public final static int VOL_ATTR_NO_NET_UIDS    = 0x80;

	// Directory Bitmap (ref: AFP31 p. 277)
	public final static int DIR_BIT_ATTRIBUTE       = 0x0001;
	public final static int DIR_BIT_PARENT_DIR_ID   = 0x0002;
	public final static int DIR_BIT_CREATE_DATE     = 0x0004;
	public final static int DIR_BIT_MOD_DATE        = 0x0008;
	public final static int DIR_BIT_BACKUP_DATE     = 0x0010;
	public final static int DIR_BIT_FINDER_INFO     = 0x0020;
	public final static int DIR_BIT_LONG_NAME       = 0x0040;
	public final static int DIR_BIT_SHORT_NAME      = 0x0080;
	public final static int DIR_BIT_NODE_ID         = 0x0100;
	public final static int DIR_BIT_OFFSPRING_COUNT = 0x0200;
	public final static int DIR_BIT_OWNER_ID        = 0x0400;
	public final static int DIR_BIT_GROUP_ID        = 0x0800;
	public final static int DIR_BIT_ACCESS_RIGHTS   = 0x1000;
	public final static int DIR_BIT_UTF8_NAME       = 0x2000;
	public final static int DIR_BIT_UNIX_PRIVS      = 0x8000;

	// Directory Attributes (ref: AFP31 p. 279)
	public final static int DIR_ATTR_INVISIBLE      = 0x0001;
	public final static int DIR_ATTR_IS_EXP_FOLDER  = 0x0002;
	public final static int DIR_ATTR_SYSTEM         = 0x0004;
	public final static int DIR_ATTR_MOUNTED        = 0x0008;
	public final static int DIR_ATTR_IN_EXP_FOLDER  = 0x0010;
	public final static int DIR_ATTR_BACKUP_NEEDED  = 0x0040;
	public final static int DIR_ATTR_RENAME_INHIBIT = 0x0080;
	public final static int DIR_ATTR_DELETE_INHIBIT = 0x0100;
	public final static int DIR_ATTR_SET_CLEAR      = 0x8000;

	// File Bitmap (ref: AFP31 p. 279)
	public final static int FILE_BIT_ATTRIBUTE      = 0x0001;
	public final static int FILE_BIT_PARENT_DIR_ID  = 0x0002;
	public final static int FILE_BIT_CREATE_DATE    = 0x0004;
	public final static int FILE_BIT_MOD_DATE       = 0x0008;
	public final static int FILE_BIT_BACKUP_DATE    = 0x0010;
	public final static int FILE_BIT_FINDER_INFO    = 0x0020;
	public final static int FILE_BIT_LONG_NAME      = 0x0040;
	public final static int FILE_BIT_SHORT_NAME     = 0x0080;
	public final static int FILE_BIT_NODE_ID        = 0x0100;
	public final static int FILE_BIT_DATA_FORK_LEN  = 0x0200;
	public final static int FILE_BIT_RSRC_FORK_LEN  = 0x0400;
	public final static int FILE_BIT_XDATA_FORK_LEN = 0x0800;
	public final static int FILE_BIT_LAUNCH_LIMIT   = 0x1000;
	public final static int FILE_BIT_UTF8_NAME      = 0x2000;
	public final static int FILE_BIT_XRSRC_FORK_LEN = 0x4000;
	public final static int FILE_BIT_UNIX_PRIVS     = 0x8000;

	// File Attributes (ref: AFP31 p. 279)
	public final static int FILE_ATTR_INVISIBLE      = 0x0001;
	public final static int FILE_ATTR_MULTIUSER      = 0x0002;
	public final static int FILE_ATTR_SYSTEM         = 0x0004;
	public final static int FILE_ATTR_DALREADY_OPEN  = 0x0008;
	public final static int FILE_ATTR_RALREADY_OPEN  = 0x0010;
	public final static int FILE_ATTR_WRITE_INHIBIT  = 0x0020;
	public final static int FILE_ATTR_BACKUP_NEEDED  = 0x0040;
	public final static int FILE_ATTR_RENAME_INHIBIT = 0x0080;
	public final static int FILE_ATTR_DELETE_INHIBIT = 0x0100;
	public final static int FILE_ATTR_COPY_PROTECT   = 0x0400;
	public final static int FILE_ATTR_SET_CLEAR      = 0x8000;

	// Access Rights Bitmap (ref: AFP31 p. 277)
	public final static int ACCESS_OWNER_SEARCH      = 0x00000001;
	public final static int ACCESS_OWNER_READ        = 0x00000002;
	public final static int ACCESS_OWNER_WRITE       = 0x00000004;
	public final static int ACCESS_GROUP_SEARCH      = 0x00000100;
	public final static int ACCESS_GROUP_READ        = 0x00000200;
	public final static int ACCESS_GROUP_WRITE       = 0x00000400;
	public final static int ACCESS_ALL_SEARCH        = 0x00010000;
	public final static int ACCESS_ALL_READ          = 0x00020000;
	public final static int ACCESS_ALL_WRITE         = 0x00040000;
	public final static int ACCESS_UA_SEARCH         = 0x01000000;
	public final static int ACCESS_UA_READ           = 0x02000000;
	public final static int ACCESS_UA_WRITE          = 0x04000000;
	public final static int ACCESS_UA_BLANK          = 0x10000000;
	public final static int ACCESS_UA_OWNER          = 0x80000000;

	public final static int ACCESS_EVERYTHING        = 0x97070707;
//	public final static int ACCESS_EVERYTHING        = 0x87000007;

	// User Authentication Methods
	public final static int UAM_UNKNOWN              = 0;
	public final static int UAM_GUEST                = 1;
	public final static int UAM_CLEARTEXT            = 2;
	public final static int UAM_RANDOM_NUM1          = 3;
	public final static int UAM_RANDOM_NUM2          = 4;
	public final static int UAM_DHX_128              = 5;
	public final static int UAM_DHX_DYNAMIC          = 6;
	public final static int UAM_KERBEROS             = 7;
                                                    
	public final static String UAM_STR_GUEST         = "No User Authent";
	public final static String UAM_STR_CLEARTEXT     = "Cleartxt passwrd";
	public final static String UAM_STR_RANDOM_NUM1   = "Randum Exchange";
	public final static String UAM_STR_RANDOM_NUM2   = "2-Way Randum";
	public final static String UAM_STR_DHX_128       = "DHCAST128";
	public final static String UAM_STR_DHX_DYNAMIC   = "DHX2";
	public final static String UAM_STR_KERBEROS      = "Client Krb v2";

	// Server Capability Flags
	public final static int CAP_COPY_FILE            = 0x0001;
	public final static int CAP_CHANGE_PASSWORD      = 0x0002;
	public final static int CAP_NO_PASSWD_SAVE       = 0x0004;
	public final static int CAP_MESSAGES             = 0x0008;
	public final static int CAP_SIGNATURE            = 0x0010;
	public final static int CAP_TCPIP                = 0x0020;
	public final static int CAP_NOTIFICATIONS        = 0x0040;
	public final static int CAP_RECONNECT            = 0x0080;
	public final static int CAP_OPEN_DIRECTORY       = 0x0100;
	public final static int CAP_UTF8_NAME            = 0x0200;
	public final static int CAP_SUPER_CLIENT         = 0x8000;

	// Diffie-Hellman Key Exchange Constants
	public final static BigInteger DHX_G = new BigInteger(1, new byte[] { (byte)0x07 });
	public final static BigInteger DHX_P = new BigInteger(1, new byte[] {
		(byte)0xba, (byte)0x28, (byte)0x73, (byte)0xdf,
		(byte)0xb0, (byte)0x60, (byte)0x57, (byte)0xd4,
		(byte)0x3f, (byte)0x20, (byte)0x24, (byte)0x74,
		(byte)0x4c, (byte)0xee, (byte)0xe7, (byte)0x5b
	});
}

