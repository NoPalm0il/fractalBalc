# Fractal Balancer
- Server with sockets and RMIs to balance servers connected for the purpose to generate fractals.

## Balancer
- Uses RMI to exchange data with connected servers, main objective is that every server does the same amount off work.

- After all servers stopped working, it re-assembles all frames.

- And a simple server socket to accept connections from the client (android).

## Server
- Connects to the balancer through RMI.

- Generates the fractal frames and sends to the balancer.
