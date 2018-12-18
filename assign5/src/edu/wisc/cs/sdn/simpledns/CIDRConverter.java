package edu.wisc.cs.sdn.simpledns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

class CIDRConverter {

    static CIDRRecordAndRegion convertStringIntoCIDRRecordAndRegion(final String record)
            throws IllegalArgumentException, UnknownHostException {

        final String[] cidrRecord = record.split("[/,]");
        if (cidrRecord.length != 3) {
            throw new IllegalArgumentException("Cannot parse record: "+record);
        }

        final int networkAddress = convertDottedDecimalStringToIPAddressInteger(cidrRecord[0]);
        final int subnetMask = convertCIDRPrefixToSubnetMaskInteger(cidrRecord[1]);

        return new CIDRRecordAndRegion(new NetworkAddressAndSubnetMask(networkAddress,
                subnetMask), cidrRecord[2]);
    }

    private static int convertCIDRPrefixToSubnetMaskInteger(final String cidrPrefix) throws NumberFormatException {
        final int prefixAsInteger = Integer.parseInt(cidrPrefix);
        return (0xffffffff) << (Integer.SIZE - prefixAsInteger);
    }

    static int convertDottedDecimalStringToIPAddressInteger(final String networkAddress)
            throws UnknownHostException {

        final InetAddress ipAddress = InetAddress.getByName(networkAddress);
        final ByteBuffer buffer = ByteBuffer.wrap(ipAddress.getAddress());
        return buffer.get();
    }

    static InetAddress convertDottedDecimalStringToInetAddress(final String networkAddress)
            throws UnknownHostException {

        return InetAddress.getByName(networkAddress);
    }
}
