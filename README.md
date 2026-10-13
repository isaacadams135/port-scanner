# port-scanner

A small multithreaded TCP port scanner written in Java. I built it while studying
for the Security+ exam to understand how a connect scan actually works under the
hood. It uses plain TCP connections, so it needs no admin/root privileges and
behaves like any other client connecting to the host.

## Features

- scans a list of ports, a range, or both (`-p 22,80,443`, `-p 1-1024`)
- parallel connections with a tunable thread pool
- optional banner grabbing on the same connection (`-b`)
- common service names for well-known ports
- plain table output, optionally written to a file (`-o`)
- sensible exit codes for scripting: 0 ok, 1 runtime error, 2 usage error

## Build

Requires JDK 21+.

```bash
./gradlew build      # compile + run tests
./gradlew jar        # build/libs/port-scanner-1.0.0.jar
```

On Windows use `gradlew.bat` instead.

## Usage

```
usage: port-scanner [options] <host>

options:
  -p, --ports <spec>    ports to scan: "22,80,443" or "1-1024" or both
                                 (default: 20 common ports)
  -t, --threads <n>     parallel connections (default: 100)
  -T, --timeout <ms>    connect timeout (default: 800)
  -b, --banner          grab a banner from each open port
  -o, --output <file>   write the results to a file as well
  -h, --help            show this help
```

### Example

```bash
java -jar build/libs/port-scanner-1.0.0.jar 127.0.0.1 -p 20-25 -b
```

```
host   127.0.0.1
ports  6 scanned in 0.44s

port     service          banner
22       ssh              SSH-2.0-OpenSSH_9.6

1 open port
```

## How it works

Every port in the list is submitted to a fixed thread pool. A worker opens a
socket with a connect timeout - connection refused or timeout means the port is
closed, a completed handshake means it's open. With `-b` the worker then reads
up to 256 bytes from that same connection looking for a banner. Protocols like
SSH, SMTP and FTP announce themselves first; HTTP servers usually wait for a
request, so those come back with an empty banner.

## Only scan what you own

Scanning systems you don't own or have permission to test can be illegal
depending on where you live. Use this on your own machines, a home lab, or
networks you have written approval for.

## License

MIT
