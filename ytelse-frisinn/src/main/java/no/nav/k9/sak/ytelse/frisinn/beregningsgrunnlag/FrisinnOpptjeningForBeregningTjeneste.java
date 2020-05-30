package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.frisinn.filter.OppgittOpptjeningFilter;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnOpptjeningForBeregningTjeneste implements OpptjeningForBeregningTjeneste {
    /** alle må starte et sted. */
    private static final LocalDate THE_FOM = LocalDate.of(2017, 3, 1);

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final OpptjeningAktivitetVurderingBeregning vurderOpptjening = new OpptjeningAktivitetVurderingBeregning();
    private Instance<OpptjeningsperioderUtenOverstyringTjeneste> opptjeningsperioderTjenesteInstanser;

    private final OpptjeningsaktiviteterPerYtelse opptjeningsaktiviteter = new OpptjeningsaktiviteterPerYtelse(Set.of(
        OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
        OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD));

    protected FrisinnOpptjeningForBeregningTjeneste() {
        // For proxy
    }

    @Inject
    public FrisinnOpptjeningForBeregningTjeneste(@Any Instance<OpptjeningsperioderUtenOverstyringTjeneste> opptjeningsperioderTjenesteInstanser) {
        this.opptjeningsperioderTjenesteInstanser = opptjeningsperioderTjenesteInstanser;
    }

    @Override
    public OpptjeningAktiviteter hentEksaktOpptjeningForBeregning(BehandlingReferanse ref,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        LocalDate stp = vilkårsperiode.getFomDato();
        LocalDate fom = THE_FOM;
        OpptjeningAktiviteter opptjeningAktiviteter = hentOpptjeningForBeregning(ref, iayGrunnlag, stp, fom);

        if (opptjeningAktiviteter.getOpptjeningPerioder().isEmpty()) {
            log.debug("Har ikke opptjening.ref={}, stp={}, fom={}", ref, stp, fom);
        }
        return opptjeningAktiviteter;
    }

    @Override
    public Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        OppgittOpptjeningFilter oppgittOpptjeningFilter = new OppgittOpptjeningFilter(iayGrunnlag.getOppgittOpptjening(), iayGrunnlag.getOverstyrtOppgittOpptjening());
        return Optional.ofNullable(oppgittOpptjeningFilter.getOppgittOpptjeningFrisinn());
    }

    private List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningsaktiviteterForBeregningFrisinn(BehandlingReferanse behandlingReferanse,
                                                                                                             InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                                                             LocalDate stp, LocalDate fomDato) {

        Optional<OppgittOpptjening> oppgittOpptjening = finnOppgittOpptjening(iayGrunnlag);
        OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste = FagsakYtelseTypeRef.Lookup.find(OpptjeningsperioderUtenOverstyringTjeneste.class, opptjeningsperioderTjenesteInstanser, behandlingReferanse.getFagsakYtelseType())            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningsperioderUtenOverstyringTjeneste.class.getSimpleName() + " for " + behandlingReferanse.getBehandlingUuid()));
        var aktiviteter = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(behandlingReferanse, iayGrunnlag, vurderOpptjening, DatoIntervallEntitet.fraOgMed(fomDato), oppgittOpptjening.orElse(null));
        return aktiviteter.stream()
            .filter(oa -> oa.getPeriode().getFomDato().isBefore(stp))
            .filter(oa -> !oa.getPeriode().getTomDato().isBefore(fomDato))
            .filter(oa -> opptjeningsaktiviteter.erInkludert(oa.getOpptjeningAktivitetType()))
            .collect(Collectors.toList());
    }

    private OpptjeningAktiviteter hentOpptjeningForBeregning(BehandlingReferanse ref,
                                                                       InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                       LocalDate stp,
                                                                       LocalDate fom) {
        var opptjeningsPerioder = hentRelevanteOpptjeningsaktiviteterForBeregningFrisinn(ref, iayGrunnlag, stp, fom).stream()
            .map(this::mapOpptjeningPeriode).collect(Collectors.toList());
        return new OpptjeningAktiviteter(opptjeningsPerioder);
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
