package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
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
    private VilkårResultatRepository vilkårResultatRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private InfotrygdService infotrygdService;

    public InfotrygdMigreringTjeneste() {
    }

    @Inject
    public InfotrygdMigreringTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                      @BehandlingTypeRef @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                      VilkårResultatRepository vilkårResultatRepository, FagsakRepository fagsakRepository,
                                      BehandlingRepository behandlingRepository, InfotrygdService infotrygdService) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
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

        var skjæringstidspunkterTilVurdering = perioderTilVurdering.stream()
            .map(DatoIntervallEntitet::getFomDato)
            .filter(fomDato -> eksisterendeInfotrygdMigreringer.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt).anyMatch(fomDato::equals))
            .collect(Collectors.toSet());

        var datoerForOverlapp = finnDatoerForOverlapp(perioderTilVurdering, behandlingId, aktørId);
        var alleSøknadsperioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(behandlingId);
        validerIngenTrukketPeriode(behandlingId, aktørId, eksisterendeInfotrygdMigreringer, alleSøknadsperioder);

        var utledetInfotrygdmigreringTilVurdering = new HashSet<LocalDate>();
        utledetInfotrygdmigreringTilVurdering.addAll(datoerForOverlapp);
        utledetInfotrygdmigreringTilVurdering.addAll(skjæringstidspunkterTilVurdering);
        utledetInfotrygdmigreringTilVurdering.forEach(localDate -> opprettMigrering(fagsakId, localDate, perioderTilVurdering));
        deaktiverSkjæringstidspunkterSomErFlyttet(eksisterendeInfotrygdMigreringer, behandlingId);
    }

    private void deaktiverSkjæringstidspunkterSomErFlyttet(List<SakInfotrygdMigrering> eksisterendeInfotrygdMigreringer, Long behandlingId) {
        var alleSkjæringstidspunkt = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)).stream()
            .flatMap(v -> v.getPerioder().stream())
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .collect(Collectors.toSet());
        var migreringerSomSkalDeaktiveres = eksisterendeInfotrygdMigreringer.stream()
            .filter(m -> alleSkjæringstidspunkt.stream().noneMatch(m.getSkjæringstidspunkt()::equals))
            .collect(Collectors.toUnmodifiableSet());
        migreringerSomSkalDeaktiveres.forEach(this::deaktiver);
    }

    private void deaktiver(SakInfotrygdMigrering m) {
        log.info("Deaktiverer skjæringstidspunkt " + m.getSkjæringstidspunkt() + " for fagsak " + m.getFagsakId());
        fagsakRepository.deaktiverInfotrygdmigrering(m.getFagsakId(), m.getSkjæringstidspunkt());
    }

    private void validerIngenTrukketPeriode(Long behandlingId, AktørId aktørId, List<SakInfotrygdMigrering> eksisterendeInfotrygdMigreringer, NavigableSet<DatoIntervallEntitet> alleSøknadsperioder) {
        var anvistePerioder = finnPerioderMedPSBFraInfotrygd(behandlingId, aktørId);
        var migreringUtenSøknad = eksisterendeInfotrygdMigreringer.stream()
            .map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .filter(migrertStp -> alleSøknadsperioder.stream().noneMatch(periode -> periode.inkluderer(migrertStp)) &&
                (anvistePerioder.stream().noneMatch(p -> inkludererEllerErKantIKantMedPeriodeFraInfotrygd(migrertStp, p))))
            .collect(Collectors.toList());
        if (!migreringUtenSøknad.isEmpty()) {
            throw new IllegalStateException("Støtter ikke trukket søknad for migrering fra infotrygd etter fjerning av periode fra inforygd.");
        }
    }

    private boolean inkludererEllerErKantIKantMedPeriodeFraInfotrygd(LocalDate migrertStp, DatoIntervallEntitet p) {
        return p.inkluderer(migrertStp) || perioderTilVurderingTjeneste.getKantIKantVurderer().erKantIKant(p, DatoIntervallEntitet.fraOgMedTilOgMed(migrertStp, migrertStp));
    }

    private void opprettMigrering(Long fagsakId, LocalDate skjæringstidspunkt, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        if (perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).noneMatch(skjæringstidspunkt::equals)) {
            throw new IllegalStateException("Prøver å opprette migrering for dato som ikke er skjæringstidspunkt for periode til vurdering: " + skjæringstidspunkt);
        }
        fagsakRepository.opprettInfotrygdmigrering(fagsakId, skjæringstidspunkt);
    }

    private Set<LocalDate> finnDatoerForOverlapp(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Long behandlingId, AktørId aktørId) {
        List<DatoIntervallEntitet> anvistePerioder = finnPerioderMedPSBFraInfotrygd(behandlingId, aktørId);
        var stpForMigrering = new HashSet<LocalDate>();
        stpForMigrering.addAll(finnSkjæringstidspunktForOverlapp(perioderTilVurdering, anvistePerioder));
        stpForMigrering.addAll(finnSkjæringstidspunktForKantIKant(perioderTilVurdering, anvistePerioder));
        return stpForMigrering;
    }

    private List<DatoIntervallEntitet> finnPerioderMedPSBFraInfotrygd(Long behandlingId, AktørId aktørId) {
        YtelseFilter ytelseFilter = finnPSBInfotryd(behandlingId, aktørId);
        var anvistePerioder = ytelseFilter.getFiltrertYtelser()
            .stream()
            .filter(y -> y.getYtelseAnvist() != null)
            .flatMap(y -> y.getYtelseAnvist().stream())
            .map(y -> DatoIntervallEntitet.fraOgMedTilOgMed(y.getAnvistFOM(), y.getAnvistTOM()))
            .toList();
        return anvistePerioder;
    }

    private Set<LocalDate> finnSkjæringstidspunktForKantIKant(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, List<DatoIntervallEntitet> anvistePerioder) {
        var kantIKantPerioder = finnKantIKantPeriode(perioderTilVurdering, anvistePerioder);
        return kantIKantPerioder.stream().map(DatoIntervallEntitet::getFomDato).collect(Collectors.toSet());
    }

    private Set<LocalDate> finnSkjæringstidspunktForOverlapp(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, List<DatoIntervallEntitet> anvistePerioder) {
        return perioderTilVurdering.stream()
            .map(DatoIntervallEntitet::getFomDato).filter(d -> anvistePerioder.stream()
                .anyMatch(p -> p.inkluderer(d))).collect(Collectors.toSet());

    }

    private Set<DatoIntervallEntitet> finnKantIKantPeriode(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, List<DatoIntervallEntitet> anvistePerioder) {
        return perioderTilVurdering.stream()
            .filter(p -> anvistePerioder.stream().anyMatch(ap -> perioderTilVurderingTjeneste.getKantIKantVurderer().erKantIKant(p, ap)))
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
