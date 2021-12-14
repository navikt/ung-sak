package no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum AktivitetStatus {
    MIDL_INAKTIV("Midlertidig inaktiv"),
    ATFL("Arbeidstaker/Frilanser"),
    KUN_YTELSE("Mottaker av tilstøtende ytelse"),
    SN("Selvstendig næringsdrivende"),
    ATFL_SN("Kombinasjon av arbeidstaker/frilanser og selvstendig næringsdrivende"),
    DP("Dagpenger"),
    SP_AV_DP("Sykepenger av dagpenger"),
    PSB_AV_DP("Pleiepenger av dagpenger"),
    AAP("Mottaker av arbeidsavklaringspenger"),
    BA("Brukers andel"),
    MS("Militær/Sivil"),
    ANNET("Annet");

    private static final Set<AktivitetStatus> GRADERBARE_AKTIVITETER = new HashSet<>(Arrays.asList(ATFL, SN, ATFL_SN));

    private final String beskrivelse;

    AktivitetStatus(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public boolean erGraderbar() {
        return GRADERBARE_AKTIVITETER.contains(this);
    }

}
