package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.InntektEndring;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagATFLDtoer;

@ApplicationScoped
public class FastsettBeregningsgrunnlagATFLHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    FastsettBeregningsgrunnlagATFLHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                           ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                           InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             FastsettBeregningsgrunnlagATFLDtoer dto,
                             List<BeregningsgrunnlagEndring> beregningsgrunnlagEndringer) {
        beregningsgrunnlagEndringer.forEach(endring -> lagHistorikkInnslag(param, endring));
        if (historikkAdapter.tekstBuilder().getHistorikkinnslagDeler().size() > 0) {
            historikkAdapter.tekstBuilder()
                .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
                .medSkjermlenke(SkjermlenkeType.BEREGNING);
            historikkAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
        }
    }


    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param,
                                     BeregningsgrunnlagEndring bgEndring) {

        List<BeregningsgrunnlagPrStatusOgAndelEndring> endringer = new ArrayList<>(bgEndring.getBeregningsgrunnlagPeriodeEndringer().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelEndringer());

        oppdaterVedEndretVerdi(param.getBehandlingId(), endringer, bgEndring.getBeregningsgrunnlagPeriodeEndringer().get(0).getPeriode().getFom());
    }

    private void oppdaterVedEndretVerdi(Long behandlingId,
                                        List<BeregningsgrunnlagPrStatusOgAndelEndring> endringer,
                                        LocalDate fom) {

        var arbeidstakerEndringer = endringer.stream()
            .filter(e -> e.getAktivitetStatus().erArbeidstaker())
            .collect(Collectors.toList());

        var frilansEndring = endringer.stream()
            .filter(e -> e.getAktivitetStatus().erFrilanser())
            .findFirst();


        if (frilansEndring.flatMap(BeregningsgrunnlagPrStatusOgAndelEndring::getInntektEndring).isPresent()) {
            InntektEndring endringFL = frilansEndring.flatMap(BeregningsgrunnlagPrStatusOgAndelEndring::getInntektEndring).get();
            historikkAdapter.tekstBuilder()
                .medNavnOgGjeldendeFra(HistorikkEndretFeltType.FRILANS_INNTEKT, null, fom)
                .medEndretFelt(HistorikkEndretFeltType.FRILANS_INNTEKT,
                    endringFL.getFraInntekt(),
                    endringFL.getTilInntekt());
        }

        if (!arbeidstakerEndringer.isEmpty()) {
            oppdaterForOverstyrt(behandlingId, arbeidstakerEndringer, fom);
        }

    }

    private void oppdaterForOverstyrt(Long behandlingId,
                                      List<BeregningsgrunnlagPrStatusOgAndelEndring> overstyrtList, LocalDate fom) {
        var arbeidsforholOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getArbeidsforholdOverstyringer();
        for (var endretAndel : overstyrtList) {
            if (endretAndel != null && endretAndel.getInntektEndring().isPresent()) {
                var visningsNavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                    endretAndel.getAktivitetStatus(),
                    endretAndel.getArbeidsgiver(),
                    Optional.ofNullable(endretAndel.getArbeidsforholdRef()),
                    arbeidsforholOverstyringer);
                BigDecimal fra = endretAndel.getInntektEndring().get().getFraInntekt().setScale(0, RoundingMode.HALF_UP);
                BigDecimal til = endretAndel.getInntektEndring().get().getTilInntekt().setScale(0, RoundingMode.HALF_UP);
                if (fra.compareTo(til) != 0) {
                    historikkAdapter.tekstBuilder()
                        .medNavnOgGjeldendeFra(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, null, fom)
                        .medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, visningsNavn,
                            fra,
                            til);
                }
            }
        }
    }

}
