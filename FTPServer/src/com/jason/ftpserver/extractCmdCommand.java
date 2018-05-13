package com.jason.ftpserver;

import java.util.Properties;
import java.util.Scanner;


public class extractCmdCommand {
	
	public static String getOS(){  
	    Properties pros = System.getProperties();  
	    String os = (String) pros.get("os.name");  
	    System.out.println(os);  
	    return os;  
	}  
	  public  static void main(String[] args) {
		  
	}
	public  boolean extract(String command) {
		boolean result=false;
		Process process=null;
		String processPrint="";
		Scanner processPrintScanner=null;
		String osName=getOS();
		try {	
				if(osName.startsWith("Windows")){//windows下调用系统命令  
		            String[] cmdWindows = {"cmd.exe","/c",command};  
		            process = Runtime.getRuntime().exec(cmdWindows);  
		        }else if(osName.startsWith("Linux")){//Linux下调用系统命令  
		            String[] cmdLinux = {"/bin/sh","-c",command};  
		            process = Runtime.getRuntime().exec(cmdLinux);  
		        }
			
			processPrintScanner=new Scanner(process.getInputStream());
			while(processPrintScanner.hasNextLine())
			{
				processPrint=processPrintScanner.nextLine();
				System.out.println(processPrint);		
			}
			if(process.waitFor()==0)
			{
				result=true;
			}

		} catch (Exception e) {
			System.out.println(e);
			// TODO: handle exception
		}
		return result;
	}
}