package edu.wisc.cs.sdn.simpledns;

import edu.wisc.cs.sdn.simpledns.packet.DNS;
import edu.wisc.cs.sdn.simpledns.packet.DNSQuestion;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

class DNSQueryResolver {

    private static int DNS_OUTBOUND_PORT = 53;

    private DNSConnection simpleDNSToRealDNSConnection;

    DNSQueryResolver() throws SocketException {
        simpleDNSToRealDNSConnection = new DNSConnection();
    }

    DNS resolveQueryNonRecursive(final DNS dnsQuery, final InetAddress rootServerAddress) throws IOException {
        simpleDNSToRealDNSConnection.sendDNSPacket(dnsQuery, rootServerAddress, DNS_OUTBOUND_PORT);
        final DatagramPacket responsePacket = simpleDNSToRealDNSConnection.waitForAndReceiveDatagramPacket();
        return DNSDatagramHandler.extractDNSHeaderFromDatagramPacket(responsePacket);
    }

    private InetAddress recursivelyResolveUncachedNameserver(final DNS dnsResponse, final InetAddress rootServerAddress)
            throws IOException {

        final String nextNameServer = DNSDatagramHandler.findNextNameserverFromDNSResponse(dnsResponse);
        final DNS nextNameserverRequest =
                DNSDatagramHandler.generateDNSRequest(new DNSQuestion(nextNameServer, DNS.TYPE_A), dnsResponse.getId());
        final DNS nextNameserverResponse = resolveQueryRecursive(nextNameserverRequest, rootServerAddress);
        final String nextServerIP = nextNameserverResponse.getAnswers().get(0).getData().toString();
        return CIDRConverter.convertDottedDecimalStringToInetAddress(nextServerIP);
    }

    private InetAddress resolveNextRootserverIP(final DNS dnsResponse, final InetAddress rootServerAddress)
            throws IOException {

        InetAddress nextRootServerIP = DNSDatagramHandler.getNextCachedNameserver(dnsResponse);
        if (nextRootServerIP == null) {
            nextRootServerIP = recursivelyResolveUncachedNameserver(dnsResponse, rootServerAddress);
        }
        return nextRootServerIP;
    }

    DNS resolveQueryRecursive(final DNS dnsQuery, final InetAddress rootServerAddress) throws IOException {

        final short queryType = DNSDatagramHandler.extractRequestTypeFromDNSHeader(dnsQuery);
        DNS currentDNSQuery = DNSDatagramHandler.generateDNSRequest(dnsQuery.getQuestions().get(0), dnsQuery.getId());
        InetAddress currentRootServerAddress = rootServerAddress;

        while (true) {

            System.out.println("===================================================================");
            System.out.println("Recursive Query Sub-Request");
            System.out.println(currentDNSQuery.toString());
            System.out.println("===================================================================");

            final DNS dnsResponse = resolveQueryNonRecursive(currentDNSQuery, currentRootServerAddress);

            System.out.println("===================================================================");
            System.out.println("Response for Recursive Query Sub-Request");
            System.out.println(dnsResponse.toString());
            System.out.println("===================================================================");

            if (!dnsResponse.getAnswers().isEmpty()) {
                dnsResponse.setId(dnsQuery.getId());
                return dnsResponse;
            }

            currentRootServerAddress = resolveNextRootserverIP(dnsResponse, rootServerAddress);
            currentDNSQuery = DNSDatagramHandler.generateDNSRequest(dnsResponse.getQuestions().get(0),
                    dnsResponse.getId());
        }
    }

    void teardownResolver() {
        simpleDNSToRealDNSConnection.teardownConnection();
    }
}
