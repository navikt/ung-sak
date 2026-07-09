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
    OPPHØR_OPPHEVET_UNGDOMSPROGRAM,

    /**
     * Ingen vedtaksbrev sendes, siden opphøret aldri ble vedtatt/iverksatt (ingen opphørsbrev til bruker
     * å reversere). Kun sporing via behandlingsårsak og historikkinnslag.
     */
    OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING_UNGDOMSPROGRAM,

    KONTROLL_AV_INNTEKT,

    INGEN_RELEVANT_BEHANDLINGÅRSAK,
    FLERE_BEHANDLINGÅRSAKER
}
