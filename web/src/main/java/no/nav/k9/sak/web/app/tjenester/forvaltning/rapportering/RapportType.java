package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.Set;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

public enum RapportType {

    UTBETALING_PER_DAG(FagsakYtelseType.PSB, FagsakYtelseType.OMP, FagsakYtelseType.FRISINN)

    ;

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
