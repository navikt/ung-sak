package no.nav.ung.abac;

public class BeskyttetRessursKoder {

    public static final String APPLIKASJON = "no.nav.abac.attributter.k9";
    public static final String DRIFT = "no.nav.abac.attributter.k9.drift";
    public static final String FAGSAK = "no.nav.abac.attributter.k9.fagsak";
    public static final String VENTEFRIST = "no.nav.abac.attributter.k9.fagsak.ventefrist";
    public static final String BATCH = "no.nav.abac.attributter.k9.batch";
    public static final String SAKLISTE = "no.nav.abac.attributter.k9.sakliste";
    public static final String OPPGAVEKO = "no.nav.abac.attributter.k9.oppgaveko";
    public static final String OPPGAVESTYRING = "no.nav.abac.attributter.k9.oppgavestyring";
    public static final String PIP = "pip.tjeneste.kan.kun.kalles.av.pdp.servicebruker";
    public static final String OPPGAVESTYRING_AVDELINGENHET = "no.nav.abac.attributter.k9.oppgavestyring.avdelingsenhet";
    public static final String UTTAKSPLAN = "no.nav.abac.attributter.resource.k9.uttaksplan";

    /**
     * Egne ressurs definisjoner. Disse brukes kun dersom systembruker. De er ikke inkludert i Abacpolicy, men sikrer at ingen andre får tilgang
     * til denne tjenesten, og spores unikt i sporingslogg.
     */

    /** oppfriskning av mange behandlinger i bolk. */
    public static final String REFRESH_BEHANDLING_REGISTERDATA = "no.nav.abac.attributter.resource.k9.behandling.refresh";

}
