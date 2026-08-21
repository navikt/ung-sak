package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
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
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        var detaljertResultat = resultatTidslinje.tilVurdering();
        if (enableAutoBrevVedBarnDødsfall) {
            return List.of();
        }

        boolean erDødsfall = harSatsendringenDødsfall(behandling, detaljertResultat)
            || resultatTidslinje.harÅrsak(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);
        if (erDødsfall) {
            return List.of(VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_IMPLEMENTERT, "Ingen brev ved dødsfall av barn."));
        }
        return List.of();

    }

    @Override
    public Presedens presedens() {
        return Presedens.OVERSTYRENDE_INGEN_BREV;
    }

    private boolean harSatsendringenDødsfall(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var grunnlag = aktivitetspengerGrunnlagRepository.hentGrunnlag(behandling.getId());
        if (grunnlag.isPresent()) {
            LocalDateTimeline<AktivitetspengerSatser> satsTidslinje = grunnlag.get().hentAktivitetspengerSatsTidslinje()
                .intersection(detaljertResultat);
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
