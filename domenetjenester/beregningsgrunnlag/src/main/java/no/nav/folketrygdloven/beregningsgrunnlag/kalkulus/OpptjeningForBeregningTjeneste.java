package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurderingBeregning;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;

@ApplicationScoped
public class OpptjeningForBeregningTjeneste {

    private final OpptjeningAktivitetVurderingBeregning vurderOpptjening = new OpptjeningAktivitetVurderingBeregning();
    private OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste;

    public OpptjeningForBeregningTjeneste() {
        // For CDI
    }

    @Inject
    public OpptjeningForBeregningTjeneste(OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste) {
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
    }


    List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningsaktiviteterForBeregningFrisinn(BehandlingReferanse behandlingReferanse,
                                                                                              InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                                              LocalDate stp, LocalDate fomDato) {

        var opptjeningsaktiviteterPerYtelse = new OpptjeningsaktiviteterPerYtelse(behandlingReferanse.getFagsakYtelseType());
        var aktiviteter = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(behandlingReferanse, iayGrunnlag, vurderOpptjening);
        return aktiviteter.stream()
                .filter(oa -> oa.getPeriode().getFomDato().isBefore(stp))
                .filter(oa -> !oa.getPeriode().getTomDato().isBefore(fomDato))
                .filter(oa -> opptjeningsaktiviteterPerYtelse.erRelevantAktivitet(oa.getOpptjeningAktivitetType(), iayGrunnlag))
                .collect(Collectors.toList());
    }

    /**
     * Henter aktiviteter vurdert i opptjening som er relevant for beregning.
     *
     * @param behandlingReferanse Aktuell behandling referanse
     * @param iayGrunnlag         {@link InntektArbeidYtelseGrunnlag}
     * @return {@link OpptjeningsperiodeForSaksbehandling}er
     */
    List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningsaktiviteterForBeregning(BehandlingReferanse behandlingReferanse,
                                                                                              InntektArbeidYtelseGrunnlag iayGrunnlag) {

        Long behandlingId = behandlingReferanse.getId();

        var opptjeningResultat = opptjeningsperioderTjeneste.hentOpptjeningHvisFinnes(behandlingId);
        if (opptjeningResultat.isEmpty()) {
            return Collections.emptyList();
        }
        var opptjening = opptjeningResultat.flatMap(it -> it.finnOpptjening(behandlingReferanse.getSkjæringstidspunktOpptjening())).orElseThrow();

        var opptjeningsaktiviteterPerYtelse = new OpptjeningsaktiviteterPerYtelse(behandlingReferanse.getFagsakYtelseType());
        var aktiviteter = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(behandlingReferanse, iayGrunnlag, vurderOpptjening, opptjening.getOpptjeningPeriode());
        return aktiviteter.stream()
            .filter(oa -> oa.getPeriode().getFomDato().isBefore(behandlingReferanse.getUtledetSkjæringstidspunkt()))
            .filter(oa -> !oa.getPeriode().getTomDato().isBefore(opptjening.getFom()))
            .filter(oa -> opptjeningsaktiviteterPerYtelse.erRelevantAktivitet(oa.getOpptjeningAktivitetType(), iayGrunnlag))
            .collect(Collectors.toList());
    }

    public Optional<OpptjeningAktiviteter> hentOpptjeningForBeregning(BehandlingReferanse ref,
                                                                      InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var opptjeningsPerioder = hentRelevanteOpptjeningsaktiviteterForBeregning(ref, iayGrunnlag).stream()
            .map(this::mapOpptjeningPeriode).collect(Collectors.toList());
        if (opptjeningsPerioder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OpptjeningAktiviteter(opptjeningsPerioder));
    }

    public Optional<OpptjeningAktiviteter> hentOpptjeningForBeregningFrisinn(BehandlingReferanse ref,
                                                                             InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp, LocalDate fom) {
        var opptjeningsPerioder = hentRelevanteOpptjeningsaktiviteterForBeregningFrisinn(ref, iayGrunnlag, stp, fom).stream()
                .map(this::mapOpptjeningPeriode).collect(Collectors.toList());
        if(opptjeningsPerioder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OpptjeningAktiviteter(opptjeningsPerioder));
    }

    public OpptjeningAktiviteter hentEksaktOpptjeningForBeregning(BehandlingReferanse ref,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag) {
        Optional<OpptjeningAktiviteter> opptjeningAktiviteter = hentOpptjeningForBeregning(ref, iayGrunnlag);

        if (opptjeningAktiviteter.isEmpty()) {
            throw new IllegalStateException("Forventer opptjening!!!");
        }
        return opptjeningAktiviteter.get();
    }

    public OpptjeningAktiviteter hentEksaktOpptjeningForBeregningFrisinn(BehandlingReferanse ref,
                                                                         InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp, LocalDate fom) {
        Optional<OpptjeningAktiviteter> opptjeningAktiviteter = hentOpptjeningForBeregningFrisinn(ref, iayGrunnlag, stp, fom);

        if (opptjeningAktiviteter.isEmpty()) {
            throw new IllegalStateException("Forventer opptjening!!!");
        }
        return opptjeningAktiviteter.get();
    }

    private OpptjeningPeriode mapOpptjeningPeriode(OpptjeningsperiodeForSaksbehandling ops) {
        var periode = new Periode(ops.getPeriode().getFomDato(), ops.getPeriode().getTomDato());
        var arbeidsgiver = ops.getArbeidsgiver();
        var orgnummer = arbeidsgiver == null ? null : arbeidsgiver.getOrgnr();
        var aktørId = arbeidsgiver == null ? null : (arbeidsgiver.getAktørId() == null ? null : arbeidsgiver.getAktørId().getId());
        var arbeidsforholdId = Optional.ofNullable(ops.getOpptjeningsnøkkel())
            .flatMap(Opptjeningsnøkkel::getArbeidsforholdRef)
            .orElse(null);
        return OpptjeningAktiviteter.nyPeriode(ops.getOpptjeningAktivitetType(), periode, orgnummer, aktørId, arbeidsforholdId);
    }

}
