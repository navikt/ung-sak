package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collection;
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
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.avklarfakta.InfotrygdMigreringTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.vedtak.ekstern.OverlappendeYtelserTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PSBInfotrygdMigreringTjeneste implements InfotrygdMigreringTjeneste {


    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private FagsakRepository fagsakRepository;
    private final KantIKantVurderer kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

    private boolean toggleMigrering;

    public PSBInfotrygdMigreringTjeneste() {
    }

    @Inject
    public PSBInfotrygdMigreringTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                         @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                         FagsakRepository fagsakRepository,
                                         @KonfigVerdi(value = "PSB_INFOTRYGD_MIGRERING", required = false, defaultVerdi = "false") boolean toggleMigrering) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.toggleMigrering = toggleMigrering;
    }

    @Override
    public void finnOgOpprettMigrertePerioder(Long behandlingId, AktørId aktørId, Long fagsakId) {
        if (toggleMigrering) {

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
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        Optional<AktørYtelse> aktørYtelse = iayGrunnlag.getAktørYtelseFraRegister(aktørId);
        YtelseFilter ytelseFilter = lagInfotrygdPSBFilter(aktørYtelse);
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

    private YtelseFilter lagInfotrygdPSBFilter(Optional<AktørYtelse> aktørYtelse) {
        return new YtelseFilter(aktørYtelse).filter(y ->
            y.getYtelseType().equals(FagsakYtelseType.PSB) &&
                y.getKilde().equals(Fagsystem.INFOTRYGD));
    }

}
