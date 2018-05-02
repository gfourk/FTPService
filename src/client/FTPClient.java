
package client;

import java.io.BufferedReader;
import java.io.Console;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import utils.BinaryTransfer;

import java.net.NoRouteToHostException;

public class FTPClient  {

	/* gets an error code and returns it's string "translation",
	 * what the error code means */
	private static String translateErrorCode(int errorCode)	{

		switch (errorCode)	{
		case 100: return "user unknown";
		case 110: return "file list error";
		case 200: return "file not found";
		case 201: return "cannot open file";
		case 202: return "cannot delete file";
		case 203: return "cannot create directory";
		case 204: return "no such directory";
		case 007: return "unknown error";
		default: assert false: "No other error code exists";
		}

		return null;
	}

	public static void main(String[] args) throws Exception	{

		Socket clientConnection = null;
		String hostname = null;
		Console console = System.console();

		while (true)	{	// while no connection is established
			hostname = console.readLine("Server's IP: ");

			try	{
				clientConnection = new Socket(hostname, 21000);
				break; // connection established
			}
			catch (ConnectException e)	{
				System.err.println("There is no proccess listening to port 21000");
			}
			catch (UnknownHostException e)	{
				System.err.println("An ip of a host couldn't be determined");
			}
			catch (NoRouteToHostException e)	{
				System.err.println("The server cannot be reached because of an" +
						"intervening router or an intermediate route is down");
			}
		}

		String sentence = null;
		String response = null;
		DataOutputStream outputToServer = new DataOutputStream( clientConnection.getOutputStream() );
		InputStream inputStreamFromServer = clientConnection.getInputStream();
		BufferedReader inputFromServer = new BufferedReader( new InputStreamReader( inputStreamFromServer ) );

		while (true)	{
			long startTime, endTime;

			System.out.print("insert command >>> ");
			sentence = console.readLine();

			if (sentence.equals("QUIT"))
				break;


			startTime = endTime = 0;
			response = null;
			// send data server and count spent time to get response
			try	{
				startTime = System.currentTimeMillis();
				outputToServer.writeBytes(sentence + "\n");

				response = inputFromServer.readLine();
				if (response == null)
					throw new SocketException("server closed connection");

				endTime = System.currentTimeMillis();
			}
			catch (SocketException e)	{
				System.err.println("Connection problem: " + e.getMessage());
				System.exit(1);
			}

			if (response.startsWith("ERROR"))	{
				int errorCode = Integer.parseInt(response.substring(6));
				String errorExplained = translateErrorCode(errorCode);
				System.out.println("ERROR response <<< \"" + errorExplained +"\"");
			}
			else if (response.startsWith("LIST OK "))	{
				// remove new line chatacters and print the filenames
				String[] filenames = response.substring(8).split("\\\\n");

				System.out.print("Files(response time = " + (endTime - startTime) + " ms): ");
				for (int i = 0; i < filenames.length -1; ++i)	{
					System.out.print(filenames[i] + ", ");
				}
				System.out.println(filenames[filenames.length - 1]);
			}
			else if (response.startsWith("GET OK "))	{

				File file = null;

				while (true)	{	// until to get a valid filename, one that doesn't exist
					String filename = console.readLine("Please give a filename(where the" +
							" transferred file will be saved): ");

					file = new File(filename);
					if (!file.exists()) break;
				}

				// write data to given filename
				BinaryTransfer.toFile(inputStreamFromServer, file);
				System.out.println("File transferred in: " + (endTime - startTime) + " ms");
			}

			else	{
				System.out.println("server response [time: " + (endTime - startTime)
						+ " ms]" + " >>> \"" + response +"\"");
			}

			if (response.equals("BYE OK"))
				break;
		}
		clientConnection.close();
	}

}
