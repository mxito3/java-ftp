package com.jason.ftpclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;

import javax.swing.plaf.synth.SynthStyle;




public class Main {
	
	private static Scanner ctrlScanner;
	private static PrintWriter ctrlWriter;
	private static InputStream dataIs;
	private static OutputStream dataOs;
	private static Scanner dataScanner;
	private static PrintWriter dataWriter;
	private static Scanner userInputScanner;
	private static int  runOrNot=0;
	private static byte[] buff = new byte[1024];
	
	public static void main(String[] args) {
		
		//check args
		if (args.length != 2) {
			System.out.println("USAGE: java -jar fileClient {ip address} {port}");
			return;
		}
		
		
		//get IP address and port
		InetAddress addr = null;
		try {
			 addr = (InetAddress) Inet4Address.getByName(args[0]);
		} catch (UnknownHostException e) {
			System.out.println("Couldn't resolve ip_address input to valid IP address");
			return;
		}
		Integer port = new Integer(args[1]);
		
		
		//set up the two socket connection
		try {
			Socket ctrlSocket = new Socket(addr,port);
			ctrlScanner = new Scanner(ctrlSocket.getInputStream());
			ctrlWriter = new PrintWriter(ctrlSocket.getOutputStream(),true);
			System.out.println("Control socket established to " + ctrlSocket.getInetAddress() + " port " + ctrlSocket.getPort());
			runOrNot=1;			
			Long connectionId = ctrlScanner.nextLong();
			System.out.println("Received connection id from server: " + connectionId);
			Socket dataSocket = new Socket(addr,port+1);
			dataIs = dataSocket.getInputStream();
			dataOs = dataSocket.getOutputStream();
			dataScanner = new Scanner(dataIs);
			dataWriter = new PrintWriter(dataOs,true);
			dataWriter.println(connectionId.toString());
			System.out.println("Data socket established to " + dataSocket.getInetAddress() + " port " + dataSocket.getPort());
			
			userInputScanner = new Scanner(System.in);
			
			String inputLine = "";
			String userCommand = "";
			StringBuilder userArg;
			while (! userCommand.equals("quit")) {
				if(runOrNot==0)
				{
					System.out.println("process end!");
					break;		
				}
				userArg = new StringBuilder();
				System.out.print("ftp> ");
				
				//get command and command string arguments
				
				inputLine = userInputScanner.nextLine().trim();
				
				String[] commandStrings = inputLine.split(" ");
				if (commandStrings != null && commandStrings.length > 0 && !commandStrings[0].trim().isEmpty()) { //check for blank lines
					
					userCommand = commandStrings[0].trim();	//the command given
					
					//reconstruct string following the command
					for (int i = 1; i<commandStrings.length; ++i) {
						userArg.append(commandStrings[i]);
						userArg.append(" ");
					}
					
					switch (userCommand) {
					case "put":
						if (do_put(userArg.toString().trim())) {
							
						} else {
							System.out.println("Server encountered an error");
						}
						break;
					case "get":
						if (get(userArg.toString().trim())) {
							
						} else {
							System.out.println("Server encountered an error");
						}
						break;
					case "list":
						do_list();
						break;
					case "mkdir":
						if (do_mkdir(userArg.toString().trim())) {
							System.out.println("mkdir "+userArg.toString().trim()+" with success!");
						} else {
							System.out.println("Server encountered an error");
						}
						break;
					case "touch":
						if (do_touch(userArg.toString().trim())) {
							System.out.println("create file "+userArg.toString().trim()+" with success!");
						} else {
							System.out.println("Server encountered an error");
						}
						break;
					case "delete":
						if (do_delete(userArg.toString().trim())) {
							System.out.println("delete file "+userArg.toString().trim()+" with success!");
						} else {
							System.out.println("Server encountered an error");
						}
						break;
					case "cd":
						if (system_call(userArg.toString().trim())) {
							
						} else {
							System.out.println("Server encountered an error");
						}
						break;
					case "help":
						do_help();				
					break;
					case "exit":
						do_quit();
						userInputScanner.close();
						ctrlSocket.close();
						dataSocket.close();	
						runOrNot=0;
					break;
					
					default:
						System.out.println("Invalid command.");
					}
				}
				
			}
		} catch (ConnectException e) {
			System.out.println("Could not connect to server.");
		} catch (IOException e) {
			System.out.println("There was a problem: " + e);
			e.printStackTrace();
		}
	}

	private static void do_quit() {
		ctrlWriter.println("CLOSE");		
	}

	private static void do_list() {
		ctrlWriter.println("LIST");
		System.out.println("\nFTP Server contents:");
		
		
		String fileName = dataScanner.nextLine();
		while(!fileName.equals("$")) {
			System.out.println(fileName);
			fileName = dataScanner.nextLine();
		}
		System.out.println();
	}

	

	private static boolean do_mkdir(String dirName)
	{
		boolean result=false;
		ctrlWriter.println("mkdir");
		dataWriter.println(dirName);
		//接受服务器发来的是否成功的消息
		if(ctrlScanner.next().equals("OK"))
		{
			result=true;
		    
		}
		  return result;

	}
	private static boolean do_touch(String fileName)
	{
		boolean result=false;
		ctrlWriter.println("touch");
		dataWriter.println(fileName);
		//接受服务器发来的是否成功的消息
		if(ctrlScanner.next().equals("OK"))
		{
			result=true;
		    
		}
		return result;

	}
	private static boolean do_delete(String fileName)
	{
		boolean result=false;
		ctrlWriter.println("delete");
		dataWriter.println(fileName);
		//接受服务器发来的是否成功的消息
		if(ctrlScanner.next().equals("OK"))
		{
			result=true;
		    
		}
		  return result;

	}
	private static Boolean system_call(String command)
	{
		boolean result=false;
		System.out.println("is"+command);
		ctrlWriter.println("cd");
		dataWriter.println(command);
		if(ctrlScanner.next().equals("OK"))
		{
			result=true;
		}	
		return result;
	}
	private static void do_help()
	 {
		 //check os
		String osName=getOS();
		String filePath="";
		if(osName.startsWith("Windows")){//windows下调用系统命令  
            filePath=System.getProperty("user.dir")+"\\"+"help.txt";
        }else if(osName.startsWith("Linux")){//Linux下调用系统命令  
        	filePath=System.getProperty("user.dir")+"/"+"help.txt";
        }
		 
         
		 char[] buf = new char[1024];  
		 int num;  
		 File file=new File(filePath);
		 if(!file.exists())
		 {
			 System.out.println("找不到help文件");
			 return;
		 }
		 
		 try {
			 FileReader fr = new FileReader(filePath);  
			while ((num = fr.read(buf)) != -1) {  
			         System.out.print(new String(buf, 0, num));  
			     }
			fr.close();  
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		// TODO Auto-generated constructor stub
	}

public static String getOS(){  
    Properties pros = System.getProperties();  
    String os = (String) pros.get("os.name");  
    return os;  
}  



private static boolean get(String fileName) {
	boolean result = false;
	File outFile = new File(fileName);
	try {
		if(outFile.exists()) {
		outFile.delete();
		}
		outFile.createNewFile();
		FileOutputStream fileOutputStream = new FileOutputStream(outFile);
		
		ctrlWriter.println("GET " + fileName);
		if(ctrlScanner.hasNext())
		{
			String haveOrNot=ctrlScanner.next().trim();
			
			if(haveOrNot.equals("no"))
			{
				System.out.println("server has no file named "+fileName);
				
			}
			else if(haveOrNot.equals("yes"))
			{
				long size = dataScanner.nextLong();
				long len = 0;
				int recv = 0;
				int times=0;
				if (size > 0) {
					while (len<size) {
						recv = dataIs.read(buff,0,buff.length);
						if(recv!=-1)
						{
							len += recv;
						}
						fileOutputStream.write(buff,0,recv);
						times++;
						System.out.println("times "+times);
					}
				}
				
				fileOutputStream.close();
				if(ctrlScanner.hasNext())
				{
					if (ctrlScanner.next().trim().equals("OK")) {
						System.out.println("Received file " + fileName);
						result = true;
					} else {
						outFile.delete();
					}
				}
			}
			else {
				System.out.println("提示信息出问题了");
			}
		}
	} catch (IOException e) {
		System.out.println("Error while runing get: " +e);
	}
	
	return result;
}

private static boolean do_put(String files) throws IOException
{
	System.out.println("files is "+files);
	boolean result = false;
	int successTime=0;
	String[] fileAll=files.split(" ");
  	String fileName="";
  	StringBuilder noSuchFile = null;
  	//noSuchFile.length=0;
  	//判断这些文件里面有没有不存在的
  	int wrongFileName=0;
  	for(int i=0;i<fileAll.length;i++)
  	{
  		fileName=fileAll[i];
  		System.out.println("for里面"+fileName);
		File inFile=new File(fileName);
  		if(!inFile.exists())
		{
  			System.out.println("file "+fileName+" doesn't exit");
  			wrongFileName++;
		}
  	}
  	if(wrongFileName>0)
  	{
  		return true;
  	}
  	else  //没不存在的文件
  	{
  		for(int i=0;i<fileAll.length;i++)
  		{
 
  				if(put_one(fileAll[i]))
  				{
  					successTime++;
  				}
  			
  			System.out.println("已成功次数是 "+successTime);
  			
  		}
  	}

	if(successTime==fileAll.length)
	{
		result=true;
	}
	return result;
}


private static boolean put_one(String fileName) throws IOException {	
	System.out.println("名字是 "+fileName);
	boolean result = false;
	String fname = fileName.trim();
	File inFile = new File(fname);
	LineNumberReader  lnr = new LineNumberReader(new FileReader(inFile));
	lnr.skip(Long.MAX_VALUE);
	lnr.close();
	if(inFile.length()!=0)
	{
		ctrlWriter.println("PUT " + fileName);
		InputStream fileStream;		
		try {
			fileStream = new FileInputStream(inFile);
			dataWriter.println(lnr.getLineNumber() + 1);
			int recv;		
			int times=0;
			while ((recv = fileStream.read(buff, 0, buff.length)) > 0) {
				dataOs.write(buff,0,recv);
				times++;
				System.out.println("times "+times);
			}
			dataOs.flush();
			fileStream.close();
			ctrlWriter.println(" finish");
			System.out.println("sent file " + fname);
			if (ctrlScanner.next().equals("OK")) {
				System.out.println("send file "+fileName+" to server with success!");
				result = true;
			} 
		
		} catch (IOException e) {
			System.out.println("Error receiving file." + e);
		}			
	
	}
	else
	{
		result=do_touch(fileName);
		if(result)
		{
			System.out.println("send file "+fileName+" to server with success!");
		}
	}
	return result;
}

}