/*
 * UTFClient.java
 * ---------------------------------------------------------------------------------
 * Emmanuel Ngenoh, Felix Gondwe
 * Dr. Lee
 * Project 2: UFT project 1 optimization
 * Due: 03/31/14
 * ----------------------------------------------------------------------------------

 * ----------------------------------------------------------------------------------
 * UDP client that reads the bytes of a file and transfers chunks of bytes to a server
 * Each chunk of bytes is transferred after getting an aknowlendgement from the server
 * Modifications:
 *	1. Data packet loss simulation
 *	2. Acknowledgement packet corruption simulation
 *	3. Timeout
 *	4. Performance measurement
 *  5.  introduce two extra system arguments that will specify the corruption and loss rate (between 0 and 1 inclusive)
 * ----------------------------------------------------------------------------------
 */

import java.net.*;
import java.util.*;
import java.io.*;


/*
 * bug report: 
 * dealing with duplicates
 * time out functionality
 */
public class UTFClient {

	public static void main(String[] args) throws Exception {
		/*------------------------------------------------------------------------------*/
		/*
		 * moved the command arguments to top connect to local host get file get
		 * number of bytes to send each time Extra arguments to simulate rate of
		 * corruption and loss: get corruption rate,get loss rate
		 */
		InetAddress toAddress = InetAddress.getByName(args[0]);
		File file = new File(args[1]);
		int lenOfPacket = Integer.parseInt(args[2]);
		double corruptionRate = Float.parseFloat(args[3]);
		double lossRate = Float.parseFloat(args[4]);
		/*-------------------------------------------------------------------------------*/

		/*
		 * create a random object to help generate the floating numbers for
		 * simulation Get the size of the whole file copy bytes from the file
		 * into an ARRAY called array Create an array to store all the file's
		 * bytes
		 */
		Random random = new Random();
		int fileSize = (int) file.length();
		// debug print statement
		System.out.println("The total file size is " + fileSize);
		FileInputStream input = new FileInputStream(file);
		byte[] array = new byte[fileSize];
		int count = 0;
		int bit;
		while ((bit = input.read()) != -1) {
			array[count] = (byte) bit;
			count++;
		}

		/*
		 * create a UDP socket with a random port available on the system create
		 * port number: UDP server is listening on port 24567 send the first
		 * batch of bytes sequence number for first batch currentByte: keeps
		 * track of the bytes to be transferred next. lastPacket: Checks for the
		 * last packet: Is 1 if this is the last packet
		 */

		DatagramSocket clientSocket = new DatagramSocket();
		int toPort = 24567;
		int sequenceNumber = 0;
		int currentByte = 0;
		int lastPacket = 0;
		int acks=0;
		int packetCounter=0;
		int retransmittedPackets=0;
		int duplicateAcks=0;
		int timeOuts=0;

		// Represents the current batch of bytes being moved
		byte[] buffer = new byte[lenOfPacket + 3];
		buffer[0] = (byte) ((sequenceNumber >> 8) & 0xFF);
		buffer[1] = (byte) (sequenceNumber & 0xFF);
		buffer[2] = (byte) lastPacket;

		int j = 3;
		for (int i = 0; i < lenOfPacket; i++) {
			buffer[j] = array[i];
			j++;
		}

		/*
		 * measure the time it takes to send file to server send the first
		 * packet After getting resposnse from the first packet sent, then send
		 * the rest of the packets report elapsed time
		 * 
		 * 
		 */

		DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length,
				toAddress, toPort);

		// Send the first packet of the first batch of bytes

		long start = System.currentTimeMillis();
		clientSocket.send(sendPacket);
		System.out.println(">>>>>>>>>>>>First Packet sent<<<<<<<<<<<<");
		currentByte += lenOfPacket; // Increment current byte by 500
		DatagramPacket sendPacketPrevious = null; // Stores the previous packet
		// in case the received
		// packet is corrupt

		//in case firstPacket times out and we will have to resend it
		sendPacketPrevious = sendPacket;

		// loop
		while (lastPacket != 2) {


			// Receiving response before sending next batch
			// Create placeholder byte array for the response packet
			byte[] response = new byte[1024];

			// create place holder datagram packet to fill upon receiving packet
			DatagramPacket responsePacket = new DatagramPacket(response,
					response.length);

			// receive packet through UDP socket
			clientSocket.setSoTimeout(200); // /Setting a time
			try {
				clientSocket.receive(responsePacket); // If aknowledgement is
				// received when time										// times out

			} catch (SocketTimeoutException e) { // If packet times out,
				// retransmit previous
				//if(sequenceNumber!=0 && sequenceNumber!=1){									// packet
				//sequenceNumber--;
				//}
				if(lastPacket==1){
					lastPacket++;
				}
				timeOuts++;
				System.out.println("Timeout for Packet #" + sequenceNumber);


				clientSocket.send(sendPacketPrevious);
				packetCounter++;
				System.out.println("Retransmitted Packet #" + sequenceNumber);
				retransmittedPackets++;
				continue;
			}
			sequenceNumber++; // Incrementing the sequence number

			byte[] responseByteString = Arrays.copyOf(responsePacket.getData(),
					responsePacket.getLength());

			// Extract the data in the received packet
			int responseSequence = (int) responseByteString[0] << 8
					| (int) responseByteString[1] & 0xff;
			acks++;

			if(responseSequence!=sequenceNumber-1){
				sequenceNumber--;
				duplicateAcks++;
				System.out.println("--Retransmitted Packet #" + sequenceNumber);
				clientSocket.send(sendPacketPrevious);
				packetCounter++;
				retransmittedPackets++;
				continue;
			}

			/*
			 * Simulating ACK packet corruption
			 */

			double corrupt = random.nextDouble();
			if (corrupt < corruptionRate ) {
				if(lastPacket!=1){
					sequenceNumber=responseSequence;
					System.out.println("Received a Corrupt ACK #" + sequenceNumber);
					clientSocket.send(sendPacketPrevious);
					packetCounter++;
					retransmittedPackets++;
					System.out.println("Retransmitted Packet #" + sequenceNumber);
				}
				continue;
			}


			System.out.println("Received ACK#" + responseSequence);

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Send next batch of data
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			// DO THIS If it is the last batch of bytes
			if (currentByte + lenOfPacket > fileSize) {
				lastPacket++;

				if (lastPacket == 2) {
					break;
				}

				// currentByte=fileSize-currentByte;
				buffer = new byte[fileSize - currentByte + 3];

				buffer[0] = (byte) ((sequenceNumber >> 8) & 0xFF);
				buffer[1] = (byte) (sequenceNumber & 0xFF);
				buffer[2] = (byte) 1;

				j = 3;
				for (int i = currentByte; i < fileSize; i++) {
					buffer[j] = array[i];
					j++;
				}

			}

			// ~~~~~~~~~~~~~~~~OTHERWISE~~~~~~~~~~~~~~~~~~

			else {

				buffer[0] = (byte) ((sequenceNumber >> 8) & 0xFF);
				buffer[1] = (byte) (sequenceNumber & 0xFF);
				buffer[2] = (byte) lastPacket;

				j = 3;
				for (int i = currentByte; i < currentByte + lenOfPacket; i++) {
					buffer[j] = array[i];
					j++;
				}

			}

			sendPacketPrevious = sendPacket;

			// Packet to send
			sendPacket = new DatagramPacket(buffer, buffer.length, toAddress,
					toPort);

			// Increment to the next batch e.g next 500
			currentByte += lenOfPacket;

			/*
			 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Simulating
			 * Packet loss ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			 */
			double loss = random.nextDouble();
			if (loss < lossRate) {

				sendPacketPrevious=sendPacket;
				System.out.println("Lost Packet#" + sequenceNumber);
				continue;
			}

			// Send bytes in batches
			clientSocket.send(sendPacket);
			packetCounter++;

			System.out.println("Sent Packet #" + sequenceNumber);


		}

		clientSocket.close();
		System.out.println("--------------------------------------------");
		System.out.println("~~~~~~~~~~~~~~~~Analysis~~~~~~~~~~~~~~~~~~~~~");
		System.out.println("--------------------------------------------");
		System.out.println("Number of ACKS received is: "+acks);
		System.out.println("The total number of transmitted packets is: "+packetCounter);
		System.out.println("The total number of retransmitted packets is: "+retransmittedPackets);
		System.out.println("Total number of Duplicate ACKs Received is "+duplicateAcks);
		System.out.println("Total Number of time outs: "+timeOuts);
		//time elapsed
		long elapsed = System.currentTimeMillis() - start;
		System.out.println("----------------------------------");
		System.out.println("Elapsed time: " + elapsed + " ms");
		System.out.println("----------------------------------");

	}

}
