package no.nav.k9.sak.ytelse.omsorgspenger.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningsaktiviteterPerYtelse;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurderingBeregning;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerOpptjeningForBeregningTjeneste implements OpptjeningForBeregningTjeneste {

    private final OpptjeningAktivitetVurderingBeregning vurderOpptjening = new OpptjeningAktivitetVurderingBeregning();
    private OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste;

    private OpptjeningsaktiviteterPerYtelse opptjeningsaktiviteter = new OpptjeningsaktiviteterPerYtelse(Set.of(
        OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
        OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
        OpptjeningAktivitetType.ARBEIDSAVKLARING));

    protected OmsorgspengerOpptjeningForBeregningTjeneste() {
        // For proxy
    }

    @Inject
    public OmsorgspengerOpptjeningForBeregningTjeneste(OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste) {
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
    }


    /**
     * Henter aktiviteter vurdert i opptjening som er relevant for beregning.
     *
     * @param behandlingReferanse Aktuell behandling referanse
     * @param iayGrunnlag         {@link InntektArbeidYtelseGrunnlag}
     * @return {@link OpptjeningsperiodeForSaksbehandling}er
     */
    private List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningsaktiviteterForBeregning(BehandlingReferanse behandlingReferanse,
                                                                                              InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                                              LocalDate stp) {

        Long behandlingId = behandlingReferanse.getId();

        var opptjeningResultat = opptjeningsperioderTjeneste.hentOpptjeningHvisFinnes(behandlingId);
        if (opptjeningResultat.isEmpty()) {
            return Collections.emptyList();
        }
        var opptjening = opptjeningResultat.flatMap(it -> it.finnOpptjening(behandlingReferanse.getSkjæringstidspunktOpptjening())).orElseThrow();

        var aktiviteter = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(behandlingReferanse, iayGrunnlag, vurderOpptjening, opptjening.getOpptjeningPeriode(), finnOppgittOpptjening(iayGrunnlag).orElse(null));
        return aktiviteter.stream()
            .filter(oa -> oa.getPeriode().getFomDato().isBefore(stp))
            .filter(oa -> !oa.getPeriode().getTomDato().isBefore(opptjening.getFom()))
            .filter(oa -> opptjeningsaktiviteter.erRelevantAktivitet(oa.getOpptjeningAktivitetType(), iayGrunnlag))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return iayGrunnlag.getOverstyrtOppgittOpptjening().isPresent() ? iayGrunnlag.getOverstyrtOppgittOpptjening() : iayGrunnlag.getOppgittOpptjening();
    }

    private Optional<OpptjeningAktiviteter> hentOpptjeningForBeregning(BehandlingReferanse ref,
                                                                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                      LocalDate stp) {
        var opptjeningsPerioder = hentRelevanteOpptjeningsaktiviteterForBeregning(ref, iayGrunnlag, stp).stream()
            .map(this::mapOpptjeningPeriode).collect(Collectors.toList());
        if (opptjeningsPerioder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OpptjeningAktiviteter(opptjeningsPerioder));
    }

    @Override
    public OpptjeningAktiviteter hentEksaktOpptjeningForBeregning(BehandlingReferanse ref,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        Optional<OpptjeningAktiviteter> opptjeningAktiviteter = hentOpptjeningForBeregning(ref, iayGrunnlag, vilkårsperiode.getFomDato());

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
