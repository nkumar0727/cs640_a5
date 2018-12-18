package edu.wisc.cs.sdn.simpledns;

import edu.wisc.cs.sdn.simpledns.packet.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

    private void addTXTAnswerIfEC2IP(final List<DNSResourceRecord> additionalEC2Answers, final DNSResourceRecord record,
                                     final DNS dnsResponse) throws UnknownHostException {

        final String resolvedIP = record.getData().toString();
        final String ec2Region = csvLoader.getRegionOfIPAddress(resolvedIP);
        if (ec2Region != null) {
            final String txtRecordDataString = String.format("%s-%s", ec2Region, resolvedIP);
            final DNSRdata txtRecordData = new DNSRdataString(txtRecordDataString);
            final DNSResourceRecord txtRecord = new DNSResourceRecord(dnsResponse.getQuestions().get(0).getName(),
                    DNS.TYPE_TXT, txtRecordData);
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
            addTXTAnswerIfEC2IP(additionalEC2Answers, answerRecord, dnsResponse);
        }

        dnsResponse.getAnswers().addAll(additionalEC2Answers);
        return dnsResponse;
    }

    private DNS generateDNSRequestForCNAME(final DNS dnsResponse, final short requestType) {
        final String cnameDomain = DNSDatagramHandler.extractCNAMEAnswerFromDNSResponse(dnsResponse);
        final DNSQuestion cnameQuestion = new DNSQuestion(cnameDomain, requestType);
        return DNSDatagramHandler.generateDNSRequest(cnameQuestion, dnsResponse.getId());
    }

    private List<DNSResourceRecord> getAnswersForRecursiveRequest(final DNS dnsRequest, final InetAddress dnsServerIP)
            throws IOException {

        final short requestType = DNSDatagramHandler.extractRequestTypeFromDNSHeader(dnsRequest);
        DNS currentDNSRequest = dnsRequest;
        final List<DNSResourceRecord> answerList = new ArrayList<>();

        while (true) {

            System.out.println("===================================================================");
            System.out.println("Recursive Request");
            System.out.println(dnsRequest.toString());
            System.out.println("===================================================================");

            final DNS dnsResponse = dnsQueryResolver.resolveQueryRecursive(currentDNSRequest, dnsServerIP);
            answerList.addAll(dnsResponse.getAnswers());

            System.out.println("===================================================================");
            System.out.println("Response for Recursive Request");
            System.out.println(dnsResponse.toString());
            System.out.println("===================================================================");

            if (!DNSDatagramHandler.dnsResponseContains_CNAMERecord(dnsResponse) || requestType == DNS.TYPE_CNAME) {
                break;
            }
            currentDNSRequest = generateDNSRequestForCNAME(dnsResponse, requestType);
        }

        return answerList;
    }

    void resolveRequest(final InetAddress dnsServerIP) throws IOException, NullPointerException {

        final DatagramPacket dnsPacket = clientToSimpleDNSConnection.waitForAndReceiveDatagramPacket();
        final DNS dnsRequest = DNSDatagramHandler.extractDNSHeaderFromDatagramPacket(dnsPacket);

        if (!DNSDatagramHandler.canHandleDNSRequest(dnsRequest)) {
            System.out.println("Cannot handle: "+dnsRequest.toString());
            return;
        }

        final InetAddress clientIPAddress = dnsPacket.getAddress();
        final int clientPort = dnsPacket.getPort();

        System.out.println("===================================================================");
        System.out.println("REQUEST");
        System.out.println(dnsRequest.toString());
        System.out.println("===================================================================");

        DNS dnsResponse;
        if (!dnsRequest.isRecursionDesired()) {
            dnsResponse = dnsQueryResolver.resolveQueryNonRecursive(dnsRequest, dnsServerIP);
        } else {
            List<DNSResourceRecord> answerList = getAnswersForRecursiveRequest(dnsRequest, dnsServerIP);
            dnsResponse = DNSDatagramHandler.generateDNSResponse(dnsRequest, answerList);
        }

        dnsResponse = modifyAndReturnResponseWithEC2Records(dnsResponse);

        System.out.println("===================================================================");
        System.out.println("RESPONSE");
        System.out.println(dnsResponse.toString());
        System.out.println("===================================================================");

        clientToSimpleDNSConnection.sendDNSPacket(dnsResponse, clientIPAddress, clientPort);
    }

    void teardownResolver() {
        clientToSimpleDNSConnection.teardownConnection();
        dnsQueryResolver.teardownResolver();
    }
}
