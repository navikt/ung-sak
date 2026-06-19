package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.SatsEndringUtleder;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class EndringBarnDødsfallStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    @Inject
    public EndringBarnDødsfallStrategy(UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        boolean erDødsfall = harSatsendringenDødsfall(behandling, detaljertResultat)
            || resultater.innholder(DetaljertResultatType.ENDRING_BARN_DØDSFALL);

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
                if (SatsEndringUtleder.bestemSatsendring(current.getValue(), previous.getValue()).dødsfallBarn()) {
                    return true;
                }
                previous = current;
            }
        }
        return false;
    }

}
