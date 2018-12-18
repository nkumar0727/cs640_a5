package edu.wisc.cs.sdn.simpledns;

class CIDRRecordAndRegion {

    private NetworkAddressAndSubnetMask cidrRecord;
    private String region;

    CIDRRecordAndRegion(final NetworkAddressAndSubnetMask cidrRecord, final String region) {
        this.cidrRecord = cidrRecord;
        this.region = region;
    }

    String getRegion() {
        return region;
    }

    public String toString() {
        return String.format("%s,%s", cidrRecord.toString(), region);
    }

    boolean isMatchingIPAddress(final int ipAddress) {
        return cidrRecord.isMatchingIPAddress(ipAddress);
    }
}
