package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum Aktivitet {
    AAP_MOTTAKER,
    ARBEIDSTAKERINNTEKT,
    DAGPENGEMOTTAKER,
    ETTERLØNN_SLUTTPAKKE,
    FORELDREPENGER_MOTTAKER,
    FRILANSINNTEKT,
    MILITÆR_ELLER_SIVILTJENESTE,
    NÆRINGSINNTEKT,
    OMSORGSPENGER,
    OPPLÆRINGSPENGER,
    PLEIEPENGER_MOTTAKER,
    SVANGERSKAPSPENGER_MOTTAKER,
    SYKEPENGER_MOTTAKER,
    VENTELØNN_VARTPENGER,
    VIDERE_ETTERUTDANNING,
    UTDANNINGSPERMISJON,
    UDEFINERT;

    private static final Set<Aktivitet> AKTIVITETER_MED_ORGNR = new HashSet<>(Arrays.asList(ARBEIDSTAKERINNTEKT, FRILANSINNTEKT));

    public boolean harOrgnr() {
        return AKTIVITETER_MED_ORGNR.contains(this);
    }
}
