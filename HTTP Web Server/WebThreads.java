/**
 * 
 * WebThreads class 
 * 
 * 
 */

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.nio.file.Files;
import java.nio.*;

public class WebThreads extends Thread
{
    //Properties 
    private Socket socket; 
    private InputStream inputStream; 
    /**
     * Default constructor to initialize the working thread 
     * @param threadNumber - ID of the thread 
     * @param socket - which socket will the thread be working on 
     * 
     * 
     */
    public WebThreads(Socket socket)
    {
        this.socket = socket;
    }

    /**
     * 
     * This is the execution the thread will perform 
     * 
     * 
     */
    public void run()
    {  
        //Properties
        byte[] httpResponse; 
        String[] getRequestParts; 
        int statusCode; 
        FileInputStream fileInputStream; 
        try
        {
            System.out.println("Thread connected");    
            
            //get InputStream provided by client server 
            inputStream = socket.getInputStream();


            /** Call getGetrequest, provides first GET line request, broken down into its parts 
             * 
             *  The we call properGetRequest, and returns statusCode, 
             * if OK request return 200
             * if Bad request return 400 
             */
            getRequestParts = getGetrequest(inputStream);
            statusCode = properGetRequest(getRequestParts); 
          
            //Proper HTTP request
            if(statusCode == 200){
                
                //Check if the file exists
                //we are going to remove the "/" so we have use object File
                String fileName = getRequestParts[1].substring(1, getRequestParts[1].length() ); 
                File file = new File(fileName);
                
                //check if file exists 
                if(file.exists()){

                    //create buffer for File 
                    byte[] httpResponseHeader; 
                    byte[] httpResponseFileByteForm = new byte[(int) file.length()];

                    //Get header of file, which contains basic info such as 
                    //date, last-time-modified, server, etc. 
                    //getValidGETRequestBytesForm returns header in byte[] form

                    httpResponseHeader = getValidGETRequestBytesForm(file , getRequestParts);

                    //Load file inputStream, and load to buffer
                    fileInputStream = new FileInputStream(file); 
                    fileInputStream.read(httpResponseFileByteForm); 

                    //Push Information to socket
                    socket.getOutputStream().write(httpResponseHeader); 
                    socket.getOutputStream().write(httpResponseFileByteForm); 
                    socket.getOutputStream().flush(); 
                    
                    //close InputStream object
                    fileInputStream.close(); 
                    //   
                }
                else
                {
                    //file not found, set statusCode to 404.
                  statusCode = 404; 
                }   
            }

            //bad HTTP request or HTTP request can not be found 
            if(statusCode == 400 || statusCode == 404)
            {
               
                //get erros massage
                //we use function getInvalidGETResquestBytesForm, that returns Bad Message the massage in byte[] form
                httpResponse = getInvalidGETResquestBytesForm(getRequestParts[2],statusCode); 

                //sent message to socket
                socket.getOutputStream().write(httpResponse);
                socket.getOutputStream().flush(); 
            }

            //Close 
            inputStream.close(); 
            socket.close();
            System.out.println("Thread Terminated");
        }
        catch(Exception e) {
        }
    }
    
    /**
     * 
     * fileExists
     * @return returns bool, if exist return true , otherwise return false
     */
    public byte[] getValidGETRequestBytesForm(File file, String[] httpGetRequest)
    {
        //Properties 
        String httpHeader;
        byte[] httpHeaderByteForm = null; 
        String contentType = "Defualt";
        
        //Get contentType, of file 
        try
        {
            contentType = Files.probeContentType( file.toPath() );
        }
        catch(Exception e)
        {
            System.out.println("Location:getValidGETRequestBytesForm\nError: "+e);
        }
        //Header message in String form 
        httpHeader = httpGetRequest[2] +" 200 OK\r\n"
                    +"Date: " + getCurrentDate()+"\r\n"
                    +"Last-Modified: "+ getLastModifiedForHTTPForm(file) +"\r\n"
                    +"Content-type: "+ contentType +"\r\n"
                    +"Connection: close\r\n"
                    + "\r\n"; 

        //convert Header message to byte[]
        try
        {
            httpHeaderByteForm = httpHeader.getBytes("US-ASCII");
        }
        catch(Exception e)
        {
            System.out.println("location: getValidGETRequestBytesForm\nError: "+ e);
        }

        //return message
        return httpHeaderByteForm; 
    }
    
    /**
     * 
     * getLastModifiedForHTTPform 
     * @param file - a File object, used to get the last-time modified in milliseconds 
     * @return the last-time the file was modified in the form "EEE, d MMM yyyy HH:mm:ss Z"
     * 
     * 
     */
    public String getLastModifiedForHTTPForm(File file)
    {     
        //Properties 
        SimpleDateFormat timeFormat;
        Long lastModInLong = file.lastModified(); 
        String lastModified = "Error getting last modified"; 

        //Set up Time format to "EEE, d MMM yyyy HH:mm:ss Z"
        // and convert millisencond lastModified time to the format 
        try
        {
            timeFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
            lastModified = timeFormat.format(lastModInLong);
        }
        catch(Exception e)
        {  
            System.out.println("Location: getLastModifiedForHTTPForm\nError: " +e); 
        }
        //return time 
        return lastModified; 
    }
    
    /**
     * 
     * properGetRequest 
     * @param getRequest - an Array that contains the GET Http request broken down in tokens 
     * @return int that reprecents status code, if valid or invalid request
     * 
     */
    public int properGetRequest(String[] getRequest){
        //We check string contains
        // GET 
        // "/" && "."  === /path.something
        // HTTP/1.1 or HTTP/1.0 
        if(!getRequest[0].equals("GET") )
        {
            return 400;
        }
        if(!(getRequest[1].contains(".") && getRequest[1].contains("/")))
        {
            return 400; 
        }
        if(!(getRequest[2].equals("HTTP/1.1") || getRequest[2].equals("HTTP/1.0")))
        {
            return 400;
        }
        return 200; 
    }
    
    /**
     * getInvalidGETResquestBytesForm
     * @param statusCode - status code of HTTP request 
     * 
     * @return returns an badRequest message in a byte[]
     * 
     */
    public byte[] getInvalidGETResquestBytesForm(String httpVersion, int statusCode)
    {
        byte[] byteInvalidMassge = null;
        String badRequest;
        //The first massage varies depending the Status code 
        if(statusCode == 400)
        {
            badRequest =  httpVersion + " 400 Bad Request\r\n";
        }else
        {
            badRequest =  httpVersion + " 404 Not Found\r\n";
        }
        //finish badRequest message in string form 
        badRequest = badRequest + "Date: " + getCurrentDate() +"\r\n" 
                        + "Server: Un familiar with this request\r\n"
                        +"Connection: close\r\n" + "\r\n";

        //convert badRequest message to byte[]
        try
        {
            byteInvalidMassge = badRequest.getBytes("US-ASCII");
        }
        catch(Exception e)
        {
            System.out.println("location: getInvalidGETResquestBytesForm\nError: "+ e);
        }

        //return message
        return byteInvalidMassge; 
    }
       
    /**
     * getCurrentDate 
     * 
     * @return returns the current date in the form "EEE, d MMM yyyy HH:mm:ss Z"
     * 
     * 
     */
    public String getCurrentDate()
    {
        //Properties 
        SimpleDateFormat timeFormat;
        Date time; 
        String date = "Error accessing date"; 

        //Set up Time format to "EEE, d MMM yyyy HH:mm:ss Z"
        //and get time in proper format 
        try{
            time = new Date(); 
            timeFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
            date = timeFormat.format(time);
        }
        catch (Exception e) 
        {
            System.out.println("Error: " + e.getMessage());
        }

        //return date 
        return date; 
    }
    
    /**
     * Returns the HTTP Header of the InputStream provided by the server 
     * 
     * @param inputStream   InputStream object 
     * @return returns the first part of the Get request: GET /test.png HTTP/1.1 or GET /test.png HTTP/1.0
     */
    public String[] getGetrequest(InputStream inputStream)
    { 
        //Properties
        byte[] httpheaderBytesArray = new byte[2048];
        String httpHeader = "default"; 
        String[] requestArray; 
        int off = 0; 
                
        //read HTTP header:
        //will read byte by byte, and added to the a string
        //until we have a string that contains "\r\n"
        //this would mean we reach the end of the first line of the header
        try
        {
            while(true)
            {
                inputStream.read(httpheaderBytesArray, off, 1);
                off++;
                httpHeader = new String(httpheaderBytesArray, 0, off, "US-ASCII");
                if(httpHeader.contains("\r\n")){
                	break;
                }
            }
        }
        catch (Exception e) 
        {
            System.out.println("Error: "+ e.getMessage());
        }
        
        //split into its individual tokens
        requestArray = httpHeader.split(" ");
        requestArray[2] = requestArray[2].substring(0,requestArray[2].indexOf("\r")); 
        
        //return GET message
        return requestArray;
    }
}
