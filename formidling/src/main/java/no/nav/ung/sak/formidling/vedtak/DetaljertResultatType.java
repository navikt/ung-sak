package no.nav.ung.sak.formidling.vedtak;

public enum DetaljertResultatType {
    AVSLAG_INNGANGSVILKÅR("Avslag inngangsvilkår"),
    ENDRING_ØKT_SATS("Endring økt sats 25 prosent"),
    ENDRING_BARN_FØDSEL("Endring pga fødsel av nytt barn"),
    KONTROLLER_INNTEKT_REDUKSJON("Reduksjon etter kontroll av inntekt"),
    KONTROLLER_INNTEKT_FULL_UTBETALING("Full utbetaling etter kontroll av inntekt"),
    KONTROLLER_INNTEKT_INGEN_UTBETALING("Ingen utbetaling etter kontroll av inntekt"),
    ENDRING_STARTDATO_BAKOVER("Innvilgelse av periode etter flytting av startdato bakover"),
    ENDRING_STARTDATO_FREMOVER("Avslått periode etter endret startdato"),
    ENDRING_OPPHØR_FREMOVER("Innvilgelse av periode etter flytting av sluttdato fremover"),
    ENDRING_OPPHØR("Avslått periode etter fastsettelse av sluttdato eller flytting av sluttdato bakover"),
    IKKE_VURDERT("Ikke vurdert"),
    INNVILGELSE_VILKÅR_NY_PERIODE("Innvilgelse av vilkår for ny periode uten utbetaling"),
    INNVILGELSE_UTBETALING_NY_PERIODE("Innvilgelse av ny periode med utbetaling"),
    //Bør spisse nærmere når det kommer brevtester
    AVSLAG_ANNET("Avslag pga annen årsak - se forklaring"),
    //Bør spisse nærmere når det kommer brevtester,
    INNVILGELSE_ANNET("Innvilgelse pga annen årsak - se forklaring"),
    INNVILGET_UTEN_ÅRSAK("Innvilgelse uten behandlingsårsak")
    ;

    private final String navn;

    DetaljertResultatType(String navn) {
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }
}
