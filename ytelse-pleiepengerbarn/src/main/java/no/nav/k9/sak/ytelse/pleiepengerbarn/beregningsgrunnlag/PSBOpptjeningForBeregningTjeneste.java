package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningsaktiviteterPerYtelse;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetResultatVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Periode;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class PSBOpptjeningForBeregningTjeneste implements OpptjeningForBeregningTjeneste {

    private OpptjeningsperioderTjeneste opptjeningsperioderTjeneste;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;
    private VilkårResultatRepository vilkårResultatRepository;

    private OpptjeningsaktiviteterPerYtelse opptjeningsaktiviteter = new OpptjeningsaktiviteterPerYtelse(Set.of(
        OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
        OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
        OpptjeningAktivitetType.ARBEIDSAVKLARING));

    protected PSBOpptjeningForBeregningTjeneste() {
        // For proxy
    }

    @Inject
    public PSBOpptjeningForBeregningTjeneste(OpptjeningsperioderTjeneste opptjeningsperioderTjeneste,
                                             OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider,
                                             VilkårResultatRepository vilkårResultatRepository) {
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
        this.oppgittOpptjeningFilterProvider = oppgittOpptjeningFilterProvider;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }


    /**
     * Henter aktiviteter vurdert i opptjening som er relevant for beregning.
     *
     * @param behandlingReferanse Aktuell behandling referanse
     * @param iayGrunnlag         {@link InntektArbeidYtelseGrunnlag}
     * @param vilkårsperiode
     * @return {@link OpptjeningsperiodeForSaksbehandling}er
     */
    private List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningsaktiviteterForBeregning(BehandlingReferanse behandlingReferanse,
                                                                                                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                                                      DatoIntervallEntitet vilkårsperiode) {

        Long behandlingId = behandlingReferanse.getId();
        var opptjeningsvilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET);
        var vilkårUtfallMerknad = opptjeningsvilkår.map(v -> v.finnPeriodeForSkjæringstidspunkt(vilkårsperiode.getFomDato()))
            .map(VilkårPeriode::getMerknad)
            .orElse(null);
        var opptjeningResultat = opptjeningsperioderTjeneste.hentOpptjeningHvisFinnes(behandlingId);
        if (opptjeningResultat.isEmpty()) {
            return Collections.emptyList();
        }
        var opptjening = opptjeningResultat.flatMap(it -> it.finnOpptjening(vilkårsperiode.getFomDato())).orElseThrow();
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()));
        List<OpptjeningsperiodeForSaksbehandling> opptjeningAktivitetPerioder = mapPerioder(behandlingReferanse, iayGrunnlag, vilkårsperiode, opptjeningResultat, opptjening, yrkesaktivitetFilter);

        return opptjeningAktivitetPerioder.stream()
            .filter(oa -> filtrerForVilkårsperiode(vilkårsperiode, oa, vilkårUtfallMerknad))
            .filter(oa -> !oa.getPeriode().getTomDato().isBefore(opptjening.getFom()))
            .filter(oa -> opptjeningsaktiviteter.erRelevantAktivitet(oa.getOpptjeningAktivitetType()))
            .filter(oa -> oa.getVurderingsStatus().equals(VurderingsStatus.GODKJENT))
            .collect(Collectors.toList());
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapPerioder(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode, Optional<OpptjeningResultat> opptjeningResultat, Opptjening opptjening, YrkesaktivitetFilter yrkesaktivitetFilter) {
        var aktiviteter = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(behandlingReferanse,
            iayGrunnlag,
            new OpptjeningAktivitetResultatVurdering(opptjeningResultat.get()),
            opptjening.getOpptjeningPeriode(),
            vilkårsperiode,
            yrkesaktivitetFilter);

        return slåSammenLikeVurderinger(aktiviteter);
    }

    private static List<OpptjeningsperiodeForSaksbehandling> slåSammenLikeVurderinger(List<OpptjeningsperiodeForSaksbehandling> aktiviteter) {
        var gruppertPåNøkkel = aktiviteter.stream().collect(Collectors.groupingBy(
            a -> new Grupperingsnøkkel(a.getOpptjeningsnøkkel(), a.getOpptjeningAktivitetType())
        ));

        return gruppertPåNøkkel.entrySet()
            .stream()
            .filter(e -> !e.getValue().isEmpty())
            .flatMap(e -> {
                var segmenter = e.getValue().stream().map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v.getVurderingsStatus())).toList();
                var tidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceLeftHandSide);
                var first = e.getValue().get(0);
                return tidslinje.compress().toSegments().stream().map(s ->
                    OpptjeningsperiodeForSaksbehandling.Builder.kopi(first)
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
                        .medVurderingsStatus(s.getValue()).build()
                );
            }).toList();
    }

    private boolean filtrerForVilkårsperiode(DatoIntervallEntitet vilkårsperiode, OpptjeningsperiodeForSaksbehandling oa, VilkårUtfallMerknad vilkårUtfallMerknad) {
        return oa.getPeriode().getFomDato().isBefore(vilkårsperiode.getFomDato()) || (erInaktiv(vilkårUtfallMerknad) && oa.getPeriode().getFomDato().equals(vilkårsperiode.getFomDato()));
    }

    private boolean erInaktiv(VilkårUtfallMerknad vilkårUtfallMerknad) {
        return VilkårUtfallMerknad.VM_7847_B.equals(vilkårUtfallMerknad) || VilkårUtfallMerknad.VM_7847_A.equals(vilkårUtfallMerknad);
    }

    @Override
    public Optional<OppgittOpptjening> finnOppgittOpptjening(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        var oppgittOpptjeningTjeneste = oppgittOpptjeningFilterProvider.finnOpptjeningFilter(referanse.getBehandlingId());
        return oppgittOpptjeningTjeneste.hentOppgittOpptjening(referanse.getBehandlingId(), iayGrunnlag, stp);
    }

    private Optional<OpptjeningAktiviteter> hentOpptjeningForBeregning(BehandlingReferanse ref,
                                                                       InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                       DatoIntervallEntitet stp) {
        var opptjeningsPerioder = hentRelevanteOpptjeningsaktiviteterForBeregning(ref, iayGrunnlag, stp)
            .stream()
            .map(this::mapOpptjeningPeriode)
            .collect(Collectors.toList());
        if (opptjeningsPerioder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OpptjeningAktiviteter(opptjeningsPerioder));
    }

    @Override
    public Optional<OpptjeningAktiviteter> hentEksaktOpptjeningForBeregning(BehandlingReferanse ref,
                                                                            InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        Optional<OpptjeningAktiviteter> opptjeningAktiviteter = hentOpptjeningForBeregning(ref, iayGrunnlag, vilkårsperiode);
        return opptjeningAktiviteter;
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


    private record Grupperingsnøkkel(Opptjeningsnøkkel nøkkel, OpptjeningAktivitetType type) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Grupperingsnøkkel that = (Grupperingsnøkkel) o;
            return Objects.equals(nøkkel, that.nøkkel) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nøkkel, type);
        }
    }

}
