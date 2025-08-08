package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.SatsEndring;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

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
        return VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_IMPLEMENTERT, "Ingen brev ved dødsfall av barn.");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        if (enableAutoBrevVedBarnDødsfall) {
            return false;
        }

        if (harSatsendringenDødsfall(behandling, detaljertResultat)) {
            return true;
        }

        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);

        return resultater.innholder(DetaljertResultatType.ENDRING_BARN_DØDSFALL);
    }

    private boolean harSatsendringenDødsfall(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId());
        if (ungdomsytelseGrunnlag.isPresent()) {
            LocalDateTimeline<UngdomsytelseSatser> satsTidslinje = ungdomsytelseGrunnlag.get().getSatsTidslinje().intersection(detaljertResultat);
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
