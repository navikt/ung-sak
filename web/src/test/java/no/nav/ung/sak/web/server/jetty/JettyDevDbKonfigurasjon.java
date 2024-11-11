package no.nav.ung.sak.web.server.jetty;

/** Dummy konfig for lokal testing. */
public class JettyDevDbKonfigurasjon {

    private String datasource = "defaultDS";
    private String url = "jdbc:postgresql://127.0.0.1:5432/ung_sak?reWriteBatchedInserts=true";
    private String user = "ung_sak";
    private String password = user;

    JettyDevDbKonfigurasjon() {
    }

    public String getDatasource() {
        return datasource;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

}
