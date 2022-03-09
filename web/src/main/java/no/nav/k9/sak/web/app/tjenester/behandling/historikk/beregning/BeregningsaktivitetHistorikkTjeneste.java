package no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningAktivitetEndring;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class BeregningsaktivitetHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningsaktivitetHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    BeregningsaktivitetHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                         InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikkForSkjæringstidspunkt(Long behandlingId,
                                                  HistorikkInnslagTekstBuilder tekstBuilder,
                                                  List<BeregningAktivitetEndring> beregningAktivitetEndringer,
                                                  LocalDate gjeldendeFra, String begrunnelse) {
        tekstBuilder.medBegrunnelse(begrunnelse).medNavnOgGjeldendeFra(HistorikkEndretFeltType.AKTIVITET, null, gjeldendeFra);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        beregningAktivitetEndringer.forEach(aktivitetEndring -> {
            var aktivitetnavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningaktivitet(
                aktivitetEndring.getAktivitetNøkkel().getArbeidsgiver(),
                aktivitetEndring.getAktivitetNøkkel().getArbeidsforholdRef(),
                aktivitetEndring.getAktivitetNøkkel().getOpptjeningAktivitetType(),
                arbeidsforholdOverstyringer);
            lagSkalBrukesHistorikk(tekstBuilder, aktivitetEndring, aktivitetnavn);
            lagPeriodeHistorikk(tekstBuilder, aktivitetEndring, aktivitetnavn);
        });
    }

    private void lagSkalBrukesHistorikk(HistorikkInnslagTekstBuilder tekstBuilder,
                                        BeregningAktivitetEndring aktivitetEndring, String aktivitetnavn) {
        var skalBrukesEndring = aktivitetEndring.getSkalBrukesEndring();
        if (skalBrukesEndring != null) {
            var skalBrukesTilVerdi = skalBrukesEndring.getTilVerdi() ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
            var skalBrukesFraVerdi = skalBrukesEndring.getFraVerdi().map(v -> v ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT).orElse(null);
            if (skalBrukesEndring.erEndret()) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.AKTIVITET, aktivitetnavn, skalBrukesFraVerdi, skalBrukesTilVerdi);
            }
        }
    }

    private void lagPeriodeHistorikk(HistorikkInnslagTekstBuilder tekstBuilder,
                                     BeregningAktivitetEndring aktivitetEndring, String aktivitetnavn) {
        if (aktivitetEndring.getTomDatoEndring() != null) {
            var tomDatoEndring = aktivitetEndring.getTomDatoEndring();
            if (tomDatoEndring.erEndret()) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.PERIODE_TOM,
                    tomDatoEndring.getFraVerdi(), tomDatoEndring.getTilVerdi());
                tekstBuilder.medTema(HistorikkEndretFeltType.AKTIVITET, aktivitetnavn);
            }
        }

    }

}
