package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static no.nav.folketrygdloven.beregningsgrunnlag.FastsettBeregningsgrunnlagPerioderTjeneste.MÅNEDER_I_1_ÅR;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningInntektsmeldingTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.MatchBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FordelBeregningsgrunnlagAndelDto;

@ApplicationScoped
public class FordelBeregningsgrunnlagAndelDtoTjeneste {

    private BeregningsgrunnlagDtoUtil dtoUtil;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste;

    FordelBeregningsgrunnlagAndelDtoTjeneste() {
        // Hibernate
    }

    @Inject
    public FordelBeregningsgrunnlagAndelDtoTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                                    BeregningsgrunnlagDtoUtil dtoUtil,
                                                    FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste) {
        this.dtoUtil = dtoUtil;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.refusjonOgGraderingTjeneste = refusjonOgGraderingTjeneste;
    }


    // TODO (TFP-559) Denne er klar for ein refaktorering etter at migering er kjørt for migrering til fordel-steget
    List<FordelBeregningsgrunnlagAndelDto> lagEndretBgAndelListe(BeregningsgrunnlagInput input,
                                                                 BeregningsgrunnlagPeriode periode) {
        var ref = input.getBehandlingReferanse();
        List<FordelBeregningsgrunnlagAndelDto> endringAndeler = new ArrayList<>();
        for (BeregningsgrunnlagPrStatusOgAndel andel : periode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            FordelBeregningsgrunnlagAndelDto endringAndel = lagEndretBGAndel(ref, andel, input.getInntektsmeldinger(), input.getAktivitetGradering(), input.getIayGrunnlag());
            RefusjonDtoTjeneste.settRefusjonskrav(andel, periode.getPeriode(), endringAndel, input.getInntektsmeldinger());
            var beregningAktivitetAggregat = input.getBeregningsgrunnlagGrunnlag().getGjeldendeAktiviteter();
            endringAndel.setNyttArbeidsforhold(refusjonOgGraderingTjeneste.erNyttArbeidsforhold(andel, beregningAktivitetAggregat));
            endringAndel.setArbeidsforholdType(andel.getArbeidsforholdType());
            endringAndeler.add(endringAndel);
        }
        return endringAndeler;
    }

    private FordelBeregningsgrunnlagAndelDto lagEndretBGAndel(BehandlingReferanse ref,
                                                              BeregningsgrunnlagPrStatusOgAndel andel,
                                                              Collection<Inntektsmelding>inntektsmeldinger,
                                                              AktivitetGradering aktivitetGradering,
                                                              InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        FordelBeregningsgrunnlagAndelDto endringAndel = new FordelBeregningsgrunnlagAndelDto(dtoUtil.lagFaktaOmBeregningAndel(
            andel,
            aktivitetGradering,
            inntektArbeidYtelseGrunnlag
        ));
        settFordelingForrigeBehandling(ref, andel, endringAndel);
        endringAndel.setFordeltPrAar(andel.getFordeltPrÅr());
        settBeløpFraInntektsmelding(andel, inntektsmeldinger, endringAndel);
        return endringAndel;
    }

    private void settBeløpFraInntektsmelding(BeregningsgrunnlagPrStatusOgAndel andel,
                                             Collection<Inntektsmelding>inntektsmeldinger,
                                             FordelBeregningsgrunnlagAndelDto endringAndel) {
        Optional<Inntektsmelding> inntektsmeldingOpt = BeregningInntektsmeldingTjeneste.finnInntektsmeldingForAndel(andel, inntektsmeldinger);
        inntektsmeldingOpt.ifPresent(im -> endringAndel.setBelopFraInntektsmelding(im.getInntektBeløp().getVerdi()));
    }

    private void settFordelingForrigeBehandling(BehandlingReferanse ref, BeregningsgrunnlagPrStatusOgAndel andel,
                                                FordelBeregningsgrunnlagAndelDto endringAndel) {
        if (andel.getLagtTilAvSaksbehandler()) {
            endringAndel.setFordelingForrigeBehandling(null);
            return;
        }
        if (ref.erRevurdering()) {
            Long originalBehandlingId = ref.getOriginalBehandlingId()
                .orElseThrow(() -> new IllegalStateException("Fant ikke original behandling for behandling " + ref.getBehandlingId().toString()));
            Optional<BeregningsgrunnlagEntitet> bgForrigeBehandling = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(originalBehandlingId);
            if (!bgForrigeBehandling.isPresent()) {
                endringAndel.setFordelingForrigeBehandling(BigDecimal.ZERO);
                return;
            }
            BeregningsgrunnlagPeriode periodeIOriginaltGrunnlag = MatchBeregningsgrunnlagTjeneste.finnPeriodeIBeregningsgrunnlag(andel.getBeregningsgrunnlagPeriode(),
                bgForrigeBehandling.get());
            BigDecimal fastsattForrigeBehandling = periodeIOriginaltGrunnlag.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(a -> a.matchUtenInntektskategori(andel))
                .map(BeregningsgrunnlagPrStatusOgAndel::getBeregnetPrÅr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
            if (fastsattForrigeBehandling != null) {
                endringAndel.setFordelingForrigeBehandling(fastsattForrigeBehandling.divide(BigDecimal.valueOf(MÅNEDER_I_1_ÅR), 0, RoundingMode.HALF_UP));
            }
        }
    }
}
