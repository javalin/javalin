# javalin-mdns

Publishes a custom mDNS hostname for a running Javalin server, so a chosen name like
`mycustomstring.local` resolves to the machine's IP on the local network. Optionally registers
an `_http._tcp` service record so the app shows up in Bonjour/Zeroconf browsers.

## Usage

```kotlin
Javalin.create { config ->
    config.registerPlugin(MdnsPlugin { it.hostname = "mycustomstring" })
}
```

The server is then reachable at `mycustomstring.local` from any device on the same LAN.

## When to use (and when not to)

**Good for** same-LAN, zero-config discovery — reach a server by `name.local` without knowing its IP:

- IoT / edge devices
- Self-hosted / homelab services
- Kiosks / point-of-sale terminals
- On-prem appliances

**Not for** the public internet, across subnets/VLANs, or cloud/Kubernetes. mDNS is link-local
multicast and does not route past the local link — use real DNS or your platform's service
discovery there.

**Security note:** advertising broadcasts the service name and port to everyone on the LAN. Avoid
it on untrusted or shared networks.
