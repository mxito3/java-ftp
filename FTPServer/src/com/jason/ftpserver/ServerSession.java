package com.jason.ftpserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
			dataScanner = new Scanner(dataIs);
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
				if (do_get()) {
					controlWriter.println("OK");
				} else {
					controlWriter.println("ERROR");
				}
				break;
			case "PUT": 
				if (do_put()) {
					controlWriter.println("OK");
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

	
	/**
	 * Sends an existing file to a client
	 */
	public boolean do_get() {		
		boolean result = false;
		String fname = controlScanner.nextLine().trim();
		File inFile = new File(fname);
		if (inFile.exists()) {			
			InputStream fileStream;			
			try {
				fileStream = new FileInputStream(inFile);
				dataWriter.println(inFile.length());
				int recv;				
				while ((recv = fileStream.read(buff, 0, buff.length)) > 0) {
					dataOs.write(buff,0,recv);
				}
				dataOs.flush();
				fileStream.close();
				System.out.println("sent file " + fname);
				result = true;
			} catch (IOException e) {
				System.out.println("Error receiving file." + e);
			}			
		} else {
			System.out.println("File " + inFile + " does not exist.");
		}
		return result;
	}
	
	/**
	 * Client is putting a file on the server
	 * @return
	 */
	public boolean do_put() {
		
		boolean result = false;
		String fileName = dataScanner.next();
		try {
			System.out.println("�������յ�������   "+fileName);
			//set up the output file
			File outFile = new File(fileName);
			if (outFile.exists())
				outFile.delete();
			outFile.createNewFile();			
			OutputStream fileStream = new FileOutputStream(outFile);
			
			//read and write the file data
			long len = 0;
			long size = dataScanner.nextLong();
			int recv = 0;
			if (size > 0) {
				while(len + recv < size) {
					len += recv;
					recv = dataIs.read(buff, 0, buff.length);
					fileStream.write(buff,0,recv);
				}
			}
			
			fileStream.close();
			result = true;			
			
		} catch (IOException e) {
			System.out.println("Problem creating output file stream: " + e);
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
				System.out.println("�ļ��д���ʧ�ܣ����ļ����Ѵ���");
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

}
