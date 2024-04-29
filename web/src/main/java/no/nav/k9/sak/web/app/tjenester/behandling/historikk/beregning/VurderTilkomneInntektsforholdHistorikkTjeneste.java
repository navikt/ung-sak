package no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.NyttInntektsforholdEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.VurderTilkomneInntektsforholdDtoer;
import no.nav.k9.sak.perioder.EndretUtbetalingPeriodeutleder;

@ApplicationScoped
public class VurderTilkomneInntektsforholdHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<EndretUtbetalingPeriodeutleder> endretUtbetalingPeriodeutleder;

    VurderTilkomneInntektsforholdHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderTilkomneInntektsforholdHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                          HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                          @Any Instance<EndretUtbetalingPeriodeutleder> endretUtbetalingPeriodeutleder) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.endretUtbetalingPeriodeutleder = endretUtbetalingPeriodeutleder;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             VurderTilkomneInntektsforholdDtoer dto,
                             List<OppdaterBeregningsgrunnlagResultat> beregningsgrunnlagEndringer) {
        var arbeidsforholOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId())
            .getArbeidsforholdOverstyringer();
        var behandlingReferanse = param.getRef();
        var endretUtbetalingPeriodeutleder = EndretUtbetalingPeriodeutleder.finnUtleder(this.endretUtbetalingPeriodeutleder, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
        var perioder = endretUtbetalingPeriodeutleder.utledPerioder(behandlingReferanse);
        for (var endringer : beregningsgrunnlagEndringer) {
            if (endringer.getBeregningsgrunnlagEndring().isPresent()) {
                var periodeEndringer = endringer.getBeregningsgrunnlagEndring().get().getBeregningsgrunnlagPeriodeEndringer()
                    .stream()
                    .filter(p -> perioder.stream().anyMatch(vp -> vp.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(p.getPeriode().getFom(), p.getPeriode().getTom()))))
                    .toList();
                var nyttInntektsforholdEndringer = periodeEndringer.stream().flatMap(p -> p.getNyttInntektsforholdEndringer().stream())
                    .collect(Collectors.toSet());
                LocalDate skjæringstidspunkt = endringer.getSkjæringstidspunkt();
                historikkTjenesteAdapter.tekstBuilder().medNavnOgGjeldendeFra(HistorikkEndretFeltType.VURDER_NYTT_INNTEKTSFORHOLD, null, skjæringstidspunkt);
                nyttInntektsforholdEndringer
                    .forEach(e -> lagHistorikkForVurderingAvReduksjon(
                        historikkTjenesteAdapter.tekstBuilder(),
                        e,
                        arbeidsforholOverstyringer));
            }
        }
        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING);
    }

    private void lagHistorikkForVurderingAvReduksjon(HistorikkInnslagTekstBuilder historikkBuilder,
                                                     NyttInntektsforholdEndring e,
                                                     List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {

        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
            e.getAktivitetStatus(),
            Optional.ofNullable(e.getArbeidsgiver()),
            Optional.empty(),
            arbeidsforholdOverstyringer);
        historikkBuilder.medEndretFelt(HistorikkEndretFeltType.VURDER_NYTT_INNTEKTSFORHOLD,
            arbeidsforholdInfo,
            e.getSkalRedusereUtbetalingEndring().erEndret() ? e.getSkalRedusereUtbetalingEndring().getFraVerdiEllerNull() : null,
            e.getSkalRedusereUtbetalingEndring().getTilVerdi());
        var bruttoInntektPrÅrEndring = e.getBruttoInntektPrÅrEndring();
        if (bruttoInntektPrÅrEndring != null && bruttoInntektPrÅrEndring.getTilBeløp() != null) {
            var fraBeløpPrMnd = bruttoInntektPrÅrEndring.getFraBeløp().orElse(null);
            var tilBeløpPrMnd = bruttoInntektPrÅrEndring.getTilBeløp();
            historikkBuilder.medEndretFelt(HistorikkEndretFeltType.BRUTTO_INNTEKT_NYTT_INNTEKTSFORHOLD,
                arbeidsforholdInfo,
                fraBeløpPrMnd == null || tilBeløpPrMnd.compareTo(fraBeløpPrMnd) == 0 ? null : fraBeløpPrMnd,
                tilBeløpPrMnd);
        }
    }

}
