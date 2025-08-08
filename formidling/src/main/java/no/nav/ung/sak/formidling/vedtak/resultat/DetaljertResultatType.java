package no.nav.ung.sak.formidling.vedtak.resultat;

public enum DetaljertResultatType {
    AVSLAG_INNGANGSVILKÅR("Avslag inngangsvilkår"),
    ENDRING_ØKT_SATS("Endring økt sats 25 prosent"),
    ENDRING_BARN_FØDSEL("Endring pga fødsel av nytt barn"),
    KONTROLLER_INNTEKT_REDUKSJON("Reduksjon etter kontroll av inntekt"),
    KONTROLLER_INNTEKT_FULL_UTBETALING("Full utbetaling etter kontroll av inntekt"),
    KONTROLLER_INNTEKT_INGEN_UTBETALING("Ingen utbetaling etter kontroll av inntekt"),
    KONTROLLER_INNTEKT_UTEN_TILKJENT_YTELSE("Ingen tilkjent ytelse etter kontroll av inntekt"),
    ENDRING_STARTDATO("Endring startdato"),
    ENDRING_SLUTTDATO("Opphør eller endring sluttdato"),
    IKKE_VURDERT("Ikke vurdert"),
    INNVILGELSE_VILKÅR_NY_PERIODE("Innvilgelse av vilkår for ny periode uten utbetaling"),
    INNVILGELSE_UTBETALING_NY_PERIODE("Innvilgelse av ny periode med utbetaling"),
    //Bør spisse nærmere når det kommer brevtester
    AVSLAG_ANNET("Avslag pga annen årsak - se forklaring"),
    //Bør spisse nærmere når det kommer brevtester,
    ENDRING_BARN_DØDSFALL("Endring pga dødsfall av barn"),
    ENDRING_DELTAKER_DØDSFALL("Endring pga dødsfall av deltaker"),
    INNVILGELSE_ANNET("Innvilgelse pga annen årsak - se forklaring"),
    INNVILGET_UTEN_ÅRSAK("Innvilgelse uten behandlingsårsak")
    ;

    private final String beskrivelse;

    DetaljertResultatType(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
