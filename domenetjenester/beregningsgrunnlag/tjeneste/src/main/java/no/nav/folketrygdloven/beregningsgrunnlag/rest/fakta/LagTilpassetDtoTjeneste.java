package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagPrStatusOgAndelATDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagPrStatusOgAndelFLDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagPrStatusOgAndelSNDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagPrStatusOgAndelYtelseDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.EgenNæringDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.PgiDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;

class LagTilpassetDtoTjeneste  {

    private static final BigDecimal MND_I_1_ÅR = BigDecimal.valueOf(12);
    private static final BigDecimal DAGPENGER_FAKTOR = BigDecimal.valueOf(62.4);
    private static final BigDecimal AAP_FAKTOR = BigDecimal.valueOf(66);
    private static final BigDecimal HUNDRE = BigDecimal.valueOf(100);

    private LagTilpassetDtoTjeneste() {
    }

    static BeregningsgrunnlagPrStatusOgAndelDto opprettTilpassetDTO(BehandlingReferanse ref,
                                                                    BeregningsgrunnlagPrStatusOgAndel andel,
                                                                    Optional<BeregningsgrunnlagPrStatusOgAndel> faktaOmBeregningAndel,
                                                                    InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        if (AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE.equals(andel.getAktivitetStatus())) {
            return opprettSNDto(andel, inntektArbeidYtelseGrunnlag);
        } else if (AktivitetStatus.ARBEIDSTAKER.equals(andel.getAktivitetStatus())
            && andel.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr).isPresent()) {
            return opprettATDto(andel);

        } else if (AktivitetStatus.FRILANSER.equals(andel.getAktivitetStatus())) {
            return opprettFLDto(andel, faktaOmBeregningAndel);
        } else if (AktivitetStatus.DAGPENGER.equals(andel.getAktivitetStatus()) || AktivitetStatus.ARBEIDSAVKLARINGSPENGER.equals(andel.getAktivitetStatus())) {
            return opprettYtelseDto(ref, inntektArbeidYtelseGrunnlag, andel);
        } else {
            return new BeregningsgrunnlagPrStatusOgAndelDto();
        }
    }

    private static BeregningsgrunnlagPrStatusOgAndelDto opprettSNDto(BeregningsgrunnlagPrStatusOgAndel andel, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        //Merk, PGI verdier ligger i kronologisk synkende rekkefølge og er pgi fra årene i beregningsperioden
        BeregningsgrunnlagPrStatusOgAndelSNDto dtoSN = new BeregningsgrunnlagPrStatusOgAndelSNDto();

        List<OppgittEgenNæring> egneNæringer = inntektArbeidYtelseGrunnlag.getOppgittOpptjening()
            .map(OppgittOpptjening::getEgenNæring)
            .orElse(Collections.emptyList());

        dtoSN.setPgiSnitt(andel.getPgiSnitt());

        List<EgenNæringDto> næringer = egneNæringer.stream().map(EgenNæringMapper::map).collect(Collectors.toList());
        dtoSN.setNæringer(næringer);

        List<PgiDto> pgiDtoer = lagPgiDto(andel);
        dtoSN.setPgiVerdier(pgiDtoer);

        return dtoSN;
    }

    private static List<PgiDto> lagPgiDto(BeregningsgrunnlagPrStatusOgAndel andel) {
        LocalDate beregningsperiodeTom = andel.getBeregningsperiodeTom();
        if (beregningsperiodeTom == null) {
            return Collections.emptyList();
        }
        List<PgiDto> liste = new ArrayList<>();
        liste.add(new PgiDto(andel.getPgi1(), beregningsperiodeTom.getYear()));
        liste.add(new PgiDto(andel.getPgi2(), beregningsperiodeTom.minusYears(1).getYear()));
        liste.add(new PgiDto(andel.getPgi3(), beregningsperiodeTom.minusYears(2).getYear()));
        return liste;
    }

    private static BeregningsgrunnlagPrStatusOgAndelDto opprettATDto(BeregningsgrunnlagPrStatusOgAndel andel) {
        BeregningsgrunnlagPrStatusOgAndelATDto dtoAT = new BeregningsgrunnlagPrStatusOgAndelATDto();
        dtoAT.setBortfaltNaturalytelse(andel.getBgAndelArbeidsforhold().orElseThrow().getNaturalytelseBortfaltPrÅr().orElseThrow());
        return dtoAT;
    }


    private static BeregningsgrunnlagPrStatusOgAndelFLDto opprettFLDto(BeregningsgrunnlagPrStatusOgAndel andel,
                                                               Optional<BeregningsgrunnlagPrStatusOgAndel> faktaOmBeregningAndel) {
        BeregningsgrunnlagPrStatusOgAndelFLDto dtoFL = new BeregningsgrunnlagPrStatusOgAndelFLDto();
        faktaOmBeregningAndel.ifPresentOrElse(a -> dtoFL.setErNyoppstartet(a.erNyoppstartet().orElse(null)),
            () -> dtoFL.setErNyoppstartet(andel.erNyoppstartet().orElse(null)));
        return dtoFL;
    }

    private static BeregningsgrunnlagPrStatusOgAndelYtelseDto opprettYtelseDto(BehandlingReferanse ref,
                                                                               InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                                                               BeregningsgrunnlagPrStatusOgAndel andel) {
        BeregningsgrunnlagPrStatusOgAndelYtelseDto dtoYtelse = new BeregningsgrunnlagPrStatusOgAndelYtelseDto();
        Optional<BigDecimal> årsbeløpFraMeldekort = FinnInntektFraYtelse.finnÅrbeløpFraMeldekort(ref, andel.getAktivitetStatus(), inntektArbeidYtelseGrunnlag);
        if (!årsbeløpFraMeldekort.isPresent()) {
            return dtoYtelse;
        }
        dtoYtelse.setBelopFraMeldekortPrAar(årsbeløpFraMeldekort.get());
        dtoYtelse.setBelopFraMeldekortPrMnd(årsbeløpFraMeldekort.get().divide(MND_I_1_ÅR, 10, RoundingMode.HALF_UP));
        if (andel.getBruttoPrÅr() != null) {
            dtoYtelse.setOppjustertGrunnlag(finnVisningstallForOppjustertGrunnlag(andel.getAktivitetStatus(), andel.getBruttoPrÅr()));
        }
        return dtoYtelse;
    }

    private static BigDecimal finnVisningstallForOppjustertGrunnlag(AktivitetStatus aktivitetStatus, BigDecimal bruttoPrÅr) {
        if (AktivitetStatus.DAGPENGER.equals(aktivitetStatus)) {
            return oppjustertDagpengesats(bruttoPrÅr);
        } else {
            return oppjustertAAPSats(bruttoPrÅr);
        }
    }

    private static BigDecimal oppjustertAAPSats(BigDecimal bruttoPrÅr) {
        BigDecimal mellomregning = bruttoPrÅr.divide(AAP_FAKTOR, 0, RoundingMode.HALF_EVEN);
        return mellomregning.multiply(HUNDRE);
    }

    private static BigDecimal oppjustertDagpengesats(BigDecimal bruttoPrÅr) {
        BigDecimal mellomregning = bruttoPrÅr.divide(DAGPENGER_FAKTOR, 0, RoundingMode.HALF_EVEN);
        return mellomregning.multiply(HUNDRE);
    }
}
