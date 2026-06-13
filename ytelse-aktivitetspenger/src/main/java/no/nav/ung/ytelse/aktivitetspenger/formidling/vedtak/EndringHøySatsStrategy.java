package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.AktivitetspengerSatsType;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringHøySatsInnholdBygger;

import java.util.List;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class EndringHøySatsStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringHøySatsInnholdBygger endringHøySatsInnholdBygger;
    private final AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;

    @Inject
    public EndringHøySatsStrategy(EndringHøySatsInnholdBygger endringHøySatsInnholdBygger, AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository) {
        this.endringHøySatsInnholdBygger = endringHøySatsInnholdBygger;
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        if (!resultater.innholder(DetaljertResultatType.ENDRING_ØKT_SATS)) {
            return List.of();
        }

        var satstidslinje = aktivitetspengerGrunnlagRepository.hentGrunnlag(behandling.getId())
            .map(AktivitetspengerGrunnlag::hentAktivitetspengerSatsTidslinje)
            .map(it -> it.intersection(detaljertResultat));

        if (satstidslinje.isEmpty()) {
            return List.of(VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT,
                "Har ikke tidligere beregnet sats"));
        } else {
            boolean allePerioderErBeregningsgrunnlag = satstidslinje.get().toSegments().stream()
                .allMatch(s -> s.getValue().hentSatsType() == AktivitetspengerSatsType.BEREGNINGSGRUNNLAG);
            if (allePerioderErBeregningsgrunnlag) {
                return List.of(VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT,
                    "Alle perioder har beregningsgrunnlag som sats, endring høy sats-brev er ikke relevant."));
            }
        }
        return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.ENDRING_HØY_SATS, endringHøySatsInnholdBygger, "Automatisk brev ved endring til høy sats."));
    }
}
