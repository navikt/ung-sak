package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.vedtak.ekstern.OverlappendeYtelserTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd.InfotrygdService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygd.InfotrygdService;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class InfotrygdMigreringTjeneste {


    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private FagsakRepository fagsakRepository;
    private final KantIKantVurderer kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();
    private InfotrygdService infotrygdService;

    public InfotrygdMigreringTjeneste() {
    }

    @Inject
    public InfotrygdMigreringTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                      @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                      FagsakRepository fagsakRepository,
                                      InfotrygdService infotrygdService) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.infotrygdService = infotrygdService;
    }

    public List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref) {

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var infotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(ref.getFagsakId());
        var eksisterendeMigreringTilVurdering = finnEksisterendeMigreringTilVurdering(perioderTilVurdering, infotrygdMigreringer);

        if (eksisterendeMigreringTilVurdering.isEmpty()) {
            return Collections.emptyList();
        }

        var grunnlagsperioderPrAktør = infotrygdService.finnGrunnlagsperioderForAndreAktører(
            ref.getPleietrengendeAktørId(),
            ref.getAktørId(),
            LocalDate.now().minusYears(1));
        var aksjonspunkter = new ArrayList<AksjonspunktResultat>();

        var harAnnenAktørMedOverlappendeInfotrygdperiode = grunnlagsperioderPrAktør.entrySet()
            .stream()
            .filter(e -> !e.getKey().equals(ref.getAktørId()))
            .flatMap(e -> e.getValue().stream())
            .anyMatch(p -> eksisterendeMigreringTilVurdering.stream()
                .anyMatch(im -> p.inkluderer(im.getSkjæringstidspunkt())));

        if (harAnnenAktørMedOverlappendeInfotrygdperiode) {
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE_ANNEN_PART));
        }

        var psbInfotrygdFilter = finnPSBInfotryd(ref.getBehandlingId(), ref.getAktørId());
        List<YtelseAnvist> anvistePeriodeSomManglerSøknad = finnAnvistePerioderFraInfotrygdUtenSøknad(perioderTilVurdering, psbInfotrygdFilter, ref.getBehandlingId());

        if (!anvistePeriodeSomManglerSøknad.isEmpty()) {
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE));
        }
        return aksjonspunkter;
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

    private List<YtelseAnvist> finnAnvistePerioderFraInfotrygdUtenSøknad(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, YtelseFilter psbInfotrygdFilter, Long behandlingId) {
        var fullstendigePerioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(behandlingId);
        var førsteStpTilVurdering = perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).min(Comparator.naturalOrder()).orElseThrow();
        var anvistePerioderUtenSøknad = psbInfotrygdFilter.getFiltrertYtelser().stream()
            .flatMap(y -> y.getYtelseAnvist().stream())
            .filter(ya -> harAnvisningSammeÅrSomFørstePeriodeTilVurdering(førsteStpTilVurdering, ya))
            .filter(ya -> !dekkesAvSøknad(fullstendigePerioder, ya, førsteStpTilVurdering.getYear()))
            .collect(Collectors.toList());
        return anvistePerioderUtenSøknad;
    }

    private boolean dekkesAvSøknad(NavigableSet<DatoIntervallEntitet> fullstendigePerioder, YtelseAnvist ya, int year) {
        var førsteMandagIÅret = LocalDate.of(year, 1, 1).with(TemporalAdjusters.dayOfWeekInMonth(1, DayOfWeek.MONDAY));
        var anvistFom = ya.getAnvistFOM().isBefore(førsteMandagIÅret) ? førsteMandagIÅret : ya.getAnvistFOM();
        return fullstendigePerioder.stream().anyMatch(p -> p.getFomDato().equals(anvistFom) && !p.getTomDato().isBefore(ya.getAnvistTOM()));
    }

    private boolean harAnvisningSammeÅrSomFørstePeriodeTilVurdering(LocalDate førsteStpTilVurdering, YtelseAnvist ya) {
        return ya.getAnvistTOM().getYear() >= førsteStpTilVurdering.getYear() && ya.getAnvistFOM().getYear() <= førsteStpTilVurdering.getYear();
    }

}
