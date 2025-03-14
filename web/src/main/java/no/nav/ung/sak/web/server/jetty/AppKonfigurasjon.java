package no.nav.ung.sak.web.server.jetty;

public interface AppKonfigurasjon {
    int DEFAULT_SERVER_PORT = 8901;
    default int getServerPort() {return DEFAULT_SERVER_PORT;}

    String getContextPath();

    default int getSslPort() {
        throw new IllegalStateException("SSL port should only be used locally");
    }
}
