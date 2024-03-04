package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.registerendringer.Aktivitetsendringer;
import no.nav.k9.sak.registerendringer.EndringerIAY;
import no.nav.k9.sak.registerendringer.Endringstype;
import no.nav.k9.sak.registerendringer.RelevanteIAYRegisterendringerUtleder;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@ApplicationScoped
public class PleiepengerRelevanteIAYRegisterendringerUtleder implements RelevanteIAYRegisterendringerUtleder {
    private VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårTjeneste vilkårTjeneste;
    private UtledTilkjentYtelseEndring utledTilkjentYtelseEndring;
    private final UtledAktivitetsperiodeEndring utledAktivitetsperiodeEndring = new UtledAktivitetsperiodeEndring();


    public PleiepengerRelevanteIAYRegisterendringerUtleder() {
        // CDI
    }

    @Inject
    public PleiepengerRelevanteIAYRegisterendringerUtleder(VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                                           @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                                           InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, VilkårTjeneste vilkårTjeneste,
                                                           UtledTilkjentYtelseEndring utledTilkjentYtelseEndring) {
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.utledTilkjentYtelseEndring = utledTilkjentYtelseEndring;
    }


    @Override
    public EndringerIAY utledRelevanteEndringer(BehandlingReferanse behandlingReferanse) {
        var tilkjentYtelseEndringPrMottaker = utledTilkjentYtelseEndring.utledEndringer(behandlingReferanse);
        var aktivitetsperiodeEndringer = finnEndringerIAnsattperioder(behandlingReferanse);
        var revurdertePerioder = finnRevurdertePerioder(behandlingReferanse);
        var endringerIAktivitetsperioder = finnRelevanteEndringerIAnsattperioderPrAktivitet(aktivitetsperiodeEndringer, tilkjentYtelseEndringPrMottaker, revurdertePerioder);
        return new EndringerIAY(endringerIAktivitetsperioder, Collections.emptyList());
    }

    private List<Aktivitetsendringer> finnRelevanteEndringerIAnsattperioderPrAktivitet(List<UtledAktivitetsperiodeEndring.AktivitetsperiodeEndring> aktivitetsperiodeEndringer, List<UtledTilkjentYtelseEndring.EndringerForMottaker> endringerPrMottaker, Set<DatoIntervallEntitet> revurdertePerioder) {
        return aktivitetsperiodeEndringer.stream()
            .map(aktivitetsendringer -> {
                var endringIRegisterOgUtbetaling = utledRelevantEndringstidslinje(aktivitetsendringer, endringerPrMottaker, revurdertePerioder);
                return new Aktivitetsendringer(aktivitetsendringer.identifikator().arbeidsgiver(), aktivitetsendringer.identifikator().ref(), endringIRegisterOgUtbetaling);
            })
            .toList();
    }

    private List<UtledAktivitetsperiodeEndring.AktivitetsperiodeEndring> finnEndringerIAnsattperioder(BehandlingReferanse behandlingReferanse) {
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var originalIAYGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.getOriginalBehandlingId().orElseThrow());

        if (originalIAYGrunnlag.isEmpty()) {
            throw new IllegalStateException("Fant ikke iay fra original behandling");
        }
        var vilkårsperioder = finnVilkårsperioder(behandlingReferanse);

        return utledAktivitetsperiodeEndring.utledEndring(inntektArbeidYtelseGrunnlag, originalIAYGrunnlag.get(), vilkårsperioder, behandlingReferanse.getAktørId());
    }

    private Set<DatoIntervallEntitet> finnRevurdertePerioder(BehandlingReferanse behandlingReferanse) {
        var perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType())
            .utled(behandlingReferanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var perioderMedMarkering = vilkårPeriodeFilterProvider.getFilter(behandlingReferanse).filtrerPerioder(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        return perioderMedMarkering.stream().filter(p -> !p.erForlengelse()).map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet());
    }

    private TreeSet<DatoIntervallEntitet> finnVilkårsperioder(BehandlingReferanse behandlingReferanse) {
        var vilkårsperioder = vilkårTjeneste.hentVilkårResultat(behandlingReferanse.getBehandlingId())
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
        return vilkårsperioder;
    }

    private LocalDateTimeline<Endringstype> utledRelevantEndringstidslinje(UtledAktivitetsperiodeEndring.AktivitetsperiodeEndring aktivitetsendringer, List<UtledTilkjentYtelseEndring.EndringerForMottaker> endringerPrMottaker, Set<DatoIntervallEntitet> revurdertePerioder) {
        var tidslinjeForEndringIUtbetaling = finnTidslinjeForEndringIUtbetaling(aktivitetsendringer, endringerPrMottaker);
        var utvidet = utvidMedDagenFørStp(tidslinjeForEndringIUtbetaling, revurdertePerioder);
        return aktivitetsendringer.endringstidslinje().intersection(utvidet);
    }

    private LocalDateTimeline<Boolean> utvidMedDagenFørStp(LocalDateTimeline<Boolean> tidslinjeForEndringIUtbetaling, Set<DatoIntervallEntitet> vilkårsperioder) {
        return vilkårsperioder.stream().filter(p -> !tidslinjeForEndringIUtbetaling.intersection(p.toLocalDateInterval()).isEmpty())
            .map(p -> new LocalDateTimeline<>(p.getFomDato().minusDays(1), p.getFomDato().minusDays(1), true))
            .reduce(tidslinjeForEndringIUtbetaling, (t1, t2) -> t1.combine(t2, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN));
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForEndringIUtbetaling(UtledAktivitetsperiodeEndring.AktivitetsperiodeEndring aktivitetsendringer, List<UtledTilkjentYtelseEndring.EndringerForMottaker> endringerPrMottaker) {
        return endringerPrMottaker.stream()
            .filter(e -> Objects.equals(e.nøkkel().aktivitetsnøkkel().arbeidsgiver(), aktivitetsendringer.identifikator().arbeidsgiver()) &&
                e.nøkkel().aktivitetsnøkkel().arbeidsforholdRef().gjelderFor(aktivitetsendringer.identifikator().ref()) &&
                matcherStatusOgType(e.nøkkel().aktivitetsnøkkel().aktivitetStatus(), aktivitetsendringer.identifikator().arbeidType())
            )
            .map(UtledTilkjentYtelseEndring.EndringerForMottaker::tidslinjeMedEndringIYtelse)
            .map(t -> t.mapValue(it -> true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.combine(t2, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN));
    }

    private boolean matcherStatusOgType(AktivitetStatus aktivitetStatus, ArbeidType arbeidType) {
        if (aktivitetStatus.erFrilanser()) {
            return arbeidType.equals(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);
        } else if (aktivitetStatus.erArbeidstaker()) {
            return arbeidType.equals(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        }
        return false;
    }


}
