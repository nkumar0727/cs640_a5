# dns-server-project
simple dns server for uni project

## Overview

## Requirements
+ Java JDK 8

## Usage

`java edu.wisc.cs.sdn.simpledns.SimpleDNS -r <root server ip> -e <ec2 csv>`

+ -r <root server ip> specifies the IP address of a root DNS server
+ -e <ec2 csv> specifies the path to a comma-separated variable (CSV) file that contains entries specifying the IP address ranges for each Amazon EC2 region
