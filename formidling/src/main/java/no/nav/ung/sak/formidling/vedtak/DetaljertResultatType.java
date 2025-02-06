package no.nav.ung.sak.formidling.vedtak;

public enum DetaljertResultatType {
    INNVILGET_NY_PERIODE("Innvilget ny periode"),
    AVSLAG_INNGANGSVILKÅR("Avslag inngangsvilkår"),
    AVSLAG_UTTAK("Avslag uttak"),
    ENDRING_ØKT_SATS("Endring økt sats 25 prosent"),
    ;

    private final String beskrivelse;

    DetaljertResultatType(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }
}
