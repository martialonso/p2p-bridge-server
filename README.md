# p2p-bridge-server
Simple server to bridge peer-to-peer connections behind a firewall/NAT

Create a peer to peer connection between to devices that are behind a firewall/NAT. Both devices initiate the connection to the same server through the same port,
the server then forwards all the data one device sends, to the other and vice versa.

## Usage
Simply run the jar file with the argument `-Dport=(port)` (Default: 7635).  
Use `-Dssl` for ssl connections (Without -Dssl, connection is plain text)

## Usage example
Example of a situation where this could be useful _(why I personally use it for)_

(TODO: insert diagram here)

Drone with Ardupilot with a phone for internet connectivity onboard.
PC with Mission Planner to receive drone telemetry.

Ardupilot and Mission Planner both connect to the server to simulate a peer to peer connection between Ardupilot and Mission Planner.
