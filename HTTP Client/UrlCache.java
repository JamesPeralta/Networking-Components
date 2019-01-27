/**
 * Implements an HTTP Client
 *
 * @author 	James Peralta
 * @version	3.2, Oct 5, 2017
 *
 */
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.io.File;

public class UrlCache {

	HashMap<String,String> catalog;
    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
     * @throws IOException if encounters any errors/exceptions
     */
	@SuppressWarnings("unchecked")
	public UrlCache() throws IOException
    {
		File tmpDir = new File(System.getProperty("user.dir")+ "/data.bin");
		//If there doesn't exist a Local Cache, Create one
		if(!tmpDir.exists())
        {
			//initialize the Hashmap for the catalog
			catalog = new HashMap<String,String>();
		}
		//else get the local Cache from the file data.bin
		else
        {
			//Gets the cache and stores it into the variable catalog
			try 
			{
				ObjectInputStream getMap = new ObjectInputStream(new FileInputStream(tmpDir));
				catalog = (HashMap<String,String>) getMap.readObject();
			} 
			catch (ClassNotFoundException e) 
			{
				e.printStackTrace();
			}			
		}
	}
	
    /**
     * Saves the HashMap after execution 
	 *
    **/
	public void saveHashMap()
    {
		try
        {
			ObjectOutputStream storeMap = new ObjectOutputStream(new FileOutputStream(System.getProperty("user.dir")+ "/data.bin"));
			storeMap.writeObject(catalog);
		}
        catch (IOException e)
        {
			e.printStackTrace();
		}
	}
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException
    {
		//extracts hostname, URl, ETC
		String[] urlArray = url.split("/");
		String hostName, fixedUrl = "";
        //Initaialize as port 80
		int portNumber = 80;
        if(urlArray[0].contains(":"))
        {
			//retrieve the host name and port number
			String[] urlArraySplit = urlArray[0].split(":");
            hostName = urlArraySplit[0];
            portNumber = Integer.parseInt(urlArraySplit[1]);
            //Put together path to file
            fixedUrl = "";
            for(int i = 1; i < urlArray.length; i++){
				fixedUrl = fixedUrl + "/" + urlArray[i];
            }
		}
		//else it doesnt include port number
		else
        {
			hostName = urlArray[0];
			//use default port number
			portNumber = 80;
            //Put together path to file
			fixedUrl = "";
			for(int i = 1; i < urlArray.length; i++)
            {
				fixedUrl = fixedUrl + "/" + urlArray[i];
			}	
		}
		
		//Sends the conditionalGet to the server
		serverConditionalGet(hostName, portNumber, fixedUrl, url, catalog.containsKey(url));	
	}
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     */
	public long getLastModified(String url) {
		
		long millis = 0;
		
		Date date = new Date();
		String lastModified = catalog.get(url);
		
		
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
		Date date1 = format.parse(lastModified, new ParsePosition(0));
		format.format(date);
		//System.out.println("To GMT: "+ date1);
		
		millis = date1.getTime();
		
		return millis;
	}
	
    /**
     * This function sends the HTTP request and calls the function storeData() if server returns message 200
     * 
     * @param hostName 	The name of the Website
	 * @param portNumber The port number to connect socket too
	 * @param the url of the file to retrieve
	 * @param the fullUrl to the website
	 * @param inCatalog true means do a conditional get else just grab from server
    **/
	public void serverConditionalGet(String hostName, int portNumber ,String url,String fullUrl, boolean inCatalog)
    {
		//retrieve input from server
		InputStream inputStream; 
		//send output to the server
		OutputStream outputStream; 
		//Server message local Variables

		//Ask if we need output stream
		try
        {
			
			//Setup TCP Connection
			Socket socket = new Socket(hostName, portNumber);
			//initialize outputStream which will send HTTP Requests to Server
			outputStream = new DataOutputStream(socket.getOutputStream());
			//initialize the input stream which will receive data in bytes from the Server
			inputStream = socket.getInputStream();
				
			String getRequest;
			//Set-up HTTP get Request
			if(inCatalog == true)
            {
				getRequest = "GET " + url + " HTTP/1.0\r\n"+"Host: " + hostName + "\r\n"+"If-Modified-Since: " + catalog.get(fullUrl) + "\r\n\r\n";
			}
			else
            {
				getRequest = "GET " + url + " HTTP/1.0\r\n"+"Host: " + hostName + "\r\n"+"If-Modified-Since: " + "Fri, 02 Oct 1971 21:23:47 GMT" + "\r\n\r\n";
			}
			
			//Send HTTP get request
			outputStream.write(getRequest.getBytes("US-ASCII"));
			outputStream.flush();
			
			//Receive the the input request------------------------------------

			// /r is 13 and /n is 10
			String theLine = "";
			//loop that reads through the header 
			ArrayList<String> headerData = new ArrayList<String>();
			while(true)
            {
				theLine = readLine(inputStream);
				//breaks loop when header is done reading
				if(theLine.equals("\r\n"))
                {
					break;
				}
				headerData.add(theLine);
			}
			
			//System.out.println(headerData.size());
			
			//Check the header to see if It already has most recent copy
			//If server header returns "304 Not Modified” do nothing
			if(headerData.get(0).contains("304"))
            {
				System.out.println("Catalog has most recent copy of "+ fullUrl);
			}
			//If server header returns "200 OK” get the object
			if(headerData.get(0).contains("200"))
            {
				String myNew = headerData.get(3).replace("Last-Modified:","").trim();
				System.out.println("Storing:" + fullUrl + "  with time:" + myNew);
				//add URL and server Message into the HashMap
				catalog.put(fullUrl, myNew);
				saveHashMap();
				storeData(inputStream, url);
			}
						
			//Cancel Connection with the server
			inputStream.close();
			outputStream.close();
			socket.close();			
		}

		catch (Exception e)
        {
			System.out.println("Error: " + e.getMessage());
		}
	}
	
   /** Stores the inputSteam to directory url when called
     * 
     * @param inputStream - Contains the body of the server HTTP response
	 * @param url - Is the directory path that it will save
	 *
    **/
	public void storeData(InputStream inputStream, String url)
    {
		try
        {
			File myFile = new File(System.getProperty("user.dir") + url);
			//if myFile exists 
			if(myFile.exists())
            {
				//If the file is already there update it		
				OutputStream writer1 = new FileOutputStream(myFile);
				//Reads the file by 16*1024 byte chunks
			    byte[] bytes = new byte[16*1024];
			    int count;
		        while ((count = inputStream.read(bytes)) > 0)
                {
                    writer1.write(bytes, 0, count);
			    }
				writer1.close();		
			}
			else
            {
				myFile.getParentFile().mkdirs(); 
				myFile.createNewFile();						
				OutputStream writer1 = new FileOutputStream(myFile);
                //Reads the file by 16*1024 byte chunks
			    byte[] bytes = new byte[16*1024];
			    int count;
		        while ((count = inputStream.read(bytes)) > 0)
                {
                    writer1.write(bytes, 0, count);
			    }
				writer1.close();
			}
		} 
		catch (IOException e)
        {
			e.printStackTrace();
		}	
	}
	
	
   /** 
     * Reads input stream until it see's the sequence "\r\n"
     * 
     * @param inputStream - Contains the line of request that the method will read
	 *
   **/
	public String readLine(InputStream inputStream){
		try {
			//This is the string representing the line
			String theLine = "";
			int lastChar = 0;
			//runs loop until "\r\n"
			while(true)
			{		
				//temp stores the byte
				int temp;
				temp = inputStream.read();
				//if temp = -1 it is the end of the string
				if(temp == -1)
                {
					break;
				}
				//Creates the string that represents the line
				char theChar1 = (char) temp;
				theLine = theLine + Character.toString(theChar1);
				//if last char is \r and this char is \n break out
				if(lastChar == 13 && temp == 10)
				{
					break;		
				}
				lastChar = temp;				
			}
			
			//return the Line read
			return theLine;
		} 
		catch (IOException e)
        {
			return "error reading a line";
		}
	}	
}
