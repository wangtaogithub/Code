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

const char* PROC_PATH = "/proc/goodix_tool";

s32 write_node(u8* data, s32 length)
{
    s32 fd;
    u32 len;

    fd = open(PROC_PATH, O_WRONLY);
    if (fd <= 0)
    {
		auto_set_node();
		fd = open(PROC_PATH, O_WRONLY);
		if(fd <= 0)
		{
			DEBUG("open %s node fail .",PROC_PATH);
			close(fd);
        	return NODE_OPEN_ERR;
		}	
    }

    len = write(fd, data, length);
    close(fd);
    if (len <= 0)
    {
		DEBUG("write node fail, return:%d",len);

		return NODE_WRITE_ERR;
    }

    return len;
}

s32 read_node(u8* data, s32 length)
{
    s32 fd;
    s32 len;

    fd = open(PROC_PATH, O_RDONLY);
    if (fd <= 0)
    {
        auto_set_node();
		fd = open(PROC_PATH, O_WRONLY);
		if(fd <= 0)
		{
			DEBUG("open %s node fail .",PROC_PATH);
			close(fd);
        	return NODE_OPEN_ERR;
		}	
    }

    len = read(fd, data, length);
    close(fd);
    if (len <= 0)
    {
        DEBUG("read node fail, return:%d",len);
		return NODE_READ_ERR;
    }

    return len;
}

s32 write_data(st_com_cmd_head* head)
{
    return write_node((u8*)head, CMD_HEAD_LENGTH + head->data_len);
}

s32 read_data(st_com_cmd_head* head, u8* buf)
{
    s32 ret;
    ret = write_node((u8*)head, CMD_HEAD_LENGTH);
    if (ret < 0)
    {
        DEBUG("write head failed");
		return ret;
    }

    return read_node(buf, head->data_len);
}

s32 read_register(u8* data,u16 len,u16 addr)
{
    s32 ret = -1,i=0;
	u8 buf[len];
	st_com_cmd_head *read_head;
	read_head = (st_com_cmd_head*)malloc(CMD_HEAD_LENGTH+1);
	INIT_READ_CMD_HEAD((*read_head),2);
	read_head->wr = 0;
	read_head->addr[0] = (addr>>8);
	read_head->addr[1] = addr&0xff;
	read_head->data_len = len;

    while(i++ < 5)
    {
    	ret = read_data(read_head,buf);

		if(ret > 0)
			break;
    }
	free(read_head);

	if(ret<0)
		return ret;

	if(data != NULL)
	{
	    memcpy(data,buf,ret);
	}

	return ret;
}

s32 write_register(u8* data,u16 len,u16 addr)
{
    s32 ret = -1,i=0;
	st_com_cmd_head *write_head;
	write_head = (st_com_cmd_head*)malloc(CMD_HEAD_LENGTH+len);
	INIT_WRITE_CMD_HEAD((*write_head),2);
	write_head->wr = 1;
	write_head->addr[0] = (addr>>8);
	write_head->addr[1] = addr&0xff;
	write_head->data_len = len;

	memcpy(write_head->data,data,len);
	while(i++ <5)
	{
		ret = write_data(write_head);
		if(ret > 0 )
			break;
	}

	return ret;
}

s32 auto_set_node()
{   
	char const* findDir = "/proc/";
	DIR* dir = NULL;
	struct dirent* entry;	
	u8 temp[30]="goodix_tool";
	s32 ret = -1;
	s8* node;
	if((dir = opendir(findDir)) == NULL)
	{
		DEBUG("open dir /proc/ failed!");
		return -1;
	}
		
	while(entry = readdir(dir))
	{
        DEBUG("%s",entry->d_name);
		if(strstr(entry->d_name,(const char*)temp)||strstr(entry->d_name,"gmnode")||strstr(entry->d_name,"GMNode") )//20130918
		{
			ret = 1;
			break;
		}	
	}
	closedir(dir);
	if(ret < 0)
	{
		return -1;
	}
	memset(temp,0,sizeof(temp));
	memcpy(temp,findDir,strlen(findDir));
	memcpy(&temp[strlen(findDir)],entry->d_name, strlen((const char*)entry->d_name));

	u8 len = strlen((const char*)temp);
	node = (s8*)malloc(len + 1);	
	memcpy(node,(const char*)temp,len);
	node[len] = '\0';
		
	PROC_PATH = (const char*)node;
    
	DEBUG("find the node file is %s.",PROC_PATH);
			
	return 1;
}

s32 guitar_reset()
{
    st_com_cmd_head * resetCmd;
	s32 ret = -1;
	resetCmd = (st_com_cmd_head*)malloc(CMD_HEAD_LENGTH+1);
	INIT_WRITE_CMD_HEAD((*resetCmd),2);
	resetCmd->wr = 13;
	ret = write_data(resetCmd);
	free(resetCmd);
	if(ret < 0)
	{
		DEBUG("leave update mode fail ");
	}
	return ret;
}

s32 download_link_code()
{
	st_com_cmd_head *downLinkCmd;
	s32 ret = -1;
	DEBUG("download_link_code");
	downLinkCmd = (st_com_cmd_head*)malloc(CMD_HEAD_LENGTH+1);
	INIT_WRITE_CMD_HEAD((*downLinkCmd),2);
	downLinkCmd->wr = 19;
	ret = write_data(downLinkCmd);
	free(downLinkCmd);
	if(ret < 0)
	{
		DEBUG("download link code failed!");
	}
	return ret;
}

s32 download_authorization_code()
{
	st_com_cmd_head *downAuthorCmd;
	s32 ret = -1;
	downAuthorCmd = (st_com_cmd_head*)malloc(CMD_HEAD_LENGTH+1);
	INIT_WRITE_CMD_HEAD((*downAuthorCmd),2);
	downAuthorCmd->wr = 19;
	downAuthorCmd->flag = 1;
	ret = write_data(downAuthorCmd);
	free(downAuthorCmd);
	if(ret < 0)
	{
		DEBUG("download authorization code failed!");
	}
	return ret;
}

bool check_crc(u8* data,u16 len,u8 rcvlen)
{
    u16  i,j;
    u16 crc = 0xFFFF;
	u8 temp[len],flag,c15;
	memcpy(temp,data,len);
	for(i=0; i<len-2; i++)
	{
        //DEBUG("crc data[%d]:0x%x",i,temp[i]);
		for (j = 0; j < 8; j++) 
	    {
	        //BIT flag = (value & 0x80);
	        flag = ((temp[i] & 0x80)>>7);
	        //BIT c15 = (crc & 0x8000);
	        c15 = (u8)((crc&0x8000)>>15);
	         
	        temp[i] <<= 1;
	        crc <<= 1;
	        if (c15^flag)
	        {
	            crc ^= 0x1021;
	        }
	    }
	}
	DEBUG("crc rcvlen:0x%x",rcvlen);
	for (j = 0; j < 8; j++) 
    {
        flag = ((rcvlen & 0x80)>>7);
        c15 = (u8)((crc&0x8000)>>15);
         
        rcvlen <<= 1;
        crc <<= 1;
        if (c15^flag)
        {
            crc ^= 0x1021;
        }
    }
    DEBUG("calculate the crc is 0x%x,receive crc is 0x%x",crc,((data[len-2]<<8) + data[len-1]));
    if(crc != ((data[len-2]<<8) + data[len-1]))
    {		
	   	return false;
    }

	return true;
}

u8 calculate_check_sum(u8* data,u8 len)
{
	u8 i,temp[len],checkesum; 

	memcpy(temp,data,len);
	checkesum = len;
	for(i=0; i<len; i++)
	{
		checkesum += temp[i];
	}
	DEBUG("%d calculate the cksum is 0x%x,0-cksum is 0x%x",len,checkesum,0-checkesum);

	return 0-checkesum;
}

