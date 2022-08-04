package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan.AksjonPerFagsak;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan.KravPrioInput;

public class DebugKjøreplan {
    private KravPrioInput input;

    private LocalDateTimeline<Set<AksjonPerFagsak>> kjøreplan;

    public DebugKjøreplan(KravPrioInput input, LocalDateTimeline<Set<AksjonPerFagsak>> kjøreplan) {
        this.input = input;
        this.kjøreplan = kjøreplan;
    }

    public DebugKjøreplan() {
    }

    public KravPrioInput getInput() {
        return input;
    }

    public LocalDateTimeline<Set<AksjonPerFagsak>> getKjøreplan() {
        return kjøreplan;
    }
}
