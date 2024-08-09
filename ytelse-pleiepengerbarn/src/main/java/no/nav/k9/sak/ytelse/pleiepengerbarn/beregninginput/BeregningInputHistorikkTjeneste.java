package no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@Dependent
public class BeregningInputHistorikkTjeneste {

    private final BeregningPerioderGrunnlagRepository grunnlagRepository;
    private final ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;
    private final HistorikkTjenesteAdapter historikkTjenesteAdapter;


    @Inject
    public BeregningInputHistorikkTjeneste(BeregningPerioderGrunnlagRepository grunnlagRepository,
                                           ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag,
                                           HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.grunnlagRepository = grunnlagRepository;
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }


    public void lagHistorikk(Long behandlingId) {
        HistorikkInnslagTekstBuilder tekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        var inputOverstyringPerioder = grunnlagRepository.hentForrigeGrunnlag(behandlingId)
            .map(BeregningsgrunnlagPerioderGrunnlag::getInputOverstyringPerioder)
            .orElse(Collections.emptyList());
        var overstyrtePerioder = grunnlagRepository.hentGrunnlag(behandlingId)
            .map(BeregningsgrunnlagPerioderGrunnlag::getInputOverstyringPerioder)
            .orElse(Collections.emptyList());
        tekstBuilder.medSkjermlenke(SkjermlenkeType.OVERSTYR_INPUT_BEREGNING);
        overstyrtePerioder.forEach(p -> lagHistorikk(p, tekstBuilder, inputOverstyringPerioder));
    }

    private void lagHistorikk(InputOverstyringPeriode p,
                              HistorikkInnslagTekstBuilder tekstBuilder,
                              List<InputOverstyringPeriode> eksisterende) {
        tekstBuilder.medNavnOgGjeldendeFra(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, null, p.getSkjæringstidspunkt());
        var eksisterendeOverstyrtperiode = eksisterende.stream().filter(periode -> periode.getSkjæringstidspunkt().equals(p.getSkjæringstidspunkt()))
            .findFirst();
        p.getAktivitetOverstyringer().forEach(a -> lagAktivitetHistorikk(a, tekstBuilder, eksisterendeOverstyrtperiode));
        tekstBuilder.ferdigstillHistorikkinnslagDel();
    }


    private void lagAktivitetHistorikk(InputAktivitetOverstyring a, HistorikkInnslagTekstBuilder tekstBuilder, Optional<InputOverstyringPeriode> eksisterendeOverstyrtperiode) {
        if (a.getArbeidsgiver() != null) {
            var eksisterende = eksisterendeOverstyrtperiode.stream().flatMap(p -> p.getAktivitetOverstyringer().stream())
                .filter(eksisterendeAktivitet -> eksisterendeAktivitet.getArbeidsgiver().equals(a.getArbeidsgiver()))
                .findFirst();
            String arbeidsforholdInfo = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(
                a.getArbeidsgiver(),
                InternArbeidsforholdRef.nullRef(),
                Collections.emptyList());
            if (a.getInntektPrÅr() != null) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
                    arbeidsforholdInfo,
                    finnFraBeløp(a.getInntektPrÅr(), eksisterende.map(InputAktivitetOverstyring::getInntektPrÅr)),
                    a.getInntektPrÅr().getVerdi());
            }
            if (a.getRefusjonPrÅr() != null) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.NYTT_REFUSJONSKRAV,
                    arbeidsforholdInfo,
                    finnFraBeløp(a.getRefusjonPrÅr(), eksisterende.map(InputAktivitetOverstyring::getRefusjonPrÅr)),
                    a.getRefusjonPrÅr().getVerdi());
            }
            if (a.getOpphørRefusjon() != null) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.OPPHØR_REFUSJON,
                    arbeidsforholdInfo,
                    eksisterende.map(InputAktivitetOverstyring::getOpphørRefusjon).orElse(null),
                    a.getOpphørRefusjon());
            }
        }
    }

    private BigDecimal finnFraBeløp(Beløp fastsatt, Optional<Beløp> fra) {
        var forrige = fra.map(Beløp::getVerdi).orElse(null);
        return forrige == null || forrige.compareTo(fastsatt.getVerdi()) == 0 ? null : forrige;
    }

}
