package no.nav.folketrygdloven.beregningsgrunnlag.input;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurderingBeregning;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;

@ApplicationScoped
public class OpptjeningForBeregningTjeneste {

    private OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste;

    private final OpptjeningAktivitetVurderingBeregning vurderOpptjening = new OpptjeningAktivitetVurderingBeregning();

    public OpptjeningForBeregningTjeneste() {
        // For CDI
    }

    @Inject
    public OpptjeningForBeregningTjeneste(OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste) {
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
    }

    /**
     * Henter aktiviteter vurdert i opptjening som er relevant for beregning.
     *
     * @param behandlingReferanse Aktuell behandling referanse
     * @param iayGrunnlag {@link InntektArbeidYtelseGrunnlag}
     * @return {@link OpptjeningsperiodeForSaksbehandling}er
     */
    List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningsaktiviteterForBeregning(BehandlingReferanse behandlingReferanse,
                                                                                                     InntektArbeidYtelseGrunnlag iayGrunnlag) {
        
        Long behandlingId = behandlingReferanse.getId();
        
        var opptjening = opptjeningsperioderTjeneste.hentOpptjeningHvisFinnes(behandlingId);
        if(opptjening.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDate opptjeningsperiodeFom = opptjening.map(Opptjening::getFom).orElseThrow();

        var opptjeningsaktiviteterPerYtelse = new OpptjeningsaktiviteterPerYtelse(behandlingReferanse.getFagsakYtelseType());
        var aktiviteter = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(behandlingReferanse, iayGrunnlag, vurderOpptjening);
        return aktiviteter.stream()
            .filter(oa -> oa.getPeriode().getFomDato().isBefore(behandlingReferanse.getUtledetSkjæringstidspunkt()))
            .filter(oa -> !oa.getPeriode().getTomDato().isBefore(opptjeningsperiodeFom))
            .filter(oa -> opptjeningsaktiviteterPerYtelse.erRelevantAktivitet(oa.getOpptjeningAktivitetType(), iayGrunnlag))
            .collect(Collectors.toList());
    }

    public Optional<OpptjeningAktiviteter> hentOpptjeningForBeregning(BehandlingReferanse ref,
                                                                     InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var opptjeningsPerioder = hentRelevanteOpptjeningsaktiviteterForBeregning(ref, iayGrunnlag).stream()
            .map(this::mapOpptjeningPeriode).collect(Collectors.toList());
        if(opptjeningsPerioder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OpptjeningAktiviteter(opptjeningsPerioder));
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
