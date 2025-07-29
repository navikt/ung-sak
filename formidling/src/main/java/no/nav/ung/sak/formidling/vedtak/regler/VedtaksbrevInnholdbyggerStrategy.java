package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatInfo;

import java.util.Set;
import java.util.stream.Collectors;

public sealed interface VedtaksbrevInnholdbyggerStrategy
    permits EndringBarnetilleggByggerStrategy, EndringHøySatsByggerStrategy, EndringInntektByggerStrategy, EndringSluttdatoByggerStrategy, EndringStartdatoByggerStrategy, FørstegangsInnvilgelseByggerStrategy {

    ByggerResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat);

    boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat);

    static Set<DetaljertResultatInfo> tilResultatInfo(LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatInfo().stream())
            .collect(Collectors.toSet());
    }

}
