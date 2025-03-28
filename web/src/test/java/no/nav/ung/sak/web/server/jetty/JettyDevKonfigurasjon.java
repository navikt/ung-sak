package no.nav.ung.sak.web.server.jetty;

public class JettyDevKonfigurasjon extends JettyWebKonfigurasjon {
    private static final int SSL_SERVER_PORT = 8443;
    private static final int DEV_SERVER_PORT = 8901;

    public JettyDevKonfigurasjon() {
        super(DEV_SERVER_PORT);
    }

    @Override
    public int getSslPort() {
        return SSL_SERVER_PORT;
    }

}
