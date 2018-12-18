package edu.wisc.cs.sdn.simpledns;

import edu.wisc.cs.sdn.simpledns.packet.DNS;
import edu.wisc.cs.sdn.simpledns.packet.DNSQuestion;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;

class DNSQueryResolver {

    private static int DNS_OUTBOUND_PORT = 53;

    private DNSConnection simpleDNSToRealDNSConnection;

    DNSQueryResolver() throws SocketException {
        simpleDNSToRealDNSConnection = new DNSConnection();
    }

    private DNS resolveQueryNonRecursive(final DNS dnsQuery, final InetAddress rootServerAddress) throws IOException {
        simpleDNSToRealDNSConnection.sendDNSPacket(dnsQuery, rootServerAddress, DNS_OUTBOUND_PORT);
        final DatagramPacket responsePacket = simpleDNSToRealDNSConnection.waitForAndReceiveDatagramPacket();
        return DNSDatagramHandler.extractDNSHeaderFromDatagramPacket(responsePacket);
    }

    private DNS generateNextNameserverRequest(final DNS dnsResponse, final String nextNameServer) {
        final DNS nextNameserverRequest = DNS.deserialize(dnsResponse.serialize(), dnsResponse.getLength());
        final DNSQuestion question = new DNSQuestion(nextNameServer, DNS.TYPE_A);
        nextNameserverRequest.setQuestions(Collections.singletonList(question));
        nextNameserverRequest.setAuthorities(new ArrayList<>());
        nextNameserverRequest.setAdditional(new ArrayList<>());
        nextNameserverRequest.setQuery(true);
        return nextNameserverRequest;
    }

    private InetAddress recursivelyResolveUncachedNameserver(final DNS dnsResponse, final InetAddress rootServerAddress)
            throws IOException {

        final String nextNameServer = DNSDatagramHandler.findNextNameserverFromDNSResponse(dnsResponse);
        System.out.println("Next NameServer: "+nextNameServer);
        final DNS nextNameserverRequest = generateNextNameserverRequest(dnsResponse, nextNameServer);
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

    private DNS generateNextRecursiveDNSQuery(final DNS currentDNSQuery, final DNS dnsResponse) {
        final DNS dnsCopy = DNS.deserialize(currentDNSQuery.serialize(), currentDNSQuery.getLength());
        dnsCopy.setQuestions(dnsResponse.getQuestions());
        return dnsCopy;
    }

    DNS resolveQueryRecursive(final DNS dnsQuery, final InetAddress rootServerAddress) throws IOException {

        DNS currentDNSQuery = dnsQuery;
        InetAddress currentRootServerAddress = rootServerAddress;

        while (true) {

  //          DNSDatagramHandler.printDNS(currentDNSQuery, currentRootServerAddress, "Recursive Query Sub-Request");
            final DNS dnsResponse = resolveQueryNonRecursive(currentDNSQuery, currentRootServerAddress);
//            DNSDatagramHandler.printDNS(dnsResponse, currentRootServerAddress, "Response for Recursive Query Sub-Request");

            if (!dnsResponse.getAnswers().isEmpty()) {
                dnsResponse.setId(dnsQuery.getId());
                return dnsResponse;
            }

            currentRootServerAddress = resolveNextRootserverIP(dnsResponse, rootServerAddress);
            currentDNSQuery = generateNextRecursiveDNSQuery(currentDNSQuery, dnsResponse);
        }
    }

    void teardownResolver() {
        simpleDNSToRealDNSConnection.teardownConnection();
    }
}
