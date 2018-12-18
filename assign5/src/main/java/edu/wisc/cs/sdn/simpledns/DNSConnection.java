package edu.wisc.cs.sdn.simpledns;

import edu.wisc.cs.sdn.simpledns.packet.DNS;

import java.io.IOException;
import java.net.*;

class DNSConnection {

    private static final int PACKET_BUFFER_SIZE = 4096;

    private DatagramSocket socketConnection;

    DNSConnection(final int listenPort) throws SocketException {
        socketConnection = new DatagramSocket(listenPort);
    }

    DNSConnection() throws SocketException {
        socketConnection = new DatagramSocket();
    }

    DatagramPacket waitForAndReceiveDatagramPacket() throws IOException {
        final byte[] buffer = new byte[PACKET_BUFFER_SIZE];
        final DatagramPacket dnsDatagramPacket = new DatagramPacket(buffer, buffer.length);
        socketConnection.receive(dnsDatagramPacket);
        return dnsDatagramPacket;
    }

    void sendDNSPacket(final DNS dnsHeader, final InetAddress ipAddress, final int portNumber) throws IOException {
        final DatagramPacket dnsPacket =
                DNSDatagramHandler.convertDNSHeaderToDatagramPacket(dnsHeader, ipAddress, portNumber);
        socketConnection.send(dnsPacket);
    }

    void teardownConnection() {
        socketConnection.close();
    }
}
