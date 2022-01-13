package no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.RefusjonoverstyringPeriodeEndring;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.refusjon.VurderRefusjonBeregningsgrunnlagDtoer;

@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagHistorikkTjeneste {
    private static final BigDecimal MÅNEDER_I_ÅR = BigDecimal.valueOf(12);

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    VurderRefusjonBeregningsgrunnlagHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                             HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             VurderRefusjonBeregningsgrunnlagDtoer dto,
                             List<OppdaterBeregningsgrunnlagResultat> beregningsgrunnlagEndringer) {
        var arbeidsforholOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId())
            .getArbeidsforholdOverstyringer();
        for (var endringer : beregningsgrunnlagEndringer) {
            if (endringer.getRefusjonoverstyringEndring().isPresent()) {
                LocalDate skjæringstidspunkt = endringer.getSkjæringstidspunkt();
                historikkTjenesteAdapter.tekstBuilder().medNavnOgGjeldendeFra(HistorikkEndretFeltType.NY_STARTDATO_REFUSJON, null, skjæringstidspunkt);
                endringer.getRefusjonoverstyringEndring().get().getRefusjonperiodeEndringer()
                    .forEach(e -> lagHistorikkForRefusjonEndring(
                        historikkTjenesteAdapter.tekstBuilder(),
                        e,
                        arbeidsforholOverstyringer));
            }
        }
        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING);
    }

    private void lagHistorikkForRefusjonEndring(HistorikkInnslagTekstBuilder historikkBuilder,
                                                RefusjonoverstyringPeriodeEndring refusjonEndringForArbeidsgiver,
                                                List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {

        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
            AktivitetStatus.ARBEIDSTAKER,
            Optional.of(refusjonEndringForArbeidsgiver.getArbeidsgiver()),
            Optional.of(refusjonEndringForArbeidsgiver.getArbeidsforholdRef()),
            arbeidsforholdOverstyringer);
        historikkBuilder.medEndretFelt(HistorikkEndretFeltType.NY_STARTDATO_REFUSJON,
            arbeidsforholdInfo,
            refusjonEndringForArbeidsgiver.getFastsattRefusjonFomEndring().getFraVerdi(),
            refusjonEndringForArbeidsgiver.getFastsattRefusjonFomEndring().getTilVerdi());
        var refusjonFørDatoEndring = refusjonEndringForArbeidsgiver.getFastsattDelvisRefusjonFørDatoEndring();
        if (refusjonFørDatoEndring != null
            && refusjonFørDatoEndring.getTilBeløp().compareTo(BigDecimal.ZERO) != 0) {
            var fraBeløpPrMnd = refusjonFørDatoEndring.getFraBeløp().orElse(null);
            var tilBeløpPrMnd = refusjonFørDatoEndring.getTilBeløp();
            historikkBuilder.medEndretFelt(HistorikkEndretFeltType.DELVIS_REFUSJON_FØR_STARTDATO,
                arbeidsforholdInfo,
                fraBeløpPrMnd, tilBeløpPrMnd);
        }
    }

}
