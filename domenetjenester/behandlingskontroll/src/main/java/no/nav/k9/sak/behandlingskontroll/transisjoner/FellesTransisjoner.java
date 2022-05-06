package no.nav.k9.sak.behandlingskontroll.transisjoner;

public class FellesTransisjoner {

    public static final String FREMHOPP_PREFIX = "fremhopp-til-";
    public static final String SPOLFREM_PREFIX = "revurdering-fremoverhopp-til-";

    public static final TransisjonIdentifikator UTFØRT = TransisjonIdentifikator.forId("utført");
    public static final TransisjonIdentifikator STARTET = TransisjonIdentifikator.forId("startet");
    public static final TransisjonIdentifikator HENLAGT = TransisjonIdentifikator.forId("henlagt");
    public static final TransisjonIdentifikator SETT_PÅ_VENT = TransisjonIdentifikator.forId("sett-på-vent");
    public static final TransisjonIdentifikator TILBAKEFØRT_TIL_STEG = TransisjonIdentifikator.forId("tilbakeført-til-steg");
    public static final TransisjonIdentifikator TILBAKEFØRT_TIL_AKSJONSPUNKT = TransisjonIdentifikator.forId("tilbakeført-til-aksjonspunkt");
    public static final TransisjonIdentifikator FREMHOPP_TIL_IVERKSETT_VEDTAK = TransisjonIdentifikator.forId(FREMHOPP_PREFIX + "iverksett-vedtak");

    private FellesTransisjoner() {
        //hindrer instansiering
    }

    public static boolean erFremhoppTransisjon(TransisjonIdentifikator transisjonIdentifikator) {
        return transisjonIdentifikator.getId().startsWith(FREMHOPP_PREFIX);
    }

    public static boolean erSpolfremTransisjon(TransisjonIdentifikator transisjonIdentifikator) {
        return transisjonIdentifikator.getId().startsWith(SPOLFREM_PREFIX);
    }
}
