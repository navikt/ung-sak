package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
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

    /**
     * Markerer skjæringstidspunkter for migrering fra infotrygd
     * Alle perioder med overlapp eller kant i kant markeres som migrert fra infotrygd
     * Dersom et skjæringstidspunkt først har blitt markert endres ikke dette selv om overlappet fjernes i infotrygd
     * <p>
     * Dersom en migrert periode utvides i forkant UTEN overlapp, fjernes markeringen og perioden behandles som en ny periode (revurdering av vedtak fra infotrygd med nytt skjæringstidspunt)
     * <p>
     * Dersom en migrert periode utvides i forkant MED overlapp, endres markeringen til å gjelde det nye skjæringstidspunktet
     * <p>
     * Nye perioder som kommer inn vil bli markert fortløpende dersom det finnes overlapp.
     *
     * @param behandlingId BehandlingId
     * @param aktørId      aktørid for søker
     * @param fagsakId     Fagsak id
     */
    public void finnOgOpprettMigrertePerioder(Long behandlingId, AktørId aktørId, Long fagsakId) {

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var eksisterendeInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(fagsakId);
        validerIngenTrukketPeriode(behandlingId, eksisterendeInfotrygdMigreringer);

        var skjæringstidspunkterTilVurdering = perioderTilVurdering.stream()
            .filter(p -> eksisterendeInfotrygdMigreringer.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt)
                .anyMatch(p::inkluderer))
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toSet());

        var datoerForOverlapp = finnDatoerForOverlapp(perioderTilVurdering, behandlingId, aktørId);

        var utledetInfotrygdmigreringTilVurdering = new HashSet<LocalDate>();
        utledetInfotrygdmigreringTilVurdering.addAll(datoerForOverlapp);
        utledetInfotrygdmigreringTilVurdering.addAll(skjæringstidspunkterTilVurdering);

        utledetInfotrygdmigreringTilVurdering.forEach(localDate -> opprettMigrering(fagsakId, localDate, perioderTilVurdering));

        deaktiverSkjæringstidspunkterSomErFlyttet(behandlingId, eksisterendeInfotrygdMigreringer, perioderTilVurdering, utledetInfotrygdmigreringTilVurdering);
    }

    private void deaktiverSkjæringstidspunkterSomErFlyttet(Long behandlingId,
                                                           List<SakInfotrygdMigrering> eksisterendeInfotrygdMigreringer,
                                                           NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                                           HashSet<LocalDate> utledetInfotrygdmigreringTilVurdering) {
        var migreringerSomSkalDeaktiveres = eksisterendeInfotrygdMigreringer.stream()
            .filter(m -> perioderTilVurdering.stream().anyMatch(periode -> periode.inkluderer(m.getSkjæringstidspunkt()) && !m.getSkjæringstidspunkt().equals(periode.getFomDato())))
            .collect(Collectors.toUnmodifiableSet());
        migreringerSomSkalDeaktiveres.forEach(m -> deaktiver(m, behandlingId, utledetInfotrygdmigreringTilVurdering));
    }

    private void deaktiver(SakInfotrygdMigrering m, Long behandlingId, HashSet<LocalDate> utledetInfotrygdmigreringTilVurdering) {
        var allePerioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(behandlingId);
        if (allePerioder.stream().map(DatoIntervallEntitet::getFomDato).anyMatch(fom -> m.getSkjæringstidspunkt().equals(fom))) {
            throw new IllegalStateException("Skal ikke deaktivere migrering for eksisterende skjærinstidspunkt");
        }
        var overlappendePeriode = allePerioder.stream()
            .filter(p -> p.inkluderer(m.getSkjæringstidspunkt())).findFirst();
        if (overlappendePeriode.isPresent()) {
            var harUtledetNyttVedStart = utledetInfotrygdmigreringTilVurdering.stream().anyMatch(nye -> overlappendePeriode.get().getFomDato().equals(nye));
            if (!harUtledetNyttVedStart) {
                throw new IllegalStateException("Skal ikke deaktivere skjæringstidspunkt for migrering uten å opprette nytt ved start av periode");
            }
        }
        log.info("Deaktiverer skjæringstidspunkt " + m.getSkjæringstidspunkt() + " for fagsak " + m.getFagsakId());
        fagsakRepository.deaktiverInfotrygdmigrering(m.getFagsakId(), m.getSkjæringstidspunkt());
    }

    private void validerIngenTrukketPeriode(Long behandlingId, List<SakInfotrygdMigrering> eksisterendeInfotrygdMigreringer) {
        var fullstendigePerioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(behandlingId);
        var migreringUtenSøknad = eksisterendeInfotrygdMigreringer.stream()
            .filter(sim -> fullstendigePerioder.stream().noneMatch(periode -> periode.inkluderer(sim.getSkjæringstidspunkt())))
            .collect(Collectors.toList());
        if (!migreringUtenSøknad.isEmpty()) {
            throw new IllegalStateException("Støtter ikke trukket søknad for migrering fra infotrygd");
        }
    }

    private void opprettMigrering(Long fagsakId, LocalDate skjæringstidspunkt, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        if (perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).noneMatch(skjæringstidspunkt::equals)) {
            throw new IllegalStateException("Prøver å opprette migrering for dato som ikke er skjæringstidspunkt for periode til vurdering: " + skjæringstidspunkt);
        }
        fagsakRepository.opprettInfotrygdmigrering(fagsakId, skjæringstidspunkt);
    }

    private Set<LocalDate> finnDatoerForOverlapp(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Long behandlingId, AktørId aktørId) {
        YtelseFilter ytelseFilter = finnPSBInfotryd(behandlingId, aktørId);
        var stpForMigrering = new HashSet<LocalDate>();
        stpForMigrering.addAll(finnSkjæringstidspunktForOverlapp(perioderTilVurdering, ytelseFilter));
        stpForMigrering.addAll(finnSkjæringstidspunktForKantIKant(perioderTilVurdering, ytelseFilter));
        return stpForMigrering;
    }

    private Set<LocalDate> finnSkjæringstidspunktForKantIKant(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, YtelseFilter ytelseFilter) {
        var kantIKantPerioder = finnKantIKantPeriode(ytelseFilter, perioderTilVurdering);
        return kantIKantPerioder.stream().map(DatoIntervallEntitet::getFomDato).collect(Collectors.toSet());
    }

    private Set<LocalDate> finnSkjæringstidspunktForOverlapp(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, YtelseFilter ytelseFilter) {
        var alleAnvistePerioder = ytelseFilter.getFiltrertYtelser()
            .stream()
            .filter(y -> y.getYtelseAnvist() != null)
            .flatMap(y -> y.getYtelseAnvist().stream())
            .map(y -> DatoIntervallEntitet.fraOgMedTilOgMed(y.getAnvistFOM(), y.getAnvistTOM()))
            .toList();
        return perioderTilVurdering.stream()
            .map(DatoIntervallEntitet::getFomDato).filter(d -> alleAnvistePerioder.stream()
                .anyMatch(p -> p.inkluderer(d))).collect(Collectors.toSet());

    }

    private Set<DatoIntervallEntitet> finnKantIKantPeriode(YtelseFilter ytelseFilter, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        return perioderTilVurdering.stream()
            .filter(p -> ytelseFilter.getFiltrertYtelser().stream()
                .anyMatch(y ->
                    y.getYtelseAnvist().stream()
                        .anyMatch(ya -> kantIKantVurderer.erKantIKant(p, DatoIntervallEntitet.fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM())))))
            .collect(Collectors.toSet());
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
