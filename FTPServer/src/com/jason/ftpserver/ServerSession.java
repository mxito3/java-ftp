package com.jason.ftpserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ServerSession implements Runnable {
	
	private Socket controlSocket;
	private Socket dataSocket;
	
	private Scanner controlScanner;
	private PrintWriter controlWriter;
	
	Path currentPath = Paths.get(".");
	private Scanner dataScanner;
	private InputStream dataIs;
	private OutputStream dataOs;
	private PrintWriter dataWriter;
	private byte[] buff = new byte[1024];	
	private  extractCmdCommand SystemExtract=new extractCmdCommand();
	//store socket and get input stream scanner
	public ServerSession(Socket controlSocket, Socket dataSocket) {
		super();
		this.controlSocket = controlSocket;		
		this.dataSocket = dataSocket;		
		try {
			this.controlSocket.setSoTimeout(0);
			this.dataSocket.setSoTimeout(0);
			controlScanner = new Scanner(controlSocket.getInputStream());
			controlWriter = new PrintWriter(controlSocket.getOutputStream(),true);
			dataIs = dataSocket.getInputStream();
			dataOs = dataSocket.getOutputStream();
			dataScanner = new Scanner(dataIs,"UTF8");
			dataWriter = new PrintWriter(dataOs,true);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Problem getting input and/or outputsreams for data and/or control sockets:" + e);
		}
	}
	
	
	//entry point for the control socket
	@Override
	public void run() {
		
		String cmd = controlScanner.next();
		
		while (!cmd.equals("CLOSE")) {
			System.out.println("Recieved command: " + cmd);
			switch (cmd) {
			case "LIST":
				do_list();
				break;
			case "GET":
				if (get()) {
					controlWriter.println("OK");
					System.out.println("我返回了okay");
				} else {
					
				}
				break;
			case "PUT": 
				if (put()) {
					controlWriter.println("OK");
					System.out.println("我返回了okay");
				} else {
					controlWriter.println("ERROR");
				}
				break;
			case "mkdir":
				if(do_mkdir())
				{
					controlWriter.println("OK");
				}
				else {
					controlWriter.println("ERROR");
				}
				break;
			case "touch":
				if(do_touch())
				{
					controlWriter.println("OK");
				}
				else {
					controlWriter.println("ERROR");
				}
				break;
			case "delete":
				if(do_delete())
				{
					controlWriter.println("OK");
				}
				else {
					controlWriter.println("ERROR");
				}
				break;
			case "cd":
				if(SystemCall("cd"))
				{
					controlWriter.println("OK");
				}
				else {
					controlWriter.println("ERROR");
				}
			default:
				System.out.println("Invalid socket control message received");
				controlWriter.println("INVALID");
				break;				
			}
			cmd = controlScanner.next();

		}
		try {
			System.out.println("Session ended from " + controlSocket.getInetAddress() +" port " + controlSocket.getPort());
			controlSocket.close();
			dataSocket.close();
		} catch (IOException e) {
			System.out.println("Problem closing control and/or data socket " + e);
		}
	}	

	

public boolean put() {
		
	boolean result = false;
	String fileName ="";
	long size=0L;
	if(controlScanner.hasNext())
	{
		fileName = controlScanner.next().trim();
	}
	
	System.out.println("服务器收到名字是   "+fileName);
	File outFile = new File(fileName);
	try {
		if(outFile.exists()) {
		outFile.delete();
		}
		outFile.createNewFile();
		
		OutputStream fileOutputStream =new FileOutputStream(outFile); 
		if(dataScanner.hasNextInt())
		{
			size = dataScanner.nextInt();
		}
		
		System.out.println("服务器收到long值是   "+size);
		if(controlScanner.hasNext())
		System.out.println("我发的结束标志"+controlScanner.next().trim());
		
		/*long len = 0;
		int recv = 0;
		int times=0;
		if (size > 0) {
			while (len<size) {
				//This method blocks until input data is available, end of file is detected
				if(dataScanner.hasNextLine())
				{
					System.out.println("有");
				}
				recv = dataIs.read(buff,0,buff.length);
				if(recv!=-1)
				{
					len += recv;
					fileOutputStream.write(buff,0,recv);
					times++;
					System.out.println("times "+times);
				}
				else
				{
					break;
				}
				
			}
		}
		
		*/
		for(int i=0;i<size;)
		{
			if(dataScanner.hasNextLine())
			{
				String neirong=dataScanner.nextLine()+"\n";
				i++;
				System.out.println("在line循环里面 内容是"+neirong);
				fileOutputStream.write(neirong.getBytes());
				//fileOutputStream.write("\n");
				fileOutputStream.flush();
			}	
		}
		fileOutputStream.close();
		
		System.out.println("出来了");
		System.out.println("要返回true了");
		result = true;	
	} catch (IOException e) {
		System.out.println("Error while runing get: " +e);
	}
	return result;	
	}

	
	/**
	 * List the contents of the directory and send them to the client
	 */
	public void do_list() {
		File[] dirList = currentPath.toFile().listFiles();
		for (File f : dirList) {
			dataWriter.println(f.getName());
		}
		dataWriter.println("$");
	}
	
	public boolean do_mkdir() {
		String dirName="";
		boolean result=false;
		
		if(dataScanner.hasNextLine())
		{
			dirName=dataScanner.nextLine();
			System.out.println(currentPath);
			File newdir=new File(dirName);
			if(newdir.exists())
			{
				System.out.println("文件夹创建失败，该文件夹已存在");
			}
			else
			{
				if(newdir.mkdirs())
				{
					result=true;
				}
			}
		}
		return result;
	}
	
	public boolean do_touch() {
		String fileName="";
		boolean result=false;
		
		if(dataScanner.hasNextLine())
		{
			fileName=dataScanner.nextLine();
			File newfile=new File(fileName);
			if(!newfile.exists())
			{
				try {
					if(newfile.createNewFile())
					{
						result=true;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else {
				result=true;
			}
			
		}
		return result;
	}
	public boolean do_delete() {
		String fileName="";
		boolean result=false;
		if(dataScanner.hasNextLine())
		{
			fileName=dataScanner.nextLine();
			File file=new File(fileName);
			if(file.exists())
			{
				if(file.isFile())
				{
					try {
						if(file.delete())
						{
							result=true;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if (file.isDirectory()) {
					result=deleteDir(file);
				}
				
				
			}
			
			
		}
		return result;
	}
	private boolean deleteDir(File dirName)
	{
		boolean result=false;
		  String[] children = dirName.list();
	            for (int i=0; i<children.length; i++) {
	                result = deleteDir(new File(dirName, children[i]));
	                if (!result) {
	                    return false;
	                }
	            }
        if(dirName.delete())
        {
        	result=true;
        }
		return result;
	}
	
	private  boolean SystemCall(String command)
	{
		boolean result=false;
		String parament="";
		if(command=="cd")
		{
			
		}
		if(dataScanner.hasNextLine())
		{
			parament=dataScanner.nextLine();
			System.out.println("parament is"+command+" "+parament);
			if(SystemExtract.extract(command+" "+parament))
			{
				result=true;
			}

		}
		return result;
	}




public boolean get() {		
	boolean result = false;
	String fname = controlScanner.nextLine().trim();
	File inFile = new File(fname);
	
	
		
	if (inFile.exists()) {
		controlWriter.println("yes ");
		InputStream fileStream;	
		//LineNumberReader lnr;
		try {
			/*lnr = new LineNumberReader(new FileReader(inFile));
			lnr.skip(Long.MAX_VALUE);
			int lineAll=lnr.getLineNumber() + 1;*/
			fileStream = new FileInputStream(inFile);
			dataWriter.println(inFile.length());
			int recv;		
			int times=0;
		
			while ((recv = fileStream.read(buff, 0, buff.length)) > 0) {
				dataOs.write(buff,0,recv);
				times++;
				System.out.println("times "+times);
			}
			dataOs.flush();
			fileStream.close();
			System.out.println("sent file " + fname);
			result = true;
		} catch (IOException e) {
			System.out.println("Error receiving file." + e);
		}			
	} else {
		controlWriter.println("no ");
	}
	return result;
}
}