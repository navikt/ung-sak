package no.nav.ung.sak.formidling.vedtak;

public enum DetaljertResultatType {
    AVSLAG_INNGANGSVILKÅR("Avslag inngangsvilkår"),
    ENDRING_ØKT_SATS("Endring økt sats 25 prosent"),
    ENDRING_BARN_FØDSEL("Endring pga fødsel av nytt barn"),
    KONTROLLER_INNTEKT_REDUKSJON("Reduksjon etter kontroll av inntekt"),
    KONTROLLER_INNTEKT_FULL_UTBETALING("Full utbetaling etter kontroll av inntekt"),
    KONTROLLER_INNTEKT_INGEN_UTBETALING("Ingen utbetaling etter kontroll av inntekt"),
    INNVILGELSE_ENDRING_STARTDATO("Innvilgelse av ny periode etter endring av startdato"),
    AVSLAG_ENDRING_STARTDATO("Avslag av tidligere periode etter endret startdato"),
    IKKE_VURDERT("Ikke vurdert"),
    INNVILGELSE_VILKÅR_NY_PERIODE("Innvilgelse av vilkår for ny periode uten utbetaling"),
    INNVILGELSE_UTBETALING_NY_PERIODE("Innvilgelse av ny periode med utbetaling"),
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
