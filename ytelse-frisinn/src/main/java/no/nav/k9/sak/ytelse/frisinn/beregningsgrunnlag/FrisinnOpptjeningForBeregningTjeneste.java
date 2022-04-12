package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningsaktiviteterPerYtelse;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurderingBeregning;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.ytelse.OpptjeningsperioderTjenesteFRISINN;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.frisinn.filter.OppgittOpptjeningFilter;

@FagsakYtelseTypeRef(FRISINN)
@ApplicationScoped
public class FrisinnOpptjeningForBeregningTjeneste implements OpptjeningForBeregningTjeneste {
    /**
     * alle må starte et sted.
     */
    private static final LocalDate THE_FOM = LocalDate.of(2017, 3, 1);

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final OpptjeningAktivitetVurderingBeregning vurderOpptjening = new OpptjeningAktivitetVurderingBeregning();
    private final OpptjeningsaktiviteterPerYtelse opptjeningsaktiviteter = new OpptjeningsaktiviteterPerYtelse(Set.of(
        OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
        OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD));
    private OpptjeningsperioderTjenesteFRISINN opptjeningsperioderTjeneste;
    private boolean nyttStpToggle;

    protected FrisinnOpptjeningForBeregningTjeneste() {
        // For proxy
    }

    @Inject
    public FrisinnOpptjeningForBeregningTjeneste(OpptjeningsperioderTjenesteFRISINN opptjeningsperioderTjeneste,
                                                 @KonfigVerdi(value = "FRISINN_NYTT_STP_TOGGLE", defaultVerdi = "false", required = false) boolean nyttStpToggle) {
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
        this.nyttStpToggle = nyttStpToggle;
    }

    @Override
    public Optional<OpptjeningAktiviteter> hentEksaktOpptjeningForBeregning(BehandlingReferanse ref,
                                                                            InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        LocalDate stp = vilkårsperiode.getFomDato();
        LocalDate fom = THE_FOM;
        OpptjeningAktiviteter opptjeningAktiviteter = hentOpptjeningForBeregning(ref, iayGrunnlag, stp, fom);

        if (opptjeningAktiviteter.getOpptjeningPerioder().isEmpty()) {
            log.debug("Har ikke opptjening.ref={}, stp={}, fom={}", ref, stp, fom);
        }
        return Optional.of(opptjeningAktiviteter);
    }

    @Override
    public Optional<OppgittOpptjening> finnOppgittOpptjening(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        OppgittOpptjeningFilter oppgittOpptjeningFilter = new OppgittOpptjeningFilter(iayGrunnlag.getOppgittOpptjening(), iayGrunnlag.getOverstyrtOppgittOpptjening(), nyttStpToggle);
        return Optional.ofNullable(oppgittOpptjeningFilter.getOppgittOpptjeningFrisinn(stp));
    }

    private List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningsaktiviteterForBeregningFrisinn(BehandlingReferanse behandlingReferanse,
                                                                                                             InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                                                             LocalDate stp, LocalDate fomDato) {
        var oppgittOpptjening = finnOppgittOpptjening(behandlingReferanse, iayGrunnlag, stp).orElse(null);
        if (oppgittOpptjening == null) {
            log.warn("Fant ingen oppgitt opptjening for opptjeningsaktiviteter for stp={}", stp);
            return List.of();
        }

        var aktiviteter = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(behandlingReferanse, iayGrunnlag, oppgittOpptjening, vurderOpptjening, DatoIntervallEntitet.fraOgMed(fomDato));
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
