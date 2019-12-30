package virtualmachine;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client 
{
	static ObjectInputStream   ois =null;
	static ObjectOutputStream  oos =null;	
	static BufferedReader keyboard ;
	static Scanner cin ;
	static DataInputStream dis;
	static InputStream is;
	static BufferedReader clientReader;
	static BufferedWriter writer;
	static Socket client;
	static long z =0;
	static OutputStream os;
	
	String hostname="10.0.0.2";
	int portnumber = 2112;
	static long size;
	static boolean check = false;
	
	public Client(int check1 ) throws UnknownHostException, IOException
	{
		Socket client = new Socket(hostname,portnumber);
		
		byte[] buffer = new byte[1024];
		int bytesRead = buffer.length;
		

		 keyboard = new BufferedReader(new InputStreamReader(System.in));
		 cin = new Scanner(System.in);
		 dis = new DataInputStream (client.getInputStream());
		 is = client.getInputStream();
		 clientReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
		 writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
		 os = client.getOutputStream();
		 
		 recieveData(check1);

	}
	
	/**
	 * This method is the main function working on the client side. 
	 * The primary role of this function is to receive the data that is sent by the server. 
	 * This function is implemented in such a way that it will only receive those file which are not already present on the client side. 
	 * If a file or a resource is present on the client side, it will skip the downloading of that particular file from the server. 
	 * The receiving or downloading of file is implemented using various java streams. 
	 * As soon as a connection between client and the server gets established, an acknowledgment is sent from the server to the client that it is 
	 * about to send the files associated with a particular virtual machine. As the soon as the acknowledgment is received from the server, 
	 * client prepares itself for receiving the data from the server by opening its reading streams. 
	 * The working of client is independent of the working of the hypervisor or working of the virtual machine.
	 * 
	 * @param check1
	 * @throws IOException
	 */
	
	public static void recieveData(int check1) throws IOException
	{
        if(check1 ==1)
        {
		os.write(1);
		os.flush();
        }
        else
        {
    		os.write(10);
    		os.flush();
        }
		
		
        String path = clientReader.readLine();
           
        
        List <String> names = new ArrayList<String>();
		String msg ;
		int lim = clientReader.read();
	
		for(int i = 0 ; i<lim ; i++)
		{
			msg = clientReader.readLine();
			names.add(msg);

		}
        
		System.out.println("Index     Name of File");
		System.out.println("==================================================");
		
		String[] newNames = new String[names.size()];
		for(int i = 0 ; i<names.size() ; i++)
		{
			newNames[i] = names.get(i);
			System.out.println(i +"         " + names.get(i));
		}
		
		
        	   
//-------------------------------------------------------------------------------------			
		
		File f1 = new File("");
		
		

		String p = f1.getAbsolutePath();
		
		if (check1 ==1)
		{
			 p = f1.getAbsolutePath()+"/Snapshots/";
		}
		CharSequence logs = "/Log";
    	CharSequence snps = "/Snapshots";
    	 	
    	
        if 	(path.contains(logs))
        {
        	
        	File theDir = new File("Logs");

     		String dir = theDir.getAbsolutePath();
    		// if the directory does not exist, create it
    		if (!theDir.exists())
    		{
    			System.out.println("==================================================");
    		    System.out.println("creating directory: " + dir);
    		    boolean result = false;

    		    try
    		    {
    		        theDir.mkdir();
    		        result = true;
    		    } 
    		    catch(SecurityException e)
    		    {
    		        System.out.println(e.getMessage());
    		    }        
    		    if(result)
    		    {    
    		        System.out.println("Directory Created");  
    		    }
    		}
        	  	
        	p = p+"/Logs/";        	

        

        }
        else if 	(path.contains(snps))
        {
        	
        	File theDir = new File("Snapshots");

     		String dir = theDir.getAbsolutePath();
    		// if the directory does not exist, create it
    		if (!theDir.exists())
    		{	
    			System.out.println("==================================================");
    		    System.out.println("creating directory: " + dir);
    		    boolean result = false;

    		    try
    		    {
    		        theDir.mkdir();
    		        result = true;
    		    } 
    		    catch(SecurityException e)
    		    {
    		        System.out.println(e.getMessage());
    		    }        
    		    if(result)
    		    {    
    		        System.out.println("Directory Created");  
    		    }
    		}
        	  	

        }
        else
        	p = p + "/";
    	
	    System.out.println(p); 
	    File f2 = new File(p);
		File [] list = f2.listFiles();
		String [] name = new String[list.length];
		for (int i =0 ; i <list.length;i++)
		{
			if (list[i].isDirectory())
			{
				name[i] = list[i].getName()+("  Folder ");
				
			}
			else
			{
				name[i] = list[i].getName();
			}
		

		}
		
		System.out.println("==================================================");
		System.out.println("Files That Are Already Presend In Current Directory");
		System.out.println("==================================================");
		for (int i =0 ; i <name.length;i++)
		{
			System.out.println(i+ " "+name[i]);
		}
			
		
        List<String> s1List = new ArrayList(Arrays.asList(newNames));

        for(int i =0;i<name.length;i++)
        {
            if (s1List.contains(name[i])) 
            {
                s1List.remove(name[i]);
            } else {
                s1List.add(name[i]);
            }
//             System.out.println("intersect on " + s1List);
        }
        
		
        List <Integer> indexes = new ArrayList<Integer>();

    	System.out.println("==================================================");
		System.out.println("Files That Are Not Already Presend In Current Directory");
		System.out.println("==================================================");
		
		for(int i =0;i < s1List.size();i++)
		{
			for(int j =0;j < newNames.length;j++)
			{
				if(s1List.get(i).equals(newNames[j]))
				{
					System.out.println( j +" " +newNames[j]);
					indexes.add(j);
				}
				
			}
		}
		
		if (check1 == 10)
		{
			indexes.add(0);
		}
				
	
		System.out.println("==================================================");
		
		int length = indexes.size();
		writer.write(length);
		writer.flush();
		
		
		for(int i = 0; i<indexes.size();i++)
		{
			System.out.println(indexes.get(i));
			
			System.out.println(names.get(indexes.get(i)));
			CharSequence a = "Folder";
    		String s = names.get(indexes.get(i));
    		boolean subCheck = s.contains(a);
    		System.out.println(s.contains(a));
	
    		
			
			if (subCheck== false)
			{
				int g = indexes.get(i);
				System.out.println(g);
				os.write(indexes.get(i));
				os.flush();
				
				FileOutputStream fos = new FileOutputStream(p+names.get(indexes.get(i)));
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				
				size =dis.readLong();
				System.out.println("size = "+size);
				
				z =0;
				int bytesRead;
				byte[] buffer = new byte[1024];
				
				while ((bytesRead = is.read(buffer, 0, 1024))>0)
				{
					z = z+bytesRead;	
					bos.write( buffer,0,bytesRead);
					bos.flush();
					
					if (z==size)
					{
						break;
					}
				}
				
		
				System.out.println("Succesufully downloaded the File :)");


			}
			
			else
			{
				writer.write(100);
				writer.flush();
			}
		
			
		} 
		
		check = true;
	}
	
	
}


