package no.nav.ung.ytelse.aktivitetspenger.del1.avkort;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerSøknadsperiodeTjeneste;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Dependent
public class AvkortTjeneste {

    private final static Set<VilkårType> STØTTEDE_VILKÅR_TYPER = Set.of(VilkårType.BOSTEDSVILKÅR, VilkårType.BISTANDSVILKÅR, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, VilkårType.AKTIVITETSVILKÅR);

    private final BehandlingRepository behandlingRepository;
    private final StartdatoRepository startdatoRepository;
    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public AvkortTjeneste(BehandlingRepository behandlingRepository, StartdatoRepository startdatoRepository, VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.startdatoRepository = startdatoRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public Map<VilkårType, LocalDateTimeline<Boolean>> utledTidslinjerForMuligAvkorting(Long behandlingId) {
        StartdatoGrunnlag startdatoGrunnlag = startdatoRepository.hentGrunnlag(behandlingId).orElse(null);
        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
        BehandlingType behandlingType = behandlingRepository.hentBehandling(behandlingId).getType();
        return STØTTEDE_VILKÅR_TYPER.stream().collect(Collectors.toMap(Function.identity(),
            vilkårType -> utledTidslinjeForMuligAvkorting(vilkårType, vilkårene, startdatoGrunnlag, behandlingType)
        ));
    }

    public LocalDateTimeline<Boolean> utledTidslinjeForMuligAvkorting(Long behandlingId, VilkårType vilkårType) {
        if (!STØTTEDE_VILKÅR_TYPER.contains(vilkårType)) {
            throw new IllegalArgumentException("AvkortTjeneste er tenkt brukt for vilkårtyper knyttet til nav-kontor-vilkår. Endre dette ved behov.");
        }
        BehandlingType behandlingType = behandlingRepository.hentBehandling(behandlingId).getType();
        StartdatoGrunnlag startdatoGrunnlagOpt = startdatoRepository.hentGrunnlag(behandlingId).orElse(null);
        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
        return utledTidslinjeForMuligAvkorting(vilkårType, vilkårene, startdatoGrunnlagOpt, behandlingType);
    }

    private static LocalDateTimeline<Boolean> utledTidslinjeForMuligAvkorting(VilkårType vilkårType, Vilkårene vilkårene, StartdatoGrunnlag startdatoGrunnlag, BehandlingType behandlingType) {
        if (vilkårType == VilkårType.AKTIVITETSVILKÅR && behandlingType == BehandlingType.FØRSTEGANGSSØKNAD) {
            return LocalDateTimeline.empty();
        }
        if (vilkårene == null || startdatoGrunnlag == null) {
            return LocalDateTimeline.empty();
        }
        LocalDateTimeline<VilkårPeriode> tidslinjeIkkeRelevantForVilkåret = vilkårene.getVilkårTimeline(vilkårType).filterValue(vilkårperiode -> vilkårperiode.getUtfall() == Utfall.IKKE_RELEVANT);
        return utledTidslinjeForMuligAvkorting(startdatoGrunnlag, tidslinjeIkkeRelevantForVilkåret);
    }

    public void validerAvkortBruktRiktig(Long behandlingId, LocalDateTimeline<Boolean> perioderSattTilAvkortet, VilkårType vilkårType) {
        LocalDateTimeline<Boolean> perioderSomKanSettesTilAvkortet = utledTidslinjeForMuligAvkorting(behandlingId, vilkårType);
        LocalDateTimeline<Boolean> manglendeOverlapp = perioderSattTilAvkortet.disjoint(perioderSomKanSettesTilAvkortet);
        if (!manglendeOverlapp.isEmpty()) {
            throw new IllegalArgumentException("Følgende perioder kan ikke settes til avkortet: " + manglendeOverlapp.getLocalDateIntervals());
        }
        if (!perioderSattTilAvkortet.isEmpty()) {
            LocalDate startAvkorting = perioderSattTilAvkortet.getMinLocalDate();
            LocalDateTimeline<Boolean> perioderSomMåSettesTilAvkortet = perioderSomKanSettesTilAvkortet.intersection(new LocalDateTimeline<>(startAvkorting, LocalDate.MAX, true));
            LocalDateTimeline<Boolean> manglendePerioder = perioderSomMåSettesTilAvkortet.disjoint(perioderSattTilAvkortet);
            if (!manglendePerioder.isEmpty()) {
                throw new IllegalArgumentException("Når avkorting blir brukt, må alle perioder etter første dato for avkorting også avkortes. Følgende mangler: " + manglendePerioder.getLocalDateIntervals());
            }
        }
    }

    static LocalDateTimeline<Boolean> utledTidslinjeForMuligAvkorting(StartdatoGrunnlag startdatoGrunnlag, LocalDateTimeline<?> tidslinjeIkkeRelevantForVilkåret) {
        return utledTidslinjeForMuligAvkortingFraStartdatoer(startdatoGrunnlag)
            .disjoint(tidslinjeIkkeRelevantForVilkåret);
    }

    static LocalDateTimeline<Boolean> utledTidslinjeForMuligAvkortingFraStartdatoer(StartdatoGrunnlag startdatoGrunnlag) {
        LocalDate sistSøkteStartdatoIBehandlingen = startdatoGrunnlag.getRelevanteStartdatoer().getStartdatoer()
            .stream()
            .map(SøktStartdato::getStartdato)
            .max(Comparator.naturalOrder())
            .orElse(null);
        if (sistSøkteStartdatoIBehandlingen == null) {
            return LocalDateTimeline.empty();
        }
        return AktivitetspengerSøknadsperiodeTjeneste.tidslinjeFraSøktDato(sistSøkteStartdatoIBehandlingen)
            .disjoint(new LocalDateTimeline<>(sistSøkteStartdatoIBehandlingen, sistSøkteStartdatoIBehandlingen, true)); //fjerner startdatoen siden saksbehandler må ta stilling til vilkår på denne datoen
    }
}
