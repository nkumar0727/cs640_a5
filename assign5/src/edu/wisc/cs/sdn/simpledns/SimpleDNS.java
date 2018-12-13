package edu.wisc.cs.sdn.simpledns;
import edu.wisc.cs.sdn.simpledns.packet.DNS;
import edu.wisc.cs.sdn.simpledns.packet.DNSQuestion;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
public class SimpleDNS 
{

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

        DatagramSocket clientToSimpleDns = null;
        DatagramSocket simpleDnsToRealDns = null;

        try {
            // extract info from args
            String ipAddress = args[1];
            InetAddress ipAddr = InetAddress.getByName(ipAddress);
            String path = args[3];

            // establish socket connections
            clientToSimpleDns = new DatagramSocket(8053);
            simpleDnsToRealDns = new DatagramSocket();
            simpleDnsToRealDns.connect(ipAddr, 53);

            byte[] buffer = new byte[10240];

            //while ("walt".equals("walt")) {
            // wait to service a DNS request packet
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            clientToSimpleDns.receive(packet);

            // determine where to send client response to
            int clientPort = packet.getPort();
            InetAddress clientAddress = packet.getAddress();

            // load DNS packet for analysis
            buffer = packet.getData();
            DNS dnsRequestFromClient = DNS.deserialize(buffer, buffer.length);

            // filter out bad DNS packets
            if (dnsRequestFromClient.getOpcode() != 0 || !dnsRequestFromClient.isQuery() || !isValidQueryType(dnsRequestFromClient)) {
                System.out.println("Not servicing this kind of DNS packet: "+dnsRequestFromClient.toString());
                return;
            }

            // send DNS request from our DNS server to REAL DNS server provided in args
            buffer = dnsRequestFromClient.serialize();
            DatagramPacket dnsPacketSentToServer = new DatagramPacket(buffer,
                    buffer.length, ipAddr, 53);
            simpleDnsToRealDns.send(dnsPacketSentToServer);
            System.out.println("+++++ Sent: "+dnsPacketSentToServer.toString()+" +++++++");

            // wait for DNS response from REAL DNS server
            buffer = new byte[10240];
            DatagramPacket dnsResolution = new DatagramPacket(buffer, buffer.length);
            simpleDnsToRealDns.receive(dnsResolution);

            // TODO: possibly remove
            buffer = dnsResolution.getData();
            DNS response = DNS.deserialize(buffer, buffer.length);
            buffer = response.serialize();

            // send DNS response from our DNS back to client
            dnsResolution = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
            clientToSimpleDns.send(dnsResolution);
            System.out.println("+++++ Sent: "+dnsResolution.getAddress()+" +++++++");

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (clientToSimpleDns != null) {
                System.out.println("Closed clientToSimpleDns");
                clientToSimpleDns.close();
            }
            if (simpleDnsToRealDns != null) {
                System.out.println("Closed simpleDnsToRealDns");
                simpleDnsToRealDns.close();
            }
        }
    }
}
