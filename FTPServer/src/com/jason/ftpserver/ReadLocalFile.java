package com.jason.ftpserver;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;  
import java.io.IOException;
import java.nio.file.Path;
public class ReadLocalFile {

	public static void main(String[] args) {

		System.out.println(System.getProperty("user.dir"));//user.dir指定了当前的路径
		  Read(System.getProperty("user.dir")+"\\"+"help.txt");
	}
	public static void Read(String filePath) {
		 
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
}
