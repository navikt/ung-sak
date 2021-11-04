package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.PeriodeMedMangler;

class EtterlysningInput {

    private Map<String, LocalDateTime> aksjonspunkter;
    private Map<DatoIntervallEntitet, List<ManglendeVedlegg>> relevanteMangler;

    EtterlysningInput(Map<String, LocalDateTime> aksjonspunkter, Map<DatoIntervallEntitet, List<ManglendeVedlegg>> relevanteMangler) {
        this.aksjonspunkter = Objects.requireNonNull(aksjonspunkter);
        this.relevanteMangler = Objects.requireNonNull(relevanteMangler);
    }

    Map<String, LocalDateTime> getAksjonspunkter() {
        return aksjonspunkter;
    }

    List<PeriodeMedMangler> getRelevanteMangler() {
        return relevanteMangler.entrySet()
            .stream()
            .map(it -> new PeriodeMedMangler(it.getKey(), it.getValue()))
            .collect(Collectors.toList());
    }
}
