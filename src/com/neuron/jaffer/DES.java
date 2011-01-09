package com.neuron.jaffer;

public final class DES
{
	// public object api

	public DES(byte key[])
	{
		schedule = genSchedule(key);
	}

	public void encrypt(byte b[])
	{
		fencrypt(b, false, schedule);
	}

	public void decrypt(byte b[])
	{
		fencrypt(b, true, schedule);
	}

	private Schedule schedule;

	// public static methods

	public static void main(String args[])
	{
		byte key[]  = longToBytes(Long.parseLong(args[0], 16));
		byte data[] = longToBytes(Long.parseLong(args[1], 16));

		System.out.println("key="+args[0]+" data="+args[1]);
		printBytes("key", key);
		printBytes("data", data);

		DES des = new DES(key);

		des.encrypt(data);
		printBytes("encrypted", data);

		des.decrypt(data);
		printBytes("decrypted", data);

	}

	public static Schedule genSchedule(byte key[])
	{
		Schedule s = new Schedule();
		fsetkey(key, s);
		return s;
	}

	public static void encrypt(Schedule s, byte b[])
	{
		fencrypt(b, false, s);
	}

	public static void decrypt(Schedule s, byte b[])
	{
		fencrypt(b, true, s);
	}

	// private fields and methods

	private static int built;

	private static class Stage
	{
		int h, l;
	}
	 
	public static class Schedule
	{
		Stage KS[];
		Schedule()
		{
			KS = new Stage[16];
			for (int i=0; i<KS.length; i++)
			{
				KS[i] = new Stage();
			}
		}
	}
	 
	private final static byte bK_C[] = {
			57, 49, 41, 33, 25, 17,  9,
			 1, 58, 50, 42, 34, 26, 18,
			10,  2, 59, 51, 43, 35, 27,
			19, 11,  3, 60, 52, 44, 36,
	};
	private final static byte bK_D[] = {
			63, 55, 47, 39, 31, 23, 15,
			 7, 62, 54, 46, 38, 30, 22,
			14,  6, 61, 53, 45, 37, 29,
			21, 13,  5, 28, 20, 12, 4,
	};
	 
	private static int wC_K4[][] = new int[8][16];
	private static int wC_K3[][] = new int[8][8];
	private static int wD_K4[][] = new int[8][16];
	private static int wD_K3[][] = new int[8][8];
	 
	private static byte preshift[] = {
			1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1,
	};
	 
	private static byte bCD_KS[] = {
			14, 17, 11, 24,  1,  5,
			3,  28, 15,  6, 21, 10,
			23, 19, 12,  4, 26,  8,
			16,  7, 27, 20, 13,  2,
			41, 52, 31, 37, 47, 55,
			30, 40, 51, 45, 33, 48,
			44, 49, 39, 56, 34, 53,
			46, 42, 50, 36, 29, 32,
	};
	 
	private static int hKS_C4[][] = new int[7][16];
	private static int lKS_D4[][] = new int[7][16];
	private static int wL_I8[]    = new int[0x55 + 1];
	private static int wO_L4[]    = new int[16];
	private static int wPS[][]    = new int[8][64];
	 
	private static byte P[] = {
			16,  7, 20, 21,
			29, 12, 28, 17,
			 1, 15, 23, 26,
			 5, 18, 31, 10,
			 2,  8, 24, 14,
			32, 27,  3,  9,
			19, 13, 30,  6,
			22, 11,  4, 25,
	};
	 
	private static byte S[][] = {
			{
			14, 4,13, 1, 2,15,11, 8, 3,10, 6,12, 5, 9, 0, 7,
			 0,15, 7, 4,14, 2,13, 1,10, 6,12,11, 9, 5, 3, 8,
			 4, 1,14, 8,13, 6, 2,11,15,12, 9, 7, 3,10, 5, 0,
			15,12, 8, 2, 4, 9, 1, 7, 5,11, 3,14,10, 0, 6,13,
			},
	 
			{
			15, 1, 8,14, 6,11, 3, 4, 9, 7, 2,13,12, 0, 5,10,
			 3,13, 4, 7,15, 2, 8,14,12, 0, 1,10, 6, 9,11, 5,
			 0,14, 7,11,10, 4,13, 1, 5, 8,12, 6, 9, 3, 2,15,
			13, 8,10, 1, 3,15, 4, 2,11, 6, 7,12, 0, 5,14, 9,
			},
	 
			{
			10, 0, 9,14, 6, 3,15, 5, 1,13,12, 7,11, 4, 2, 8,
			13, 7, 0, 9, 3, 4, 6,10, 2, 8, 5,14,12,11,15, 1,
			13, 6, 4, 9, 8,15, 3, 0,11, 1, 2,12, 5,10,14, 7,
			 1,10,13, 0, 6, 9, 8, 7, 4,15,14, 3,11, 5, 2,12,
			},
	 
			{
			 7,13,14, 3, 0, 6, 9,10, 1, 2, 8, 5,11,12, 4,15,
			13, 8,11, 5, 6,15, 0, 3, 4, 7, 2,12, 1,10,14, 9,
			10, 6, 9, 0,12,11, 7,13,15, 1, 3,14, 5, 2, 8, 4,
			 3,15, 0, 6,10, 1,13, 8, 9, 4, 5,11,12, 7, 2,14,
			},
	 
			{
			 2,12, 4, 1, 7,10,11, 6, 8, 5, 3,15,13, 0,14, 9,
			14,11, 2,12, 4, 7,13, 1, 5, 0,15,10, 3, 9, 8, 6,
			 4, 2, 1,11,10,13, 7, 8,15, 9,12, 5, 6, 3, 0,14,
			11, 8,12, 7, 1,14, 2,13, 6,15, 0, 9,10, 4, 5, 3,
			},
	 
			{
			12, 1,10,15, 9, 2, 6, 8, 0,13, 3, 4,14, 7, 5,11,
			10,15, 4, 2, 7,12, 9, 5, 6, 1,13,14, 0,11, 3, 8,
			 9,14,15, 5, 2, 8,12, 3, 7, 0, 4,10, 1,13,11, 6,
			 4, 3, 2,12, 9, 5,15,10,11,14, 1, 7, 6, 0, 8,13,
			},
	 
			{
			 4,11, 2,14,15, 0, 8,13, 3,12, 9, 7, 5,10, 6, 1,
			13, 0,11, 7, 4, 9, 1,10,14, 3, 5,12, 2,15, 8, 6,
			 1, 4,11,13,12, 3, 7,14,10,15, 6, 8, 0, 5, 9, 2,
			 6,11,13, 8, 1, 4,10, 7, 9, 5, 0,15,14, 2, 3,12,
			},
	 
			{
			13, 2, 8, 4, 6,15,11, 1,10, 9, 3,14, 5, 0,12, 7,
			 1,15,13, 8,10, 3, 7, 4,12, 5, 6,11, 0,14, 9, 2,
			 7,11, 4, 1, 9,12,14, 2, 0, 6,10,13,15, 3, 5, 8,
			 2, 1,14, 7, 4,10, 8,13,15,12, 9, 0, 3, 5, 6,11,
			},
	};

	private static void buildtables()
	{
			int i, j, v;
			int wC_K[] = new int[64];
			int wD_K[] = new int[64];
			int hKS_C[] = new int[28];
			int lKS_D[] = new int[28];
			int Smap[] = new int[64];
			int wP[] = new int[32];
	 
			/* Invert permuted-choice-1 (key => C,D) */

			v = 1;
			for (j = 28; --j >= 0; )
			{
					wC_K[ bK_C[j] - 1 ] = wD_K[ bK_D[j] - 1 ] = v;
					v += v;         /* (i.e. v <<= 1) */
			}
	 
			for (i = 0; i < 64; i++)
			{
				int t = 8 >>> (i & 3);
				for (j = 0; j < 16; j++)
				{
					if ((j & t) != 0)
					{
						wC_K4[i >>> 3][j] |= wC_K[i];
						wD_K4[i >>> 3][j] |= wD_K[i];
						if (j < 8)
						{
							wC_K3[i >>> 3][j] |= wC_K[i + 3];
							wD_K3[i >>> 3][j] |= wD_K[i + 3];
						}
					}
				}
				/* Generate the sequence 0,1,2,3, 8,9,10,11, ..., 56,57,58,59. */
				if (t == 1)
				{
					i += 4;
				}
			}
	 
			/* Invert permuted-choice-2 */
	 
			v = 1;
			for (i = 24; (i -= 6) >= 0; )
			{
				j = i+5;
				do
				{
					hKS_C[ bCD_KS[j] - 1 ] = lKS_D[ bCD_KS[j+24] - 28 - 1 ] = v;
					v += v;         /* Like v <<= 1 but may be faster */
				}
				while(--j >= i);
				v <<= 2;            /* Keep byte aligned */
			}
	 
			for (i = 0; i < 28; i++)
			{
				v = 8 >>> (i & 3);
				for (j = 0; j < 16; j++)
				{
					if ((j & v) != 0)
					{
						hKS_C4[i >>> 2][j] |= hKS_C[i];
						lKS_D4[i >>> 2][j] |= lKS_D[i];
					}
				}
			}
	 
			/* Initial permutation */
	 
			for (i = 0; i <= 0x55; i++)
			{
				v = 0;
				if ((i & 64) != 0) { v =  1 << 24; }
				if ((i & 16) != 0) { v |= 1 << 16; }
				if ((i &  4) != 0) { v |= 1 <<  8; }
				if ((i &  1) != 0) { v |= 1; }
				wL_I8[i] = v;
			}
	 
			/* Final permutation */
	 
			for (i = 0; i < 16; i++)
			{
				v = 0;
				if ((i & 1) != 0) { v  = 1 << 24; }
				if ((i & 2) != 0) { v |= 1 << 16; }
				if ((i & 4) != 0) { v |= 1 <<  8; }
				if ((i & 8) != 0) { v |= 1; }
				wO_L4[i] = v;
			}
	 
			/* Funny bit rearrangement on second index into S tables */
	 
			for (i = 0; i < 64; i++)
			{
					Smap[i] = (i & 0x20) | (i & 1) << 4 | (i & 0x1e) >>> 1;
			}
	 
			/* Invert permutation P into mask indexed by R bit number */
	 
			v = 1;
			for (i = 32; --i >= 0; )
			{
					wP[ P[i] - 1 ] = v;
					v += v;
			}
	 
			/* Build bit-mask versions of S tables, indexed in natural bit order */
	 
			for (i = 0; i < 8; i++)
			{
				for (j = 0; j < 64; j++)
				{
					int k, t;
	 
					t = S[i][ Smap[j] ];
					for (k = 0; k < 4; k++)
					{
						if ((t & 8) != 0)
						{
							wPS[i][j] |= wP[4*i + k];
						}
						t += t;
					}
				}
			}
	}

	private static byte[] longToBytes(long l)
	{
		return new byte[] {
			(byte)((l >>> 56) & 0xff),
			(byte)((l >>> 48) & 0xff),
			(byte)((l >>> 40) & 0xff),
			(byte)((l >>> 32) & 0xff),
			(byte)((l >>> 24) & 0xff),
			(byte)((l >>> 16) & 0xff),
			(byte)((l >>>  8) & 0xff),
			(byte)((l       ) & 0xff),
		};
	}

	private static void printBytes(String hdr, byte data[])
	{
		System.out.print(hdr+" = ");
		for (int i=0; i<data.length; i++)
		{
			System.out.print(Integer.toHexString(data[i]&0xff)+",");
		}
		System.out.println();
	}

	private static void fsetkey(byte key[], Schedule ks)
	{
			int i;
			int C, D;
	 		
			if (built != 1)
			{
				buildtables();
				built = 1;
			}
	 
			C = D = 0;
			for (i = 0; i < 8; i++)
			{
					int v;
	 
					v = key[i] >>> 1;        /* Discard "parity" bit */
					C |= wC_K4[i][(v>>>3) & 15] | wC_K3[i][v & 7];
					D |= wD_K4[i][(v>>>3) & 15] | wD_K3[i][v & 7];
			}
	 
			/*
			 * C and D now hold the suitably right-justified
			 * 28 permuted key bits each.
			 */
			for (i = 0; i < 16; i++)
			{
					/* 28-bit left circular shift */
					C <<= preshift[i];
					C = ((C >>> 28) & 3) | (C & ((1<<28) - 1));
					ks.KS[i].h = choice2(hKS_C4, C);
	 
					D <<= preshift[i];
					D = ((D >>> 28) & 3) | (D & ((1<<28) - 1));
					ks.KS[i].l = choice2(lKS_D4, D);
			}
	}
	 
	private static void fencrypt(byte block[], boolean decrypt, Schedule ks)
	{
			int i;
			int L, R;
			int kspp;
			Stage ksp;
	 
			/* Initial permutation */
	 
			L = R = 0;
			i = 7;
			do
			{
					int v;
					v = block[i];   /* Could optimize according to ENDIAN */
					L = wL_I8[(v     ) & 0x55] | (L << 1);
					R = wL_I8[(v >>> 1) & 0x55] | (R << 1);
			}
			while(--i >= 0);
	 
			if (decrypt) {
					kspp = 15;
			} else {
					kspp = 0;
			}

			i = 16;
			do {
					int k, tR;
					ksp = ks.KS[kspp];
	 
					tR = (R >>> 15) | (R << 17);
	 
					k = ksp.h;
					L ^= PS(0, ((tR >>> 12) ^ (k >>> 24)) & 63)
					   | PS(1, ((tR >>>  8) ^ (k >>> 16)) & 63)
					   | PS(2, ((tR >>>  4) ^ (k >>>  8)) & 63)
					   | PS(3, ((tR       ) ^ (k      )) & 63);
	 
					k = ksp.l;
					L ^= PS(4, ((R  >>> 11) ^ (k >>> 24)) & 63)
					   | PS(5, ((R  >>>  7) ^ (k >>> 16)) & 63)
					   | PS(6, ((R  >>>  3) ^ (k >>>  8)) & 63)
					   | PS(7, ((tR >>> 16) ^ (k       )) & 63);
	 
					tR = L;
					L = R;
					R = tR;
	 
	 
					if (decrypt) {
							kspp--;
					} else {
							kspp++;
					}
			}
			while (--i > 0);
			{
					int t;

					t = FP(L,R,0) | (FP(L,R,8)  | (FP(L,R,16) | (FP(L,R,24) << 2)) << 2) << 2;
					R = FP(L,R,4) | (FP(L,R,12) | (FP(L,R,20) | (FP(L,R,28) << 2)) << 2) << 2;
					L = t;
			}
			{
					int t;
	 
					t = R;
					block[7] = (byte)((t       ) & 0xff);
					block[6] = (byte)((t >>>= 8) & 0xff);
					block[5] = (byte)((t >>>= 8) & 0xff);
					block[4] = (byte)((t >>>  8) & 0xff);
					t = L;
					block[3] = (byte)((t       ) & 0xff);
					block[2] = (byte)((t >>>= 8) & 0xff);
					block[1] = (byte)((t >>>= 8) & 0xff);
					block[0] = (byte)((t >>>  8) & 0xff);
			}
	}

	private static int choice2(int b[][], int v)
	{
		return
			b[6][(v>>> 0)&15] | 
			b[5][(v>>> 4)&15] |
			b[4][(v>>> 8)&15] | 
			b[3][(v>>>12)&15] |
			b[2][(v>>>16)&15] | 
			b[1][(v>>>20)&15] |
			b[0][(v>>>24)&15] ;
	}

	private static int PS(int i, int j)
	{
		return wPS[i][j];
	}

	private static int FP(int L, int R, int k)
	{
		return ((wO_L4[ (L >>> (k)) & 15 ] << 1) | wO_L4[ (R >>> (k)) & 15 ]);
	}
}

