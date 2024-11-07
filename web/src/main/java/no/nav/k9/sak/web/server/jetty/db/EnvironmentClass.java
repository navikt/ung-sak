package no.nav.k9.sak.web.server.jetty.db;
public enum EnvironmentClass {
    LOCALHOST, PREPROD, PROD;

    public String mountPath() {
        return "postgresql/" + name().toLowerCase() + "-fss";
    }
}