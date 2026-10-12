package io.github.koka99cab.multisolo.relay;

import java.net.InetSocketAddress;
import java.nio.file.Path;

public final class RelayMain {
    private RelayMain() {}
    public static void main(String[] args) throws Exception {
        String bind = System.getenv().getOrDefault("MULTISOLO_BIND", "127.0.0.1");
        int port = Integer.parseInt(System.getenv().getOrDefault("MULTISOLO_PORT", "8787"));
        Path data = Path.of(System.getenv().getOrDefault("MULTISOLO_DATA", "data/messages-v2"));
        RelayServer server = new RelayServer(new InetSocketAddress(bind, port), data);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        System.out.println("Multisolo relay listening on " + bind + ":" + server.port());
    }
}
