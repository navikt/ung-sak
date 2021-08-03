package no.nav.k9.sak.web.server.jetty;

import no.nav.k9.felles.sikkerhet.ContextPathHolder;

public class JettyWebKonfigurasjon implements AppKonfigurasjon {
    public static final String COOKIE_PATH = "/k9";
    public static final String CONTEXT_PATH = COOKIE_PATH + "/sak";

    private Integer serverPort;

    public JettyWebKonfigurasjon() {
        ContextPathHolder.instance(CONTEXT_PATH, COOKIE_PATH);
    }

    public JettyWebKonfigurasjon(int serverPort) {
        this();
        this.serverPort = serverPort;
    }

    @Override
    public int getServerPort() {
        if (serverPort == null) {
            return AppKonfigurasjon.DEFAULT_SERVER_PORT;
        }
        return serverPort;
    }

    @Override
    public String getContextPath() {
        return CONTEXT_PATH;
    }

    @Override
    public int getSslPort() {
        throw new IllegalStateException("SSL port should only be used locally");
    }

}
