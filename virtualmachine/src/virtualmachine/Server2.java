package virtualmachine;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;


public class Server2 
{

	
	public void StartServer()
	{
		int port =1090;
		ServerSocket ss = null;
		Socket clientSocket;
		
		try 
		{
			ss= new ServerSocket(port);
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("SERVER ");
		System.out.println("Waiting for client:");

		try
		{
			clientSocket=ss.accept();
			System.out.println("Connection Accepted");
			BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			
			System.out.println(clientReader.readLine());
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	
	
	public static void main(String[] args) 
	{
		new Server2().StartServer();
	}

}
