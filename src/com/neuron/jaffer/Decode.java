/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

import java.io.IOException;

class Decode extends Utility implements AFP_Constants, DSI_Constants
{
	private final static int     BYTE              = 0;
	private final static int     SHORT             = 1;
	private final static int     INTEGER           = 2;
	private final static int     LONG              = 3;
	private final static int     PSTRING           = 4;
	private final static int     AFPSTRING         = 5;
	private final static int     TYPEDSTRING       = 6;

	private final static Integer I_BYTE            = new Integer(BYTE);
	private final static Integer I_SHORT           = new Integer(SHORT);
	private final static Integer I_INTEGER         = new Integer(INTEGER);
	private final static Integer I_LONG            = new Integer(LONG);
	private final static Integer I_PSTRING         = new Integer(PSTRING);
	private final static Integer I_AFPSTRING       = new Integer(AFPSTRING);
	private final static Integer I_TYPEDSTRING     = new Integer(TYPEDSTRING);

	public static void afpRequest(byte b[], int cmd)
		throws IOException
	{
		switch (cmd)
		{
			case CMD_LOGIN:
				raw(b, new Object[][] {
					{ I_BYTE,        "Command"        },
					{ I_PSTRING,     "AFPVersion"     },
					{ I_PSTRING,     "UAM"            },
				});
				break;
			case CMD_LOGIN_CONT:
				raw(b, new Object[][] {
					{ I_BYTE,        "Command"        },
					{ I_BYTE,        "Pad"            },
					{ I_SHORT,       "ID"             },
				});
				break;
			case CMD_LOGIN_EXT:
				raw(b, new Object[][] {
					{ I_BYTE,        "Command"        },
					{ I_BYTE,        "Pad"            },
					{ I_SHORT,       "Flags"          },
					{ I_PSTRING,     "AFPVersion"     },
					{ I_PSTRING,     "UAM"            },
					{ I_TYPEDSTRING, "UserName"       },
					{ I_TYPEDSTRING, "PathName"       },
				});
				break;
			case CMD_GET_USER_INFO:
				raw(b, new Object[][] {
					{ I_BYTE,        "Command"        },
					{ I_BYTE,        "Pad"            },
					{ I_INTEGER,     "UserID",        },
					{ I_SHORT,       "Bitmap",        },
				});
				break;
			case CMD_OPEN_VOL:
				raw(b, new Object[][] {
					{ I_BYTE,        "Command"        },
					{ I_BYTE,        "Pad"            },
					{ I_SHORT,       "Bitmap",        },
					{ I_PSTRING,     "VolumeName"     },
				});
				break;
			case CMD_GET_FILE_DIR_PARMS:
				raw(b, new Object[][] {
					{ I_BYTE,        "Command"        },
					{ I_BYTE,        "Pad"            },
					{ I_SHORT,       "VolumeID"       },
					{ I_INTEGER,     "DirectoryID"    },
					{ I_SHORT,       "FileBitmap"     },
					{ I_SHORT,       "DirBitmap"      },
					{ I_TYPEDSTRING, "PathName"       },
				});
				break;
		}
	}

	public static void raw(byte b[], Object rule[][])
		throws IOException
	{
		ByteReader rr = new ByteReader(b);
		for (int i=0; i<rule.length; i++)
		{
			String label = (String)rule[i][1];
			System.out.print("=> ("+rr.getPosition()+") "+label+" = ");
			switch (((Integer)rule[i][0]).intValue())
			{
				case BYTE:
					System.out.println(hex(rr.readUnsignedByte()));
					break;
				case SHORT:
					System.out.println(hex(rr.readUnsignedShort()));
					break;
				case INTEGER:
					System.out.println(hex(rr.readInt()));
					break;
				case LONG:
					System.out.println(hex(rr.readLong()));
					break;
				case PSTRING:
					System.out.println("'"+rr.readPString()+"'");
					break;
				case TYPEDSTRING:
					System.out.println("'"+rr.readTypedString()+"'");
					break;
			}
		}
	}
}

