from sys import argv
import simplejson as json
import struct, socket

def extract_cidr_from_string(record):

    (record_ip_str, record_mask_str) = record.split('/')
    host_bits = 32 - int(record_mask_str)
    record_mask = (1 << 32) - (1 << host_bits)
    record_ip = struct.unpack('!L', socket.inet_aton(record_ip_str))[0]          

    return (record_ip, record_mask)

def report_match(domain_name, input_ip_str, input_ip, record_ip, record_mask,
record):

    res_input = input_ip & record_mask
    res_record = record_ip & record_mask

    if res_input == res_record:
        print('<%s, %s> matches with record %s' % (domain_name, input_ip_str, record))


def main(argv):

    if len(argv) < 2:
        print('Usage: python check_domain_cdn.py <domain_name1> <domain_name2> ...')
        return

    ip_str = None

    for idx in range(1,len(argv)):
        print('> PROCESSING %s' % argv[idx])
        print('============================================================================')
        try:
            ip_str = socket.gethostbyname(argv[idx])
        except:
            print('Issue with DNS lookup of %s' % argv[idx])
            continue
            
        input_ip = struct.unpack('!L', socket.inet_aton(ip_str))[0]

        print('Checking if <%s, %s> goes through Amazon\'s Network...' %
        (argv[idx], ip_str))
        with open('./ip-ranges.json') as json_data:

            d = json.load(json_data)
            prefixes_json = d['prefixes']

            for record in prefixes_json:
                (record_ip, record_mask) = extract_cidr_from_string(record['ip_prefix'])
                report_match(argv[1], ip_str, input_ip, record_ip, record_mask, record)

        print('\nChecking if <%s, %s> resolves to node in EdgeCast CDN...' %
        (argv[idx], ip_str))
        with open('./edgecast-ip-ranges.txt') as edgecast_ip_list:

            for record in edgecast_ip_list:
                (record_ip, record_mask) = extract_cidr_from_string(record) 
                report_match(argv[1], ip_str, input_ip, record_ip, record_mask, record)

        print('============================================================================\n')

if __name__ == "__main__":
    main(argv)

