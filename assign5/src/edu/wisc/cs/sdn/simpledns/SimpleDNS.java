package edu.wisc.cs.sdn.simpledns;
import edu.wisc.cs.sdn.simpledns.packet.DNS;
import edu.wisc.cs.sdn.simpledns.packet.DNSQuestion;

import java.util.List;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
public class SimpleDNS 
{

    private static final int DNS_SERVER_LISTEN_PORT = 8053;
    private static final int DNS_OUTBOUND_PORT = 53;
    
    private static final int PACKET_BUFFER_SIZE = 4092;

    public static boolean isValidQueryType(DNS dns) {

        for (DNSQuestion question : dns.getQuestions()) {
            short type = question.getType();
            if (type != DNS.TYPE_A && type != DNS.TYPE_AAAA
                    && type != DNS.TYPE_CNAME && type != DNS.TYPE_NS) {
                return false;
            }
        }

        return true;
    }

    private static DNS convertDatagramPacketToDNS(final DatagramPacket datagramPacket) {
        return DNS.deserialize(datagramPacket.getData(), datagramPacket.getLength());
    }

    private static DatagramPacket waitForAndReceiveDatagramPacketInSocket(final DatagramSocket socket) 
    		throws IOException {
        
    	final byte[] buffer = new byte[PACKET_BUFFER_SIZE];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }
    
    private static boolean areQuestionsValidTypes(final List<DNSQuestion> questionList) {
    	short type;
    	for (DNSQuestion question : questionList) {
    		type = question.getType();
    		if (type != DNS.TYPE_A && type != DNS.TYPE_AAAA && 
    				type != DNS.TYPE_CNAME && type != DNS.TYPE_NS) {
    			return false;
    		}
    	}
    	
    	return true;
    }
    
    private static boolean canHandleDNSRequest(final DNS dnsRequest) {
    	return dnsRequest.getOpcode() == 0 && dnsRequest.isQuery() && 
    			areQuestionsValidTypes(dnsRequest.getQuestions());
    }
    
    private static DatagramPacket convertDNSRequestToDatagramPacket(final DNS dnsRequest, 
    		final InetAddress destinationIPAddress, final int destinationPort) {
    	
    	final byte[] buffer = dnsRequest.serialize();
    	final DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
    			destinationIPAddress, destinationPort);
    	return packet;
    }
    /*
    public static void handleRicinQuery(DNS dns) {

        if(dns.getId() == DNS.TYPE_A || dns.getId() == DNS.TYPE_AAAA) {
            for(int i = 0; i < dns.getQuestions().size(); i++) {
                if(dns.getQuestions().get(i).)
            }
        }
    }
    */
	public static void main(String[] args)
	{
        if(!args[0].equals("-r") || !args[2].equals("-e") || args.length != 4) {
            System.out.println("Incorrect Arguments.");
            return;
        }

        DatagramSocket clientToSimpleDNS = null;
        DatagramSocket simpleDnsToRealDNS = null;

        try {
            // extract info from arguments
            final String ec2CSVPath = args[3];
            final String realDNSIPString = args[1];
            final InetAddress realDNSIPAddress = InetAddress.getByName(realDNSIPString);

            // establish socket connections
            clientToSimpleDNS = new DatagramSocket(DNS_SERVER_LISTEN_PORT);
            simpleDnsToRealDNS = new DatagramSocket();
            simpleDnsToRealDNS.connect(ipAddr, DNS_OUTBOUND_PORT);

            final DatagramPacket packet = waitForAndReceiveDatagramPacketInSocket(clientToSimpleDNS);
            final DNS dnsRequestFromClient = convertDatagramPacketToDNS(packet);
            
            if (!canHandleDNSRequest(dnsRequestFromClient)) {
            	 System.out.println("Not servicing this kind of DNS packet: "+dnsRequestFromClient.toString());
                 return;
            }
            
            // determine where to send client response to
            final int clientInboundPort = packet.getPort();
            final InetAddress clientInboundAddress = packet.getAddress();
            
            // 
          
            final DatagramPacket dnsPacketForRealDNSServer = 
            		convertDNSRequestToDatagramPacket(dnsRequestFromClient, );
            

            // send DNS request from our DNS server to REAL DNS server provided in args
            buffer = dnsRequestFromClient.serialize();
            DatagramPacket dnsPacketSentToServer = new DatagramPacket(buffer,
                    buffer.length, ipAddr, 53);
            simpleDnsToRealDNS.send(dnsPacketSentToServer);
            System.out.println("+++++ Sent: "+dnsPacketSentToServer.toString()+" +++++++");

            // wait for DNS response from REAL DNS server
            buffer = new byte[10240];
            DatagramPacket dnsResolution = new DatagramPacket(buffer, buffer.length);
            simpleDnsToRealDNS.receive(dnsResolution);

            // TODO: possibly remove
            buffer = dnsResolution.getData();
            DNS response = DNS.deserialize(buffer, buffer.length);
            buffer = response.serialize();

            // send DNS response from our DNS back to client
            dnsResolution = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
            clientToSimpleDNS.send(dnsResolution);
            System.out.println("+++++ Sent: "+dnsResolution.getAddress()+" +++++++");

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (clientToSimpleDNS != null) {
                System.out.println("Closed clientToSimpleDns");
                clientToSimpleDNS.close();
            }
            if (simpleDnsToRealDNS != null) {
                System.out.println("Closed simpleDnsToRealDns");
                simpleDnsToRealDNS.close();
            }
        }
    }
}
