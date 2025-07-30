package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;

@Dependent
public final class EndringBarnDødsfallStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private final boolean enableAutoBrevVedBarnDødsfall;

    @Inject
    public EndringBarnDødsfallStrategy(
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        @KonfigVerdi(value = "ENABLE_AUTO_BREV_BARN_DØDSFALL", defaultVerdi = "false") boolean enableAutoBrevVedBarnDødsfall
    ) {
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.enableAutoBrevVedBarnDødsfall = enableAutoBrevVedBarnDødsfall;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return new VedtaksbrevStrategyResultat(null, "Ingen brev ved dødsfall av barn.", IngenBrevÅrsakType.IKKE_IMPLEMENTERT);
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        if (enableAutoBrevVedBarnDødsfall) {
            return false;
        }

        if (harSatsendringenDødsfall(behandling)) {
            return true;
        }

        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);

        return resultater.innholder(DetaljertResultatType.ENDRING_BARN_DØDSFALL);
    }

    private boolean harSatsendringenDødsfall(Behandling behandling) {
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId());
        if (ungdomsytelseGrunnlag.isPresent()) {
            LocalDateTimeline<UngdomsytelseSatser> satsTidslinje = ungdomsytelseGrunnlag.get().getSatsTidslinje();
            var satsSegments = satsTidslinje.toSegments();
            LocalDateSegment<UngdomsytelseSatser> previous = null;
            for (LocalDateSegment<UngdomsytelseSatser> current : satsSegments) {
                if (previous == null) {
                    previous = current;
                    continue;
                }
                if (SatsEndring.bestemSatsendring(current.getValue(), previous.getValue()).dødsfallBarn()) {
                    return true;
                }
                previous = current;
            }
        }
        return false;
    }

}
