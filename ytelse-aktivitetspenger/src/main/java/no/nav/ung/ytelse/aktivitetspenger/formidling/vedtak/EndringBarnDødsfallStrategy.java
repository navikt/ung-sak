package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class EndringBarnDødsfallStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;
    private final boolean enableAutoBrevVedBarnDødsfall;

    @Inject
    public EndringBarnDødsfallStrategy(
        AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository,
        @KonfigVerdi(value = "ENABLE_AUTO_BREV_BARN_DØDSFALL", defaultVerdi = "false") boolean enableAutoBrevVedBarnDødsfall
    ) {
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
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
        var grunnlag = aktivitetspengerGrunnlagRepository.hentGrunnlag(behandling.getId());
        if (grunnlag.isPresent()) {
            LocalDateTimeline<AktivitetspengerSatser> satsTidslinje = grunnlag.get().hentAktivitetspengerSatsTidslinje().intersection(detaljertResultat);
            var satsSegments = satsTidslinje.toSegments();
            LocalDateSegment<AktivitetspengerSatser> previous = null;
            for (LocalDateSegment<AktivitetspengerSatser> current : satsSegments) {
                if (previous == null) {
                    previous = current;
                    continue;
                }
                if (current.getValue().satsGrunnlag().antallBarn() < previous.getValue().satsGrunnlag().antallBarn()) {
                    return true;
                }
                previous = current;
            }
        }
        return false;
    }

}
