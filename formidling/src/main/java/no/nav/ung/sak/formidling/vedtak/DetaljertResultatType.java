package no.nav.ung.sak.formidling.vedtak;

public enum DetaljertResultatType {
    AVSLAG_INNGANGSVILKÅR("Avslag inngangsvilkår"),
    ENDRING_ØKT_SATS("Endring økt sats 25 prosent"),
    ENDRING_RAPPORTERT_INNTEKT("Reduksjon pga inntekt"),
    AVSLAG_RAPPORTERT_INNTEKT("Avslag pga rapportert inntekt"),
    IKKE_VURDERT("Ikke vurdert"),
    INNVILGELSE_VILKÅR_NY_PERIODE("Innvilgelse av vilkår for ny periode uten utbetaling"),
    INNVILGELSE_UTBETALING_NY_PERIODE("Innvilgelse av ny periode med utbetaling"),
    INNVILGELSE_UTBETALING_UTEN_INNTEKT("Innvilgelse med utbetaling uten rapportert inntekt"),
    //Bør spisse nærmere når det kommer brevtester
    AVSLAG_ANNET("Avslag pga annen årsak - se forklaring"),
    //Bør spisse nærmere når det kommer brevtester
    INNVILGELSE_ANNET("Innvilgelse pga annen årsak - se forklaring"),
    ;

    private final String navn;

    DetaljertResultatType(String navn) {
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }
}
