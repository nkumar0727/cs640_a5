JFLAGS = -g
JC = javac
JVM = java
.SUFFIXES: .java .class
.java.class:
		$(JC) $(JFLAGS) $*.java 

CLASSES = \
		edu/wisc/cs/sdn/simpledns/packet/DNS.java \
		edu/wisc/cs/sdn/simpledns/packet/DNSQuestion.java \
		edu/wisc/cs/sdn/simpledns/packet/DNSRdata.java \
		edu/wisc/cs/sdn/simpledns/packet/DNSRdataAddress.java \
		edu/wisc/cs/sdn/simpledns/packet/DNSRdataBytes.java \
		edu/wisc/cs/sdn/simpledns/packet/DNSRdataName.java \
		edu/wisc/cs/sdn/simpledns/packet/DNSRdataString.java \
		edu/wisc/cs/sdn/simpledns/packet/DNSResourceRecord.java \
		edu/wisc/cs/sdn/simpledns/CIDRConverter.java \
		edu/wisc/cs/sdn/simpledns/CIDRRecordAndRegion.java \
		edu/wisc/cs/sdn/simpledns/CSVLoader.java \
		edu/wisc/cs/sdn/simpledns/DNSConnection.java \
		edu/wisc/cs/sdn/simpledns/DNSDatagramHandler.java \
		edu/wisc/cs/sdn/simpledns/DNSQueryResolver.java \
		edu/wisc/cs/sdn/simpledns/DNSRequestResolver.java \
		edu/wisc/cs/sdn/simpledns/NetworkAddressAndSubnetMask.java \
		edu/wisc/cs/sdn/simpledns/SimpleDNS.java 

all: classes

run:
	$(JVM) edu.wisc.cs.sdn.simpledns.SimpleDNS -r ${ip} -e ${ec2}

classes: $(CLASSES:.java=.class)

clean:
		$(RM) edu/wisc/cs/sdn/simpledns/packet/*.class
		$(RM) edu/wisc/cs/sdn/simpledns/*.class