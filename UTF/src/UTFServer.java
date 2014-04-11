/*
 * UTFServer.java
 * ---------------------------------------------------------------------------------
 * Emmanuel Ngenoh, Felix Gondwe
 * Dr. Lee
 * Project 2: UFT project 1 optimization
 * Due: 03/31/14
 * ----------------------------------------------------------------------------------

 * Server program that receives chunks bytes from a client and and keeps track of the the sequence number of packets
 * Sends ackowledgements to the client after writing the bytes received to a file.
 * 
 */

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class UTFServer {

	public static void main(String[] args) throws Exception {

		// Extra arguments to simulate rate of corruption and loss

		double corruptionRate = Float.parseFloat(args[1]);
		double lossRate = Float.parseFloat(args[2]);
		Random random = new Random();

		int lastPacket = 0;
		int acks=0;
		int totalPackets=0;
		int nonDuplicatePackets=0;
		int duplicatePackets;
		int duplicateAcks=0;

		// Open the bianry file to write based on the system argument.
		File receivedFile = new File(args[0]);

		FileOutputStream output = new FileOutputStream(receivedFile);

		// creates a UDP socket at port 24567

		DatagramSocket serverSocket = new DatagramSocket(24567);

		// creates a byte array to hold the incoming data
		byte[] data = new byte[1024];

		int sequenceNumber = 0;
		

		while (lastPacket != 1) {

			// Creates datagram Packet to hold the receiving packet
			DatagramPacket receivedPacket = new DatagramPacket(data,
					data.length);

			System.out.println();
			serverSocket.receive(receivedPacket);

			byte[] relevantData = Arrays.copyOf(receivedPacket.getData(),
					receivedPacket.getLength());

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Getting sequence number
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			int previousSequenceNumber = sequenceNumber;

			sequenceNumber = (int) relevantData[0] << 8 | (int) relevantData[1]
					& 0xff;
			System.out.println("--------The sequence number is---------"
					+ sequenceNumber);

			// Checks if this is the last byte.
			lastPacket = relevantData[2] & 0xFF;
			
			
			/*----------------------------------------------
			 * Simulating Corruption
			 * ---------------------------------------------
			 */
			

			double corrupt = random.nextDouble();
			if (corrupt <= corruptionRate) {
				//Incase the last packet is considered to be corrupt
				//Keep the server on for retransmission
				if(lastPacket==1){
					lastPacket=0;
				}
				totalPackets++;
				duplicateAcks++;
				System.out.println("Received a corrupt packet");
				sequenceNumber = previousSequenceNumber;
				System.out.println("Sent ACK #" + sequenceNumber);
				acks++;
				
			} else {
				totalPackets++;
				System.out.println("Received Packet #" + sequenceNumber);
				System.out.println("Sent ACK #" + sequenceNumber);
				acks++;
				
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~---------------------------------
				// Write received bytes into file if the packet received is not a duplicate
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~---------------------------------
				if(previousSequenceNumber!=sequenceNumber || sequenceNumber==0){
					nonDuplicatePackets++;
					output.write(relevantData, 3, relevantData.length - 3);
				}

			}
			
			/*---------------------------------------------
			 * Simulating Loss
			 * --------------------------------------------
			 */
			
			double loss = random.nextDouble();
			if (loss <= lossRate) {
				if(lastPacket==1){
					lastPacket=0;
				}
				acks--;
				System.out.println("Lost ACK #" + sequenceNumber);
				
				continue;
			}
			

			
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Prepare to send back acknowledgment
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			// Extract the from-port from received packet
			int returnPort = receivedPacket.getPort();

			InetAddress returnAddress = receivedPacket.getAddress();

			byte[] ack = new byte[2];
			ack[0] = (byte) ((sequenceNumber >> 8) & 0xFF);
			ack[1] = (byte) (sequenceNumber & 0xFF);

			// Creating a datagram packet with the address and destination port
			DatagramPacket ackPacket = new DatagramPacket(ack, ack.length,
					returnAddress, returnPort);

			// Sending back aknowledgement.
			serverSocket.send(ackPacket);

		}
		System.out.println("--------------------------------------------");
		System.out.println("~~~~~~~~~~~~~~~~Analysis~~~~~~~~~~~~~~~~~~~~~");
		System.out.println("--------------------------------------------");
		System.out.println("Total Packets received: "+totalPackets);
		System.out.println("The total number of duplicate ACKs is: "+duplicateAcks);
		System.out.println("Sent number of ACKS is: "+acks);
		System.out.println("The total number of duplicate packets is: "+(totalPackets-nonDuplicatePackets));
		// Close the fileoutputstream
		output.close();

		// close serversocket thus closing the server.

		serverSocket.close();

	}

}
