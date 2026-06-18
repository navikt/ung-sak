package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;

import java.util.List;

@ApplicationScoped
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
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        if (enableAutoBrevVedBarnDødsfall) {
            return List.of();
        }

        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        boolean erDødsfall = harSatsendringenDødsfall(behandling, detaljertResultat)
            || resultater.innholder(DetaljertResultatType.ENDRING_BARN_DØDSFALL);
        if (!erDødsfall) {
            return List.of();
        }

        return List.of(VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_IMPLEMENTERT, "Ingen brev ved dødsfall av barn."));
    }

    @Override
    public Presedens presedens() {
        return Presedens.OVERSTYRENDE_INGEN_BREV;
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
