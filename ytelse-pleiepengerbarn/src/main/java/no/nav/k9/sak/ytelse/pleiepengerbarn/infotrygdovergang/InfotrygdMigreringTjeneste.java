package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.vedtak.ekstern.OverlappendeYtelserTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd.InfotrygdService;

@ApplicationScoped
public class InfotrygdMigreringTjeneste {

    private static final Logger log = LoggerFactory.getLogger(InfotrygdMigreringTjeneste.class);

    public static final String GAMMEL_ORDNING_KODE = "PB";
    public static final int ÅR_2022 = 2022;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private final KantIKantVurderer kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();
    private InfotrygdService infotrygdService;

    public InfotrygdMigreringTjeneste() {
    }

    @Inject
    public InfotrygdMigreringTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                      @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                      FagsakRepository fagsakRepository,
                                      BehandlingRepository behandlingRepository, InfotrygdService infotrygdService) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.infotrygdService = infotrygdService;
    }

    public List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref) {

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var psbInfotrygdFilter = finnPSBInfotryd(ref.getBehandlingId(), ref.getAktørId());
        var perioderSomManglerSøknad = finnPerioderFraInfotrygdUtenSøknad(perioderTilVurdering, psbInfotrygdFilter, ref.getBehandlingId());

        var aksjonspunkter = new ArrayList<AksjonspunktResultat>();
        if (!perioderSomManglerSøknad.isEmpty()) {
            log.info("Fant perioder i infotrygd som mangler søknad: " + perioderSomManglerSøknad);
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE));
        }

        var grunnlagsperioderPrAktør = infotrygdService.finnGrunnlagsperioderForAndreAktører(
            ref.getPleietrengendeAktørId(),
            ref.getAktørId(),
            LocalDate.now().minusYears(1),
            Set.of("PN", GAMMEL_ORDNING_KODE));

        if (harBerørtSakPåGammelOrdning(grunnlagsperioderPrAktør, perioderTilVurdering)) {
            throw new IllegalStateException("Fant berørt sak på gammel ordning");
        }

        if (!grunnlagsperioderPrAktør.isEmpty()) {
            log.info("Fant berørte perioder i infotrygd: " + grunnlagsperioderPrAktør.values());
        }

        var ikkeSøktMedOverlappMap = grunnlagsperioderPrAktør.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> finnIkkeSøktOverlapp(perioderTilVurdering, getInfotrygdPerioder(e), e.getKey())));

        var harAnnenAktørMedOverlappendeInfotrygdperiode = ikkeSøktMedOverlappMap.values().stream().anyMatch(v -> !v.isEmpty());

        if (harAnnenAktørMedOverlappendeInfotrygdperiode) {
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE_ANNEN_PART));
        }

        return aksjonspunkter;
    }

    @NotNull
    private List<DatoIntervallEntitet> getInfotrygdPerioder(Map.Entry<AktørId, List<IntervallMedBehandlingstema>> e) {
        return e.getValue().stream().map(IntervallMedBehandlingstema::intervall).collect(Collectors.toList());
    }

    private List<DatoIntervallEntitet> finnIkkeSøktOverlapp(NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                                            List<DatoIntervallEntitet> infotrygdperioder, AktørId annenPartAktørId) {
        var annenPartFagsakId = fagsakRepository.hentForBruker(annenPartAktørId).stream().filter(f -> f.getYtelseType().equals(FagsakYtelseType.PSB))
            .findFirst()
            .map(Fagsak::getId);

        var annenPartSøktePerioderStream = annenPartFagsakId.flatMap(behandlingRepository::hentSisteBehandlingForFagsakId)
            .map(Behandling::getId)
            .stream()
            .flatMap(id -> perioderTilVurderingTjeneste.utledFullstendigePerioder(id).stream());
        var annenPartSøktTidslinje = lagTidslinje(annenPartSøktePerioderStream);
        var annenPartInfotrygdTidslinje = lagTidslinje(infotrygdperioder.stream());
        var tilVurderingTidslinje = lagTidslinje(perioderTilVurdering.stream());

        return annenPartInfotrygdTidslinje.intersection(tilVurderingTidslinje)
            .disjoint(annenPartSøktTidslinje)
            .toSegments()
            .stream()
            .map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
            .collect(Collectors.toList());
    }


    private LocalDateTimeline<Boolean> lagTidslinje(Stream<DatoIntervallEntitet> periodeStream) {
        var annenPartSøktePerioderSegments = periodeStream
            .map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), true))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(annenPartSøktePerioderSegments, StandardCombinators::coalesceLeftHandSide);
    }

    private boolean harBerørtSakPåGammelOrdning(Map<AktørId, List<IntervallMedBehandlingstema>> grunnlagsperioderPrAktør, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        return grunnlagsperioderPrAktør.values().stream()
            .flatMap(Collection::stream)
            .filter(intervallMedTema -> perioderTilVurdering.stream().anyMatch(p -> p.overlapper(intervallMedTema.intervall())))
            .anyMatch(p -> p.behandlingstema().equals(GAMMEL_ORDNING_KODE));
    }

    public void finnOgOpprettMigrertePerioder(Long behandlingId, AktørId aktørId, Long fagsakId) {

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var eksisterendeInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(fagsakId);
        deaktiverMigreringerUtenSøknad(behandlingId, eksisterendeInfotrygdMigreringer);
        var eksisterendeMigreringTilVurdering = finnEksisterendeMigreringTilVurdering(perioderTilVurdering, eksisterendeInfotrygdMigreringer);
        if (!eksisterendeMigreringTilVurdering.isEmpty()) {
            eksisterendeMigreringTilVurdering.forEach(oppdaterSkjæringstidspunkt(perioderTilVurdering));
        } else {
            var saksnummer = fagsakRepository.finnEksaktFagsak(fagsakId).getSaksnummer();
            var stpMedOverlapp = finnSkjæringstidspunktMedOverlapp(saksnummer, perioderTilVurdering, behandlingId, aktørId);
            stpMedOverlapp.ifPresent(localDate -> fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsakId, localDate)));
        }
    }

    private void deaktiverMigreringerUtenSøknad(Long behandlingId, List<SakInfotrygdMigrering> eksisterendeInfotrygdMigreringer) {
        var fullstendigePerioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(behandlingId);
        var migreringUtenSøknad = eksisterendeInfotrygdMigreringer.stream()
            .filter(sim -> fullstendigePerioder.stream().noneMatch(periode -> periode.inkluderer(sim.getSkjæringstidspunkt())))
            .collect(Collectors.toList());
        migreringUtenSøknad.forEach(m -> {
            m.setAktiv(false);
            fagsakRepository.lagreOgFlush(m);
        });
    }

    private Consumer<SakInfotrygdMigrering> oppdaterSkjæringstidspunkt(NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        return sakInfotrygdMigrering -> {
            var skjæringstidspunkt = perioderTilVurdering.stream()
                .filter(p -> p.inkluderer(sakInfotrygdMigrering.getSkjæringstidspunkt()))
                .findFirst()
                .map(DatoIntervallEntitet::getFomDato)
                .orElseThrow();
            if (!skjæringstidspunkt.equals(sakInfotrygdMigrering.getSkjæringstidspunkt())) {
                sakInfotrygdMigrering.setSkjæringstidspunkt(skjæringstidspunkt);
                fagsakRepository.lagreOgFlush(sakInfotrygdMigrering);
            }
        };
    }

    private Optional<LocalDate> finnSkjæringstidspunktMedOverlapp(Saksnummer saksnummer, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Long behandlingId, AktørId aktørId) {
        YtelseFilter ytelseFilter = finnPSBInfotryd(behandlingId, aktørId);
        LocalDateTimeline<Boolean> vilkårsperioderTidslinje = lagPerioderTilVureringTidslinje(perioderTilVurdering);
        Map<Ytelse, NavigableSet<LocalDateInterval>> psbOverlapp = OverlappendeYtelserTjeneste.doFinnOverlappendeYtelser(saksnummer, vilkårsperioderTidslinje, ytelseFilter);
        var kantIKantPeriode = finnKantIKantPeriode(ytelseFilter, perioderTilVurdering);
        if (!psbOverlapp.isEmpty()) {
            return finnSkjæringstidspunktMedOverlapp(perioderTilVurdering, psbOverlapp);
        }
        return kantIKantPeriode.map(DatoIntervallEntitet::getFomDato);
    }

    private List<SakInfotrygdMigrering> finnEksisterendeMigreringTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, List<SakInfotrygdMigrering> eksisterendeInfotrygdMigreringer) {
        var migreringTilVurdering = eksisterendeInfotrygdMigreringer.stream()
            .filter(sim -> perioderTilVurdering.stream().anyMatch(periode -> periode.inkluderer(sim.getSkjæringstidspunkt())))
            .collect(Collectors.toList());
        var antallPerioderMedOverlapp = perioderTilVurdering.stream().filter(periode -> migreringTilVurdering.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .anyMatch(periode::inkluderer)).count();
        if (migreringTilVurdering.size() > antallPerioderMedOverlapp) {
            throw new IllegalStateException("Forventer maksimalt en migrering til vurdering per periode");
        }
        return migreringTilVurdering;
    }

    private Optional<DatoIntervallEntitet> finnKantIKantPeriode(YtelseFilter ytelseFilter, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        return perioderTilVurdering.stream()
            .filter(p -> ytelseFilter.getFiltrertYtelser().stream()
                .anyMatch(y ->
                    y.getYtelseAnvist().stream()
                        .anyMatch(ya -> kantIKantVurderer.erKantIKant(p, DatoIntervallEntitet.fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM())))))
            .findFirst();
    }

    private Optional<LocalDate> finnSkjæringstidspunktMedOverlapp(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Map<Ytelse, NavigableSet<LocalDateInterval>> psbOverlapp) {
        Set<LocalDateInterval> overlappPerioder = psbOverlapp.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        Set<LocalDate> stpMedInfotrygdoverlapp = finnStpMedOverlapp(perioderTilVurdering, overlappPerioder);
        if (stpMedInfotrygdoverlapp.isEmpty()) {
            return Optional.empty();
        }
        if (stpMedInfotrygdoverlapp.size() == 1) {
            return Optional.of(stpMedInfotrygdoverlapp.iterator().next());
        }
        throw new IllegalStateException("Fant flere skjæringstidspunkter med overlapp mot PSB i infotrygd");
    }

    private Set<LocalDate> finnStpMedOverlapp(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Set<LocalDateInterval> overlappPerioder) {
        return perioderTilVurdering.stream()
            .filter(p -> overlappPerioder.stream().map(ldi -> DatoIntervallEntitet.fraOgMedTilOgMed(ldi.getFomDato(), ldi.getTomDato()))
                .anyMatch(op -> op.overlapper(p)))
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toSet());
    }

    private LocalDateTimeline<Boolean> lagPerioderTilVureringTidslinje(NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        List<LocalDateSegment<Boolean>> segmenter = perioderTilVurdering.stream()
            .map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), true))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segmenter);
    }

    private YtelseFilter finnPSBInfotryd(Long behandlingId, AktørId aktørId) {
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        Optional<AktørYtelse> aktørYtelse = iayGrunnlag.getAktørYtelseFraRegister(aktørId);
        YtelseFilter ytelseFilter = lagInfotrygdPSBFilter(aktørYtelse);
        return ytelseFilter;
    }


    private YtelseFilter lagInfotrygdPSBFilter(Optional<AktørYtelse> aktørYtelse) {
        return new YtelseFilter(aktørYtelse).filter(y ->
            y.getYtelseType().equals(FagsakYtelseType.PSB) &&
                y.getKilde().equals(Fagsystem.INFOTRYGD));
    }

    private List<DatoIntervallEntitet> finnPerioderFraInfotrygdUtenSøknad(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, YtelseFilter psbInfotrygdFilter, Long behandlingId) {
        var fullstendigePerioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(behandlingId);
        if (!harPerioderTilVurderingIEllerEtterÅr(perioderTilVurdering, ÅR_2022)) {
            return Collections.emptyList();
        }
        var ytelseTidslinje = new LocalDateTimeline<>(psbInfotrygdFilter.getFiltrertYtelser().stream()
            .flatMap(y -> y.getYtelseAnvist().stream())
            .map(ya -> new LocalDateSegment<>(ya.getAnvistFOM(), ya.getAnvistTOM(), true))
            .collect(Collectors.toList()), StandardCombinators::coalesceLeftHandSide);
        var søknadTidslinje = new LocalDateTimeline<>(fullstendigePerioder.stream()
            .map(this::utvidetOverHelgSegment)
            .collect(Collectors.toList()), StandardCombinators::coalesceLeftHandSide);
        var tidslinje2022 = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 12, 31), true)));

        var ytelseIkkeSøktForTidslinje = ytelseTidslinje.intersection(tidslinje2022)
            .disjoint(søknadTidslinje);

        return ytelseIkkeSøktForTidslinje.toSegments()
            .stream().map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
            .toList();
    }

    private LocalDateSegment<Boolean> utvidetOverHelgSegment(DatoIntervallEntitet s) {
        var tomUkedag = s.getTomDato().getDayOfWeek();
        var fomUkedag = s.getFomDato().getDayOfWeek();
        var tom = tomUkedag.equals(DayOfWeek.FRIDAY) || tomUkedag.equals(DayOfWeek.SATURDAY) ? s.getTomDato().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)) : s.getTomDato();
        var fom = fomUkedag.equals(DayOfWeek.MONDAY) ? s.getFomDato().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)) : s.getFomDato();
        return new LocalDateSegment<>(fom, tom, true);
    }

    private boolean harPerioderTilVurderingIEllerEtterÅr(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, int år) {
        return perioderTilVurdering.stream().anyMatch(p -> p.getFomDato().getYear() <= år && p.getTomDato().getYear() >= år);
    }

}
