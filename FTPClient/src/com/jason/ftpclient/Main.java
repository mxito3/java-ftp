package com.jason.ftpclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {
	
	private static Scanner ctrlScanner;
	private static PrintWriter ctrlWriter;
	private static InputStream dataIs;
	private static OutputStream dataOs;
	private static Scanner dataScanner;
	private static PrintWriter dataWriter;
	private static Scanner userInputScanner;
	
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
			//建立与control socket的连接
			Socket ctrlSocket = new Socket(addr,port);
			ctrlScanner = new Scanner(ctrlSocket.getInputStream());
			ctrlWriter = new PrintWriter(ctrlSocket.getOutputStream(),true);
			System.out.println("Control socket established to " + ctrlSocket.getInetAddress() + " port " + ctrlSocket.getPort());
			//获取control socket发给我的socket的id
			Long connectionId = ctrlScanner.nextLong();
			System.out.println("Received connection id from server: " + connectionId);
			//尝试连接到提供数据的socket
			Socket dataSocket = new Socket(addr,port+1);
			dataIs = dataSocket.getInputStream();
			dataOs = dataSocket.getOutputStream();
			dataScanner = new Scanner(dataIs);
			dataWriter = new PrintWriter(dataOs,true);
			//将刚刚control socket发给我的id发送给data server
			dataWriter.println(connectionId.toString());
			System.out.println("Data socket established to " + dataSocket.getInetAddress() + " port " + dataSocket.getPort());
			//获取键盘输入
			userInputScanner = new Scanner(System.in);
			
			String inputLine = "";
			String userCommand = "";
			StringBuilder userArg;
			while (! userCommand.equals("quit")) {
				
				userArg = new StringBuilder();
				System.out.print("ftp> ");
				
				//获取输入的命令的，多个命令以空格分开
				inputLine = userInputScanner.nextLine().trim();
				//commandStrings是单个命令的数组
				String[] commandStrings = inputLine.split(" ");
				if (commandStrings != null && commandStrings.length > 0 && !commandStrings[0].trim().isEmpty()) { //check for blank line
					
					
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
						if (do_get(userArg.toString().trim())) {
							
						} else {
							System.out.println("Server encountered an error");
						}
						break;
					case "mkdir":
						if(do_mkdir(userArg.toString().trim()))
						{
							System.out.println("mkdir "+userArg.toString().trim()+" with success!");
						}else
						{
							System.out.println("Server encountered an error");
						}
						break;
					case "list":
						do_list();
						break;
					case "quit":
						do_quit();
						userInputScanner.close();
						ctrlSocket.close();
						dataSocket.close();					
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

	private static boolean do_get(String fileName) {
		boolean result = false;
		File outFile = new File(fileName);
		try {
			if(outFile.exists()) {
			outFile.delete();
			}
			outFile.createNewFile();
			FileOutputStream fileOutputStream = new FileOutputStream(outFile);
			
			ctrlWriter.println("GET " + fileName);
			long size = dataScanner.nextLong();
			long len = 0;
			int recv = 0;
			if (size > 0) {
				while (len + recv < size) {
					len += recv;
					recv = dataIs.read(buff,0,buff.length);					
					fileOutputStream.write(buff,0,recv);
				}
			}
			
			fileOutputStream.close();
			if (ctrlScanner.next().equals("OK")) {
				System.out.println("Received file " + fileName);
				result = true;
			} else {
				outFile.delete();
			}
		
		} catch (IOException e) {
			System.out.println("Error while runing get: " +e);
		}
		return result;
	}

	private static boolean do_put(String fileName) {
		boolean result = false;
		//File用来新建一个文件对象来读取文件
		File inFile = new File(fileName);
		try {
			//读取文件内容到文件对象
			FileInputStream fileInputStream = new FileInputStream(inFile);
			ctrlWriter.println("PUT");
			dataWriter.println(fileName);
			dataWriter.println(inFile.length());
			int recv = 0;
			while ((recv = fileInputStream.read(buff, 0, buff.length)) > 0) {
            //read用来从文件对象读数据，每次读取一个字符，如果没到文件末尾他会将该字符当作整数值返回，如果到文件末尾，返回-1
				dataOs.write(buff,0,recv);
			}
			dataOs.flush();
			fileInputStream.close();
			if (ctrlScanner.next().equals("OK")) {
				//next读取一个单词，不带空格
				result = true;
			} else {
				System.out.println("Problem sending file to server.");
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("File " + fileName + " was not found");
		} catch (IOException e) {
			System.out.println("Problem transfering file for put: " + e);
		}
		
		return result;
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

}
