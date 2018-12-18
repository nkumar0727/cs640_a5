package edu.wisc.cs.sdn.simpledns;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

class CSVLoader {

    private List<CIDRRecordAndRegion> csvRecordList;

    void loadCSV(final String csvFilePath) throws IOException, IllegalArgumentException {
        csvRecordList = new ArrayList<>();

        FileReader file = new FileReader(csvFilePath);
        BufferedReader reader = new BufferedReader(file);

        String currentLine;
        while((currentLine = reader.readLine()) != null) {
            currentLine = currentLine.trim();
            if (!currentLine.isEmpty()) {
                final CIDRRecordAndRegion cidrRecordAndRegion =
                        CIDRConverter.convertStringIntoCIDRRecordAndRegion(currentLine);
                csvRecordList.add(cidrRecordAndRegion);
            }
        }
    }

    String getRegionOfIPAddress(final String ipDottedDecimal) throws UnknownHostException {

        final int ipAddress = CIDRConverter.convertDottedDecimalStringToIPAddressInteger(ipDottedDecimal);

        for (CIDRRecordAndRegion cidrRecordAndRegion : csvRecordList) {
            if (cidrRecordAndRegion.isMatchingIPAddress(ipAddress)) {
                return cidrRecordAndRegion.getRegion();
            }
        }

        return null;
    }
}
