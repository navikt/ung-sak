package no.nav.k9.sak.web.server.jetty;

public class JettyWebKonfigurasjon implements AppKonfigurasjon {
    public static final String CONTEXT_PATH = "/k9/sak";
    private static final String SWAGGER_HASH = "sha256-q/YPt9L9Ie+qVycDQ7fOW4abIqYB+EE3F18SkqJJZcQ=";

    private Integer serverPort;

    public JettyWebKonfigurasjon() {}

    public JettyWebKonfigurasjon(int serverPort) {
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

    @Override
    public String getSwaggerHash() {
        return SWAGGER_HASH;
    }


}
