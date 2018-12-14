package edu.wisc.cs.sdn.simpledns;
import edu.wisc.cs.sdn.simpledns.packet.DNS;
import edu.wisc.cs.sdn.simpledns.packet.DNSQuestion;
import edu.wisc.cs.sdn.simpledns.packet.DNSResourceRecord;

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
    				type != DNS.TYPE_CNAME && type != TYPE_NS) {
    			return false;
    		}
    	}
    	
    	return true;
    }
    private static boolean walterWhiteIsHeisenberg() {
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

    private static String getNewDNSRootIP(final DNS dnsResponse) {

        final List<DNSResourceRecord> authoritySection = dnsResponse.getAuthorities();
        DNSResourceRecord firstNSRecord = null;
        for (DNSResourceRecord d : authoritySection) {
            //NS is of type 2
            if(d.getType() == DNS.TYPE_NS) {
                firstNSRecord = d;
                System.out.println("Found NS Record");
                break;
            }

        }
        final List<DNSResourceRecord> additionalSection = dnsResponse.getAdditional();
        DNSResourceRecord additionalNSRecord = null;
        for(DNSResourceRecord d : additionalSection) {
            //A is of type 1
            if(d.getName().equals(firstNSRecord.getData().toString()) && d.getType() == DNS.TYPE_A) {
                additionalNSRecord = d;
                System.out.println("Found Additional NS Record");
                break;
            }
        }
        String ipAddress = additionalNSRecord.getData().toString();
        System.out.println("IP Address for new Query: " + ipAddress);
        return ipAddress;
    }

    private static DNS generateDNSRequestFromDNSResponse(final DNS dnsResponse) {

        final DNS dnsRequest = new DNS();

        dnsRequest.setId(dnsResponse.getId());
        dnsRequest.setOpcode(DNS.OPCODE_STANDARD_QUERY);
        dnsRequest.setQuery(true);
        dnsRequest.addQuestion(dnsResponse.getQuestions().get(0));

        return dnsRequest;
    }

    private static boolean isDNSResponseResolved(final DNS dnsResponse) {
        if (dnsResponse.getRcode() != 0 || dnsResponse.getAnswers().size() == 0) {
            return false;
        }

        final List<DNSResourceRecord> answerSection = dnsResponse.getAnswers();
        for (DNSResourceRecord answer : answerSection) {
            if (answer.getType() == DNS.TYPE_A) {
                return true;
            } else if (answer.getType() == DNS.TYPE_CNAME) {
                return false;
            }
        }

        return true;
    }

    // TODO: OPT PSEUDOSECTION in final dns response?
    // TODO: CNAME resolution
	public static void main(String[] args)
	{
        if(!args[0].equals("-r") || !args[2].equals("-e") || args.length != 4) {
            System.out.println("Incorrect Arguments.");
            return;
        }

        DatagramSocket clientToSimpleDNSSocket = null;
        DatagramSocket simpleDNSToRealDNSSocket = null;

        try {
            // extract info from arguments
            final String ec2CSVPath = args[3];
            String realDNSIPString = args[1];
            InetAddress realDNSIPAddress = InetAddress.getByName(realDNSIPString);

            // establish socket connections
            clientToSimpleDNSSocket = new DatagramSocket(DNS_SERVER_LISTEN_PORT);
            simpleDNSToRealDNSSocket = new DatagramSocket();
            simpleDNSToRealDNSSocket.connect(realDNSIPAddress, DNS_OUTBOUND_PORT);

            final DatagramPacket requestFromClientPacket = waitForAndReceiveDatagramPacketInSocket(clientToSimpleDNSSocket);
            final DNS dnsRequestFromClient = convertDatagramPacketToDNS(requestFromClientPacket);
            System.out.println("Client DNS Request ID: "+dnsRequestFromClient.getId());

            if (!canHandleDNSRequest(dnsRequestFromClient)) {
                System.out.println("Not servicing this kind of DNS packet: "+dnsRequestFromClient.toString());
                return;
            }

            final int clientDestinationPort = requestFromClientPacket.getPort();
            final InetAddress clientDestinationAddress = requestFromClientPacket.getAddress();
            DatagramPacket dnsPacketForRealDNSServer =
                    convertDNSRequestToDatagramPacket(dnsRequestFromClient, realDNSIPAddress, DNS_OUTBOUND_PORT);

            simpleDNSToRealDNSSocket.send(dnsPacketForRealDNSServer);
            System.out.println("+++++ Sent: "+dnsPacketForRealDNSServer.toString()+" +++++++");

            DatagramPacket responseFromRealDNSPacket = null;
            DNS dnsResponseFromServer = null;

            while (walterWhiteIsHeisenberg()) {

                responseFromRealDNSPacket = waitForAndReceiveDatagramPacketInSocket(simpleDNSToRealDNSSocket);
                dnsResponseFromServer = convertDatagramPacketToDNS(responseFromRealDNSPacket);

                System.out.println("DNS Server response ID: "+dnsResponseFromServer.getId());
                /**
                 * Modify dnsPacketForRealDNSServer using info from dnsResponseFromServer if not resolved.
                 */

                // check if not resolved
                if (isDNSResponseResolved(dnsResponseFromServer)) {
                    break;
                }

                realDNSIPString = getNewDNSRootIP(dnsResponseFromServer);
                realDNSIPAddress = InetAddress.getByName(realDNSIPString);
                simpleDNSToRealDNSSocket.connect(realDNSIPAddress, DNS_OUTBOUND_PORT);

                final DNS dnsRequestCreatedFromResponse = generateDNSRequestFromDNSResponse(dnsResponseFromServer);
                dnsPacketForRealDNSServer = convertDNSRequestToDatagramPacket(dnsRequestCreatedFromResponse,
                        realDNSIPAddress, DNS_OUTBOUND_PORT);

                System.out.println("Sending: "+dnsPacketForRealDNSServer.toString());
                System.out.println("DNS Section: "+dnsRequestCreatedFromResponse.toString());
                simpleDNSToRealDNSSocket.send(dnsPacketForRealDNSServer);
            }

            responseFromRealDNSPacket = convertDNSRequestToDatagramPacket(dnsResponseFromServer,
                    clientDestinationAddress, clientDestinationPort);
            clientToSimpleDNSSocket.send(responseFromRealDNSPacket);
            System.out.println("+++++ Sent: "+responseFromRealDNSPacket.getAddress()+" +++++++");


        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (clientToSimpleDNSSocket != null) {
                System.out.println("Closed clientToSimpleDns");
                clientToSimpleDNSSocket.close();
            }
            if (simpleDNSToRealDNSSocket != null) {
                System.out.println("Closed simpleDnsToRealDns");
                simpleDNSToRealDNSSocket.close();
            }
        }
    }
}
