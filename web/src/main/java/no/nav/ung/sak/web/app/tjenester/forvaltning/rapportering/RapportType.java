package no.nav.ung.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public enum RapportType {

    UTBETALING_PER_DAG(FagsakYtelseType.UNGDOMSYTELSE),
    UTBETALING_PER_BRUKER(FagsakYtelseType.UNGDOMSYTELSE),
    G_REGULERING(FagsakYtelseType.UNGDOMSYTELSE)
    ;

    @JsonIgnore
    private Set<FagsakYtelseType> ytelseTyper;

    private RapportType(FagsakYtelseType... ytelseTyper) {
        this.ytelseTyper = Set.of(ytelseTyper);
    }

    public void valider(FagsakYtelseType ytelseType) {
        if (!ytelseTyper.contains(ytelseType)) {
            throw new IllegalArgumentException("Støtter ikke dette uttrekket [" + this.name() + "] for ytelseType:" + ytelseType + ", tillater kun: " + ytelseTyper);
        }
    }
}
