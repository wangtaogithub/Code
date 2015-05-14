/*
 * Copyright (C) 2013 Shenzhen Huiding Technology Co.,Ltd
 *
 */
 
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <dirent.h>
#include <time.h>

#include "tool.h"
#define RESET_EXIT_XFE_MODE

#define REG_2ND_CMD         0x8040
#define REG_1ST_CMD         0x8046
#define REG_LINK_MODE       0x81B0
#define REG_PAIR_FLAG       0x81B2
#define REG_XFE_STATE       0xAB10
#define REG_NOTIFY_STATE    0xAB1F
#define REG_SNDBUF_START    0xAC90
#define REG_DATA_FRESH      0xAD91
#define REG_RCVBUF_STATE    0xAE10
#define REG_RCVBUF_START    0xAE12

#define CMD_ENTER_SLAVE     0x20
#define CMD_ENTER_MASTER    0x21
#define CMD_ENTER_TRANSFER  0x22
#define CMD_EXIT_SLAVE      0x28
#define CMD_EXIT_MASTER     0x29
#define CMD_EXIT_TRANSFER   0x2A

#define MODE_TOUCH_ONLY     0x00
#define MODE_PAIRED         0X55
#define MODE_SLAVE          0x88
#define MODE_MASTER         0x99
#define MODE_SEND           0xAA
#define MODE_RECEIVE        0xBB

bool read_pair_state()
{
    s32 ret = -1;
    u8  buf[2] = {0,0};
    
    ret = read_register(buf, 2, REG_PAIR_FLAG);
    if(ret < 0)
    {
        DLINK("Fail: read_pair_state# IIC read from 0x81B2 error.");
        return false;
    }

    if(buf[0]==buf[1] && buf[0]==MODE_PAIRED)
    {
        return true;
    }
    
    return false;
}
    
bool clear_pair_buf()
{
    u8 buf[2] = {0,0};
    u8 retry = 0;
    
    while(retry++ < 20)
    {
        buf[0]=0;
        buf[1]=0;
        write_register(buf, 2, REG_PAIR_FLAG); 
        buf[0]=0x00;
        buf[1]=0x55;
        read_register(buf, 2, REG_PAIR_FLAG);
        if(buf[0]==buf[1] && buf[0]==MODE_TOUCH_ONLY)
        {
            return true;
        }
    };
    
    DLINK("Fail: clear_pair_buf[0x81B2]# Time out error.");
    return false;
    
}

bool snd_cfm_cmd(u8 cmd)
{
    s32 ret = -1;
    u8  buf = cmd;
    u8  retry = 0;
    
    while(retry++ < 5)
    {
        ret = write_register(&buf, 1, REG_1ST_CMD);
        if (ret < 0)
        {
            continue;
        }
        ret = write_register(&buf, 1, REG_2ND_CMD);
        if (ret < 0)
        {
            continue;
        }
        return true;
    }
    
    DLINK("Fail: snd_cfm_cmd# IIC write to 0x8046 & 0x8040 error.");
    return false;
}

//LinkÃüÁî²Ù×÷
bool enter_slave_mode()
{
    s32 ret = -1;
    u8  buf[2] = {0,0};
    
    if (!snd_cfm_cmd(CMD_ENTER_SLAVE))
    {
        DLINK("Fail: enter_slave_mode# Send cmd error");
        return false;
    }
    usleep(40*1000);
    ret = read_register(buf, 2, REG_LINK_MODE);
    if (ret < 0)
    {
        DLINK("Fail: enter_slave_mode# IIC read from 0x81B0 error.");
        return false;
    }
    if (buf[0]==MODE_SLAVE && buf[0]==buf[1])
    {
        return true;
    }
    return false;
}

bool enter_master_mode()
{
    s32 ret = -1;
    u8  buf[2] = {0,0};
    u8  retry = 0;
    
    while(retry++ < 10)
    {
        if (!snd_cfm_cmd(CMD_ENTER_MASTER))
        {
            DLINK("Fail: enter_master_mode# Send cmd error");
            continue;
        }
        usleep(40*1000);
        ret = read_register(buf, 2, REG_LINK_MODE);
        if (ret<0)
        {
            DLINK("Fail: enter_master_mode# IIC read error.");
            continue;
        }
        if (buf[0]==MODE_MASTER && buf[0]==buf[1])
        {
            return true;
        }
        DLINK("Fail: enter_master_mode# [0x81B0]=0x%02x,[0x81B1]=0x%02x", buf[0], buf[1]);
    }
    DLINK("Fail: enter_master_mode# Time out error");
    return false;
}

bool enter_transfer_mode()
{
    s32 ret = -1;
    u8  buf[2] = {0,0};
    s32 retry = 0;
    
    while(retry++ < 10)
    {
        if (!snd_cfm_cmd(CMD_ENTER_TRANSFER))
        {
            DLINK("Fail: enter_transfer_mode# Send cmd error");
            continue;
        }
        usleep(100*1000);                               //compatible to multi-system chip
        ret = read_register(buf, 2, REG_LINK_MODE);
        if (ret<0)
        {
            DLINK("Fail: enter_transfer_mode# IIC read error.");
            continue;
        }
        if(buf[0]==MODE_SEND && buf[0]==buf[1]) 
        {
            return true;
        }
        DLINK("Fail: enter_transfer_mode# [0x81B0]=0x%02x,[0x81B1]=0x%02x", buf[0], buf[1]);
    }
    DLINK("Fail: enter_transfer_mode# Time out error");
    return false;
}

bool exit_slave_mode()
{
    s32 ret = -1;
    u8  buf[2] = {0,0};
    u8  retry = 0;
    
    while(retry++ < 10)
    {
        if (!snd_cfm_cmd(CMD_EXIT_SLAVE))
        {
            DLINK("Fail: exit_slave_mode# Send cmd error");
            continue;
        }
        usleep(40*1000);
        ret = read_register(buf, 2, REG_LINK_MODE);
        if (ret<0)
        {
            DLINK("Fail: exit_slave_mode# IIC read error.");
            continue;
        }
        
        if (buf[0]!=MODE_SLAVE && buf[0]==buf[1])
        {
            return true;
        }
        DLINK("Fail: exit_slave_mode# [0x81B0]=0x%02x,[0x81B1]=0x%02x", buf[0], buf[1]);
    }
    
    DLINK("Fail: exit_slave_mode# Time out error");
    return false;
}
   
bool exit_master_mode()
{
    s32 ret = -1;
    u8  buf[2] = {0,0};
    u8  retry = 0;
    
    return true;		//for flashless
    while(retry++ < 10)
    {
        if (!snd_cfm_cmd(CMD_EXIT_MASTER))
        {
            DLINK("Fail: exit_master_mode# Send cmd error");
            continue;
        }
        usleep(40*1000);
        ret = read_register(buf, 2, REG_LINK_MODE);
        if (ret < 0)
        {
            DLINK("Fail: exit_master_mode# IIC read error.");
            continue;
        }
        if (buf[0]!=MODE_MASTER && buf[0]==buf[1])        
        {
            return true;
        }
        DLINK("Fail: exit_master_mode# [0x81B0]=0x%02x,[0x81B1]=0x%02x", buf[0], buf[1]);
    }
    DLINK("Fail: exit_master_mode# Time out error");
    return false;
}

bool exit_transfer_mode()
{
    s32 ret = -1;
    u8  buf[2] = {0,0};
    u8  retry = 0;
    
    while(retry++ < 20)
    {
    #ifdef RESET_EXIT_XFE_MODE
        DLINK("exit_transfer_mode# reset guitar.");
        ret = guitar_reset();      
        if(ret < 0)
        {
            DLINK("Fail: exit_transfer_mode# Reset guitar error.");
            continue;
        }
        return true;
    #else
        if (!snd_cfm_cmd(CMD_EXIT_TRANSFER))
        {
            DLINK("Fail: exit_transfer_mode# Send cmd error");
            continue;
        }
        usleep(100*1000);
        ret = read_register(buf, 2, REG_LINK_MODE);
        if (ret < 0)
        {
            DLINK("Fail: exit_transfer_mode# IIC read error.");
            continue;
        }
        if(buf[0]!=MODE_SEND && buf[0] !=MODE_RECEIVE && buf[0]==buf[1])        
        {
            return true;
        }
        DLINK("Fail: exit_transfer_mode# [0x81B0]=0x%02x,[0x81B1]=0x%02x", buf[0], buf[1]);
    #endif
    }
    DLINK("Fail: exit_transfer_mode# Time out error");
    return false;
}

/*------------------------------Send Buffer-------------------------------
 *    |  Addr  |  Bit7  | Bit6 | Bit5 | Bit4 | Bit3 | Bit2 | Bit1 | Bit0 |
 *    | 0xAC90 |                        DataLength                       |
 *    | 0xAC91 |                          Data0                          |
 *    | 0xAC92 |                          Data1                          |
 *    | ...... |                          .....                          |
 *    | 0xAD8D |                         Data251                         |
 *    | 0xAD8E |                      CRC High Byte(Floating Addr)       |
 *    | 0xAD8F |                      CRC Low  Byte(Floating Addr)       |
 *    | 0xAD91 |                         DataFresh                       |
 *------------------------------------------------------------------------
 *    | 0xAB10 |                        SendStatus                       |
 *    | 0xAB11 |                       ReceiveStatus                     |
 *    | 0xAB12 |                       SendStatusBak                     |
 *    | 0xAB13 |                      ReceiveStatusBak                   |
 *    | ...... |                          Reserved                       |
 *    | 0xAB1F |                        NotifyStatus                     |
 *-----------------------------------------------------------------------*/
bool send_data(u8* buf, s32 length)
{
    bool running = true;
    s32 ret = -1;
    u8  sndFlag[10];
    u8  *sndBuf = NULL;
    s32 idleTimes = 0;
    s32 retry = 0;
    
    sndBuf = (u8*)malloc(length + 2);
    if(sndBuf == NULL)
    {
        DLINK("Fail: send_data# Alloc memory error.");
        return false;
    }
    //query 0xAB10 send state is not sending 0x02
    while(retry++ < 5) 
    {
        ret = read_register(sndFlag, 4, REG_XFE_STATE);
        if(ret < 0)
        {
            DLINK("Fail: send_data# IIC read from 0xAB10 error.");
            continue;
        }
        if(sndFlag[0]!=sndFlag[2] || sndFlag[1]!=sndFlag[3])
        {
            DLINK("Fail: send_data# [0xAB10~0xAB13]:0x%02x,0x%02x,0x%02x,0x%02x"
                 ,sndFlag[0],sndFlag[1],sndFlag[2],sndFlag[3]);
            continue;
        }       
        if(sndFlag[0] == 0x02)
        {
            DLINK("Pending: send_data# Wait for sending completed.");
            sndBuf[0] = 0;
            write_register(sndBuf, 1, REG_NOTIFY_STATE);
            continue;
        }
        //Calculate the checksum
        sndBuf[0] = length;
        sndBuf[length+1] = calculate_check_sum(buf,length);
        //System.arraycopy(buf, 0, sndBuf, 1, length);
        memcpy(&sndBuf[1], buf, length);
        //Writes data to the 0xAC90
        ret = write_register(sndBuf, length+2, REG_SNDBUF_START);
        if(ret < 0)
        {
            DLINK("Fail: send_data# IIC write to 0xAC90 error.");
            continue;
        }
        sndFlag[0] = 0xAA;
        ret = write_register(sndFlag, 1, REG_DATA_FRESH);
        if(ret < 0)
        {
            DLINK("Fail: send_data# IIC write to 0xAD91 error.");
            continue;
        }
        break;
    }
    
    if(retry >= 5)
    {
        DLINK("Fail: send_data# Time out error");
        free(sndBuf);
        return false;
    }
    
    //polling 0xAB10 send state is 0x03,send success
    while(running)
    {
        if(idleTimes>300)
        {
            DLINK("Fail: send_data# Wait send flag[0xAB10] timeout error.");
            free(sndBuf);
            return false;
        }
        ret = read_register(sndFlag, 4, REG_XFE_STATE);
        if(ret < 0)
        {
            DLINK("Fail: send_data# IIC read from 0xAB10 error.");
            idleTimes ++;
            usleep(40*1000);
            continue;
        }
        if(sndFlag[1]!=sndFlag[3] || sndFlag[0]!=sndFlag[2])
        {
            DLINK("Fail: send_data# Flag not match[0xAB10~0xAB13]:0x%02x,0x%02x,0x%02x,0x%02x"
                 ,sndFlag[0],sndFlag[1],sndFlag[2],sndFlag[3]);
            read_register(sndFlag, 2, 0x81B0);
            DLINK("Fail: send_data# [0x81B0]=0x%02x,[0x81B1]=0x%02x", sndFlag[0], sndFlag[1]);
            idleTimes ++;
            usleep(40*1000);
            continue;
        } 
        DLINK("Pending: send_data# Snd state[0xAB10]=0x%02x,Retries=%d", sndFlag[0], idleTimes);
        switch(sndFlag[0])
        { 
        case 0x01:
        case 0x02:
        case 0x05:
            idleTimes++;            
            break;
        case 0x03:
            sndBuf[0] = 0;
            write_register(sndBuf, 1, REG_NOTIFY_STATE);
            free(sndBuf);
            return true;
        case 0x04:
            DLINK("Pending: Chip request re_send_data.");
            ret = write_register(sndBuf, length+2, REG_SNDBUF_START);
            if(ret < 0)
            {
                DLINK("Fail: re_send_data# IIC write to 0xAC90 error.");
                continue;
            }
            sndFlag[0] = 0xAA;
            ret = write_register(sndFlag, 1, REG_DATA_FRESH);
            if(ret < 0)
            {
                DLINK("Fail: re_send_data# IIC write to 0xAC90 error.");
                running = false;;
            }
            continue;
            break;
        default:
            DLINK("Fail: send_data# Unknown state read from 0xAB10");
            idleTimes++;
            break;
        } 
        usleep(40*1000);
    }
    DLINK("Fail: send_data# Polling 0xAB10 timeout,Retries=%d.", idleTimes);
    free(sndBuf);
    return false;
}

/*-----------------------------Receive Buffer-----------------------------
 *    |  Addr  |  Bit7  | Bit6 | Bit5 | Bit4 | Bit3 | Bit2 | Bit1 | Bit0 |
 *    | 0xAE10 |BufState|                  NC                            |
 *    | 0xAE11 |                        DataLength                       |
 *    | 0xAE12 |                          Data0                          |
 *    | 0xAE13 |                          Data1                          |
 *    | ...... |                          .....                          |
 *    | 0xAF0D |                         Data251                         |
 *    | 0xAF0E |                      CRC High Byte(Floating Addr)       |
 *    | 0xAF0F |                      CRC Low  Byte(Floating Addr)       |
 *------------------------------------------------------------------------
 *    | 0xAB10 |                        SendStatus                       |
 *    | 0xAB11 |                       ReceiveStatus                     |
 *    | 0xAB12 |                       SendStatusBak                     |
 *    | 0xAB13 |                      ReceiveStatusBak                   |
 *    | ...... |                          Reserved                       |
 *    | 0xAB1F |                        NotifyStatus                     |
 *-----------------------------------------------------------------------*/
s32 receive_data(u8* buf)
{
    bool running = true;
    s32 ret = -1;
    s32 rcvLength = 0;
    s32 crcLength = 0;
    s32 idleTimes = 0;
    u8  rcvFlag[4];
    u8  rcvBuf[260];
    
    //query 0xAB10 receive state is 0x03,receive success
    while(running)
    {
        if(idleTimes > 300)
        {
            DLINK("Fail: receive_data# Wait receive flag[0xAB10] timeout error.");
            return -2;
        }
        ret = read_register(rcvFlag, 4, REG_XFE_STATE);
        if(ret < 0)
        {
            DLINK("Fail: receive_data# IIC read 0xAB10 error.");
            idleTimes ++;
            usleep(40*1000);
            continue;
        }       
        if(rcvFlag[0]!=rcvFlag[2] || rcvFlag[1]!=rcvFlag[3])
        {
            DLINK("Fail: receive_data# Flag not match[0xAB10~0xAB13]:0x%02x,0x%02x,0x%02x,0x%02x"
                 ,rcvFlag[0],rcvFlag[1],rcvFlag[2],rcvFlag[3]);
            idleTimes ++;
            usleep(40*1000);
            continue;
        }
        switch(rcvFlag[1])
        {
        case 0x01:
        case 0x02:
            idleTimes++;   
            break;
        case 0x03:
            running = false;
            break;
        case 0x04:
            idleTimes=0;
            DLINK("Pending: Chip request re_receive_data");
            break;
        default:
            idleTimes++;    
            break;
        }
        usleep(40*1000);
        DLINK("Pending: receive_data# Rcv state[0xAB11]=0x%02x,Retries=%d", rcvFlag[1], idleTimes);
    }
    
    //read data buffer
    s32 retry = 0;
    while(retry++ < 5) 
    {
        ret = read_register(rcvBuf, 2, REG_RCVBUF_STATE);
        if(ret < 0)
        {
            DLINK("Fail: receive_data# IIC read from 0xAE10 error.");
            continue;
        }       
        if((rcvBuf[0]&0x80)==0)
        {
            DLINK("Fail: receive_data# Data not ready:0x%02X.");
            rcvFlag[0] = 0;
            write_register(rcvFlag, 1, REG_NOTIFY_STATE);
            continue;
        }
        
        //rcvLength = rcvBuf[1]>0 ? rcvBuf[1] : rcvBuf[1]+256 ;
        rcvLength = rcvBuf[1];
        if(rcvLength > 252)
        {
            DLINK("Fail: receive_data# Data length out of range,Length:", rcvLength);
            rcvFlag[0] = 0;
            write_register(rcvFlag, 1, REG_NOTIFY_STATE);
            continue;
        }
        crcLength = rcvLength;
        if(rcvLength%2==1)
        {
            crcLength = rcvLength+1;
        }
        //read the remain data
        ret = read_register(rcvBuf, crcLength+2, REG_RCVBUF_START);
        if(ret < 0)
        {
            DLINK("Fail: receive_data# IIC read from 0xAE12 error.");
            continue;
        } 

        if(!check_crc(rcvBuf, crcLength+2, rcvLength))
        {
            DLINK("Fail: receive_data# CRC check error");
            rcvFlag[0] = 0;
            write_register(rcvFlag, 1, REG_NOTIFY_STATE);
            continue;
        }
        break;
    }
    
    if(retry >= 5 || rcvLength == 0)
    {
        DLINK("Fail: receive_data# Time out error.");
        return -2;
    }
    
    //System.arraycopy(rcvBuf, 0, buf, 0, rcvLength);
    memcpy(buf, rcvBuf, rcvLength);
    //clear 0xAE10,0xAB1F
    rcvFlag[0] = 0;
    rcvFlag[1] = 0;
    write_register(rcvFlag, 2, REG_RCVBUF_STATE);
    write_register(rcvFlag, 1, REG_NOTIFY_STATE);
    
    return rcvLength;
}


#define BYTE u8

void printBytes(BYTE b[], int len) {
	DEBUG_ARRAY(b,len);
}

/******************************************************************************/

// The following lookup tables and functions are for internal use only!
BYTE AES_Sbox[] = {99,124,119,123,242,107,111,197,48,1,103,43,254,215,171,
  118,202,130,201,125,250,89,71,240,173,212,162,175,156,164,114,192,183,253,
  147,38,54,63,247,204,52,165,229,241,113,216,49,21,4,199,35,195,24,150,5,154,
  7,18,128,226,235,39,178,117,9,131,44,26,27,110,90,160,82,59,214,179,41,227,
  47,132,83,209,0,237,32,252,177,91,106,203,190,57,74,76,88,207,208,239,170,
  251,67,77,51,133,69,249,2,127,80,60,159,168,81,163,64,143,146,157,56,245,
  188,182,218,33,16,255,243,210,205,12,19,236,95,151,68,23,196,167,126,61,
  100,93,25,115,96,129,79,220,34,42,144,136,70,238,184,20,222,94,11,219,224,
  50,58,10,73,6,36,92,194,211,172,98,145,149,228,121,231,200,55,109,141,213,
  78,169,108,86,244,234,101,122,174,8,186,120,37,46,28,166,180,198,232,221,
  116,31,75,189,139,138,112,62,181,102,72,3,246,14,97,53,87,185,134,193,29,
  158,225,248,152,17,105,217,142,148,155,30,135,233,206,85,40,223,140,161,
  137,13,191,230,66,104,65,153,45,15,176,84,187,22};

BYTE AES_ShiftRowTab[] = {0,5,10,15,4,9,14,3,8,13,2,7,12,1,6,11};

BYTE AES_Sbox_Inv[256];
BYTE AES_ShiftRowTab_Inv[16];
BYTE AES_xtime[256];

void AES_SubBytes(BYTE state[], BYTE sbox[]) {
  int i;
  for(i = 0; i < 16; i++)
    state[i] = sbox[state[i]];
}

void AES_AddRoundKey(BYTE state[], BYTE rkey[]) {
  int i;
  for(i = 0; i < 16; i++)
    state[i] ^= rkey[i];
}

void AES_ShiftRows(BYTE state[], BYTE shifttab[]) {
  BYTE h[16];
  memcpy(h, state, 16);
  int i;
  for(i = 0; i < 16; i++)
    state[i] = h[shifttab[i]];
}

void AES_MixColumns(BYTE state[]) {
  int i;
  for(i = 0; i < 16; i += 4) {
    BYTE s0 = state[i + 0], s1 = state[i + 1];
    BYTE s2 = state[i + 2], s3 = state[i + 3];
    BYTE h = s0 ^ s1 ^ s2 ^ s3;
    state[i + 0] ^= h ^ AES_xtime[s0 ^ s1];
    state[i + 1] ^= h ^ AES_xtime[s1 ^ s2];
    state[i + 2] ^= h ^ AES_xtime[s2 ^ s3];
    state[i + 3] ^= h ^ AES_xtime[s3 ^ s0];
  }
}

void AES_MixColumns_Inv(BYTE state[]) {
  int i;
  for(i = 0; i < 16; i += 4) {
    BYTE s0 = state[i + 0], s1 = state[i + 1];
    BYTE s2 = state[i + 2], s3 = state[i + 3];
    BYTE h = s0 ^ s1 ^ s2 ^ s3;
    BYTE xh = AES_xtime[h];
    BYTE h1 = AES_xtime[AES_xtime[xh ^ s0 ^ s2]] ^ h;
    BYTE h2 = AES_xtime[AES_xtime[xh ^ s1 ^ s3]] ^ h;
    state[i + 0] ^= h1 ^ AES_xtime[s0 ^ s1];
    state[i + 1] ^= h2 ^ AES_xtime[s1 ^ s2];
    state[i + 2] ^= h1 ^ AES_xtime[s2 ^ s3];
    state[i + 3] ^= h2 ^ AES_xtime[s3 ^ s0];
  }
}

// AES_Init: initialize the tables needed at runtime.
// Call this function before the (first) key expansion.
void AES_Init() {
  int i;
  for(i = 0; i < 256; i++)
    AES_Sbox_Inv[AES_Sbox[i]] = i;

  for(i = 0; i < 16; i++)
    AES_ShiftRowTab_Inv[AES_ShiftRowTab[i]] = i;

  for(i = 0; i < 128; i++) {
    AES_xtime[i] = i << 1;
    AES_xtime[128 + i] = (i << 1) ^ 0x1b;
  }
}

// AES_Done: release memory reserved by AES_Init.
// Call this function after the last encryption/decryption operation.
void AES_Done() {}

/* AES_ExpandKey: expand a cipher key. Depending on the desired encryption
   strength of 128, 192 or 256 bits 'key' has to be a byte array of length
   16, 24 or 32, respectively. The key expansion is done "in place", meaning
   that the array 'key' is modified.
*/
int AES_ExpandKey(BYTE key[], int keyLen) {
  int kl = keyLen, ks, Rcon = 1, i, j;
  BYTE temp[4], temp2[4];
  switch (kl) {
    case 16: ks = 16 * (10 + 1); break;
    case 24: ks = 16 * (12 + 1); break;
    case 32: ks = 16 * (14 + 1); break;
    default:
    	  DEBUG("AES_ExpandKey: Only key lengths of 16, 24 or 32 bytes allowed!");
  }
  for(i = kl; i < ks; i += 4) {
    memcpy(temp, &key[i-4], 4);
    if (i % kl == 0) {
      temp2[0] = AES_Sbox[temp[1]] ^ Rcon;
      temp2[1] = AES_Sbox[temp[2]];
      temp2[2] = AES_Sbox[temp[3]];
      temp2[3] = AES_Sbox[temp[0]];
      memcpy(temp, temp2, 4);
      if ((Rcon <<= 1) >= 256)
        Rcon ^= 0x11b;
    }
    else if ((kl > 24) && (i % kl == 16)) {
      temp2[0] = AES_Sbox[temp[0]];
      temp2[1] = AES_Sbox[temp[1]];
      temp2[2] = AES_Sbox[temp[2]];
      temp2[3] = AES_Sbox[temp[3]];
      memcpy(temp, temp2, 4);
    }
    for(j = 0; j < 4; j++)
      key[i + j] = key[i + j - kl] ^ temp[j];
  }
  return ks;
}

// AES_Encrypt: encrypt the 16 byte array 'block' with the previously expanded key 'key'.
void AES_Encrypt(BYTE block[], BYTE key[], int keyLen) {
  int l = keyLen, i;
  printBytes(block, 16);
  AES_AddRoundKey(block, &key[0]);
  for(i = 16; i < l - 16; i += 16) {
    AES_SubBytes(block, AES_Sbox);
    AES_ShiftRows(block, AES_ShiftRowTab);
    AES_MixColumns(block);
    AES_AddRoundKey(block, &key[i]);
  }
  AES_SubBytes(block, AES_Sbox);
  AES_ShiftRows(block, AES_ShiftRowTab);
  AES_AddRoundKey(block, &key[i]);
}

// AES_Decrypt: decrypt the 16 byte array 'block' with the previously expanded key 'key'.
void AES_Decrypt(BYTE block[], BYTE key[], int keyLen) {
  int l = keyLen, i;
  AES_AddRoundKey(block, &key[l - 16]);
  AES_ShiftRows(block, AES_ShiftRowTab_Inv);
  AES_SubBytes(block, AES_Sbox_Inv);
  for(i = l - 32; i >= 16; i -= 16) {
    AES_AddRoundKey(block, &key[i]);
    AES_MixColumns_Inv(block);
    AES_ShiftRows(block, AES_ShiftRowTab_Inv);
    AES_SubBytes(block, AES_Sbox_Inv);
  }
  AES_AddRoundKey(block, &key[0]);
}

// get the key
void GetKey(int index, BYTE key[],int len)
{
	int i;
	for(i=0;i<len;i++)
	{
		key[i] = i;
	}
}

bool check_authorization()
{
	BYTE writeBuffer[16];
	BYTE readBuffer[16];
	BYTE temp;
	int i;
	int retry=0;

	// initialize the text
	for(i=0;i<16;i++)
	{
		writeBuffer[i] = i;
	}

	while(retry++<5)
	{
		if(download_authorization_code()<0)
		{
			continue;
		}
		DEBUG("download authorization code success");
		temp=0x33;
		if(write_register(&temp, 1, REG_2ND_CMD)<0)
		{
			continue;
		}
		DEBUG("write 0x33 to 0x8040 success");
		usleep(10*1000);
		if(read_register(&temp, 1, REG_2ND_CMD)<0)
		{
			continue;
		}
		if(temp!=0x44)
		{
			continue;
		}
		DEBUG("read 0x44 from 0x8040 success");
		if(write_register(writeBuffer, 16, 0xAB11)<0)
		{
			continue;
		}
		DEBUG("write challenge pattern to 0xAB11 success");
		temp = 0x55;
		if(write_register(&temp, 1, REG_2ND_CMD)<0)
		{
			continue;
		}
		DEBUG("write 0x55 to 0x8040 success");
		usleep(200*1000);
		if(read_register(&temp, 1, 0xAB10)<0)
		{
			continue;
		}
		if(temp != 0xAA)
		{
			continue;
		}
		DEBUG("read 0xAA from 0x8040 success");
		if(read_register(readBuffer, 16, 0xAB11)<0)
		{
			continue;
		}
		DEBUG("read cipher from 0xAB11 success");
		break;
	}

	DLINK("exit_transfer_mode# reset guitar.");
    guitar_reset();

	if(retry >= 5)
	{
		DEBUG("check authorization time out!");
		return false;
	}



	DEBUG("Original:"); printBytes(readBuffer, 16);

	BYTE key[16 * (14 + 1)];
	int keyLen = 32, maxKeyLen=16 * (14 + 1), blockLen = 16;

	GetKey(0,key,keyLen);

	DEBUG("Key:"); printBytes(key, keyLen);

	AES_Init();

	int expandKeyLen = AES_ExpandKey(key, keyLen);

	//AES_Encrypt(block, key, expandKeyLen);

	//DEBUG("Encrypt:"); printBytes(block, blockLen);

	AES_Decrypt(readBuffer, key, expandKeyLen);

	DEBUG("Decrypt:"); printBytes(readBuffer, blockLen);

	AES_Done();

	for(i = 0; i < 16; i++)
	{
		if(readBuffer[i] != writeBuffer[i])
		{
			break;
		}
	}

	if(i == 16)
	{
		return true;
	}
	else
	{
		return false;
	}
	/*
  int i;
  AES_Init();

  BYTE block[16];
  for(i = 0; i < 16; i++)
    block[i] = 0x11 * i;

  DEBUG("Original:"); printBytes(block, 16);

  BYTE key[16 * (14 + 1)];
  int keyLen = 32, maxKeyLen=16 * (14 + 1), blockLen = 16;

  GetKey(0,key,keyLen);

  DEBUG("Key:"); printBytes(key, keyLen);

  int expandKeyLen = AES_ExpandKey(key, keyLen);

  //DEBUG("Expand Key:"); printBytes(key, expandKeyLen);

  AES_Encrypt(block, key, expandKeyLen);

  DEBUG("Encrypt:"); printBytes(block, blockLen);

  AES_Decrypt(block, key, expandKeyLen);

  DEBUG("Decrypt:"); printBytes(block, blockLen);

  AES_Done();

  	  return false;
  	  */
}
