package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatInfo;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regler for å bestemme riktig bygger av vedtaksbrev. Kan også avgjøre om brev IKKE skal bestilles.
 * Reglene skal være rekkefølgeuavhengig, men regler som fører til ingen brev har høyere presedens.
 *
 */
public sealed interface VedtaksbrevInnholdbyggerStrategy
    permits EndringBarnetilleggStrategy, EndringHøySatsStrategy, EndringInntektFullUtbetalingStrategy, EndringInntektReduksjonStrategy, OpphørStrategy, EndringProgramPeriodeStrategy, FørstegangsInnvilgelseStrategy, EndringBarnDødsfallStrategy {

    VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat);

    boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat);

    static Set<DetaljertResultatInfo> tilResultatInfo(LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatInfo().stream())
            .collect(Collectors.toSet());
    }

}
