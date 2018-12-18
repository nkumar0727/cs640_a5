package edu.wisc.cs.sdn.simpledns;

class NetworkAddressAndSubnetMask {

    private int networkAddress;
    private int subnetMask;

    NetworkAddressAndSubnetMask(final int networkAddress, final int subnetMask) {
        this.networkAddress = networkAddress;
        this.subnetMask = subnetMask;
    }

    boolean isMatchingIPAddress(final int ipAddress) {
        return (networkAddress & subnetMask) == (ipAddress & subnetMask);
    }
}
