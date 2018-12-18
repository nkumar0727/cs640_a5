package edu.wisc.cs.sdn.simpledns;

import edu.wisc.cs.sdn.simpledns.packet.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class DNSRequestResolver {

    private static int DNS_LISTEN_PORT = 8053;

    private DNSQueryResolver dnsQueryResolver;
    private DNSConnection clientToSimpleDNSConnection;
    private CSVLoader csvLoader;

    DNSRequestResolver(final CSVLoader csvLoader) throws SocketException {
        clientToSimpleDNSConnection = new DNSConnection(DNS_LISTEN_PORT);
        dnsQueryResolver = new DNSQueryResolver();
        this.csvLoader = csvLoader;
    }

    private void addTXTAnswerIfEC2IP(final List<DNSResourceRecord> additionalEC2Answers, final DNSResourceRecord record)
            throws UnknownHostException {

        final String resolvedIP = record.getData().toString();
        final String ec2Region = csvLoader.getRegionOfIPAddress(resolvedIP);
        if (ec2Region != null) {
            final String txtRecordDataString = String.format("%s-%s", ec2Region, resolvedIP);
            final DNSRdata txtRecordData = new DNSRdataString(txtRecordDataString);
            final DNSResourceRecord txtRecord = new DNSResourceRecord(record.getName(), DNS.TYPE_TXT, txtRecordData);
            additionalEC2Answers.add(txtRecord);
        }
    }

    private DNS modifyAndReturnResponseWithEC2Records(final DNS dnsResponse) throws UnknownHostException {

        if (dnsResponse.getQuestions().get(0).getType() != DNS.TYPE_A) {
            return dnsResponse;
        }

        final List<DNSResourceRecord> additionalEC2Answers = new ArrayList<>();
        for (DNSResourceRecord answerRecord : dnsResponse.getAnswers()) {
            if (answerRecord.getType() != DNS.TYPE_A && answerRecord.getType() != DNS.TYPE_AAAA) {
                continue;
            }
            addTXTAnswerIfEC2IP(additionalEC2Answers, answerRecord);
        }

        dnsResponse.getAnswers().addAll(additionalEC2Answers);
        return dnsResponse;
    }

    private DNS generateDNSRequestForCNAME(final DNS copyDNSPacket, final DNS dnsResponse, final short requestType) {
        final DNS dns = DNS.deserialize(copyDNSPacket.serialize(), copyDNSPacket.getLength());
        final String cnameDomain = DNSDatagramHandler.extractCNAMEAnswerFromDNSResponse(dnsResponse);
        final DNSQuestion cnameQuestion = new DNSQuestion(cnameDomain, requestType);
        dns.setQuestions(Collections.singletonList(cnameQuestion));
        return dns;
    }

    private DNS getResponseForRecursiveRequest(final DNS dnsRequest, final InetAddress dnsServerIP)
            throws IOException {

        final short requestType = DNSDatagramHandler.extractRequestTypeFromDNSHeader(dnsRequest);
        DNS currentDNSRequest = dnsRequest;
        final List<DNSResourceRecord> answerList = new ArrayList<>();

        while (true) {

            final DNS dnsResponse = dnsQueryResolver.resolveQueryRecursive(currentDNSRequest, dnsServerIP);
            answerList.addAll(dnsResponse.getAnswers());

            if (DNSDatagramHandler.dnsResponseContainsRecordType(dnsResponse, requestType)) {
                dnsResponse.setAnswers(answerList);
                return dnsResponse;
            }

            currentDNSRequest = generateDNSRequestForCNAME(dnsRequest, dnsResponse, requestType);
        }
    }

    private DNS respondToClientDNSRequest(final DNS dnsRequest, final InetAddress dnsServerIP) throws IOException {
        final DNS dnsResponse = dnsRequest.isRecursionDesired() ?
                getResponseForRecursiveRequest(dnsRequest, dnsServerIP) :
                dnsQueryResolver.resolveQueryRecursive(dnsRequest, dnsServerIP);
        dnsResponse.setQuestions(dnsRequest.getQuestions());
        return modifyAndReturnResponseWithEC2Records(dnsResponse);
    }

    void resolveRequest(final InetAddress dnsServerIP) throws IOException, NullPointerException {

        final DatagramPacket dnsPacket = clientToSimpleDNSConnection.waitForAndReceiveDatagramPacket();
        final DNS dnsRequest = DNSDatagramHandler.extractDNSHeaderFromDatagramPacket(dnsPacket);
        dnsRequest.setAdditional(new ArrayList<>());

        DNSDatagramHandler.printDNS(dnsRequest, dnsServerIP, "CLIENT DNS REQUEST");

        if (!DNSDatagramHandler.canHandleDNSRequest(dnsRequest)) {
            System.out.println("Cannot handle: "+dnsRequest.toString());
            return;
        }

        final InetAddress clientIPAddress = dnsPacket.getAddress();
        final int clientPort = dnsPacket.getPort();

        final DNS dnsResponse = respondToClientDNSRequest(dnsRequest, dnsServerIP);
        DNSDatagramHandler.printDNS(dnsResponse, dnsServerIP, "RESPONSE TO CLIENT DNS REQUEST");

        clientToSimpleDNSConnection.sendDNSPacket(dnsResponse, clientIPAddress, clientPort);
    }

    void teardownResolver() {
        clientToSimpleDNSConnection.teardownConnection();
        dnsQueryResolver.teardownResolver();
    }
}
