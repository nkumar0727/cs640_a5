package edu.wisc.cs.sdn.simpledns;
import java.net.*;
import java.io.IOException;

public class SimpleDNS {

    private static boolean argumentsAreValid(final String[] args) {
        return (args.length != 4 ||
                (!args[0].equals("-r") || !args[2].equals("-e")) ||
                (!args[2].equals("-r") || !args[0].equals("-e")));
    }

    private static String extractCSVPathFromArgs(final String[] args) {
        return args[0].equals("-e") ? args[1] : args[3];
    }

    private static String extractRootNameserverFromArgs(final String[] args) {
        return args[0].equals("-r") ? args[1] : args[3];
    }

	public static void main(String[] args)
	{
        if (!argumentsAreValid(args)) {
            System.out.println("Usage:\njava edu.wisc.cs.sdn.simpledns.SimpleDNS -r <root server ip> -e <ec2 csv>\nor");
            System.out.println("java edu.wisc.cs.sdn.simpledns.SimpleDNS -e <ec2 csv> -r <root server ip>");
            return;
        }

        final InetAddress rootDNSIPAddress;
        final String csvPath = extractCSVPathFromArgs(args);
        final String rootIPString = extractRootNameserverFromArgs(args);

        try {
            rootDNSIPAddress = InetAddress.getByName(rootIPString);
        } catch (final UnknownHostException ex) {
            System.out.printf("Bad ip address provided: %s\n", ex.toString());
            return;
        }

        final CSVLoader csvLoader = new CSVLoader();
        try {
            csvLoader.loadCSV(csvPath);
        } catch (final IOException | IllegalArgumentException ex) {
            System.out.printf("Could not read file %s due to: %s\n", csvPath, ex.toString());
            return;
        }

        DNSRequestResolver dnsRequestResolver = null;
        while (true) {
            try {
                dnsRequestResolver = new DNSRequestResolver(csvLoader);
                dnsRequestResolver.resolveRequest(rootDNSIPAddress);
            } catch (final IOException ex) {
                System.out.printf("Issue resolving request due to: %s\n", ex.toString());
                ex.printStackTrace();
            } finally {
                if (dnsRequestResolver != null) {
                    dnsRequestResolver.teardownResolver();
                }
            }
        }
    }
}
