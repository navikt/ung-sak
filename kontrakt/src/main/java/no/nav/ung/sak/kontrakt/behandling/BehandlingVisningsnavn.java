package no.nav.ung.sak.kontrakt.behandling;

public enum BehandlingVisningsnavn {
    BEREGNING_AV_HØY_SATS,
    ENDRING_AV_BARNETILLEGG,
    BRUKERS_DØDSFALL,
    UNGDOMSPROGRAMENDRING,
    OPPHØR_VED_MAKSDATO,

    /**
     * Et tidligere opphør av ungdomsprogrammet er reversert, og det tidligere opphøret ble faktisk
     * vedtatt/iverksatt (dvs. bruker mottok et opphørsbrev). Gir eget vedtaksbrev om opphevelsen.
     */
    OPPHØR_OPPHEVET,

    /**
     * Et opphør av ungdomsprogrammet ble avbrutt før det noen gang ble vedtatt/iverksatt, f.eks. fordi
     * veileder fjernet sluttdatoen på nytt mens behandlingen med opphørsårsak fortsatt ventet på uttalelse
     * fra deltaker. Ingen opphørsbrev ble noen gang sendt, og det sendes derfor heller ikke noe eget
     * vedtaksbrev om annulleringen — kun sporing via behandlingsårsak og historikkinnslag.
     */
    OPPHØR_ANNULERT,

    KONTROLL_AV_INNTEKT,

    INGEN_RELEVANT_BEHANDLINGÅRSAK,
    FLERE_BEHANDLINGÅRSAKER
}
