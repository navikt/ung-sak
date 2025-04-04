package no.nav.ung.sak.formidling.vedtak;

public enum DetaljertResultatType {
    INNVILGELSE_VILKÅR("Innvilgelse av vilkår for ny periode ny periode"),
    AVSLAG_INNGANGSVILKÅR("Avslag inngangsvilkår"),
    AVSLAG_UTTAK("Avslag uttak"),
    ENDRING_ØKT_SATS("Endring økt sats 25 prosent"),
    ENDRING_RAPPORTERT_INNTEKT("Reduksjon pga inntekt"),
    AVSLAG_RAPPORTERT_INNTEKT("Avslag pga rapportert inntekt"),
    IKKE_VURDERT("Ikke vurdert"),
    INNVILGELSE_MED_UTBETALING("Innvilgelse av ny periode med utbetaling");

    private final String beskrivelse;

    DetaljertResultatType(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }
}
