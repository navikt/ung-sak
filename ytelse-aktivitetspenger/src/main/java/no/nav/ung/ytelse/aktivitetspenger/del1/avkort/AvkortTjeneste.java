package no.nav.ung.ytelse.aktivitetspenger.del1.avkort;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerSøknadsperiodeTjeneste;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;

@Dependent
public class AvkortTjeneste {

    private final StartdatoRepository startdatoRepository;
    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public AvkortTjeneste(StartdatoRepository startdatoRepository, VilkårResultatRepository vilkårResultatRepository) {
        this.startdatoRepository = startdatoRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public LocalDateTimeline<Boolean> utledTidslinjeForMuligAvkorting(Long behandlingId, VilkårType vilkårType) {
        if (!Set.of(VilkårType.BOSTEDSVILKÅR, VilkårType.BISTANDSVILKÅR, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR).contains(vilkårType)) {
            throw new IllegalArgumentException("AvkortTjeneste er tenkt brukt for vilkårtyper knyttet til nav-kontor-vilkår. Endre dette ved behov.");
        }

        StartdatoGrunnlag startdatoGrunnlagOpt = startdatoRepository.hentGrunnlag(behandlingId).orElse(null);
        LocalDateTimeline<VilkårPeriode> tidslinjeIkkeRelevantForVilkåret = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .map(vilkårene -> vilkårene.getVilkårTimeline(vilkårType).filterValue(vilkårperiode -> vilkårperiode.getUtfall() == Utfall.IKKE_RELEVANT))
            .orElse(LocalDateTimeline.empty());
        return utledTidslinjeForMuligAvkorting(startdatoGrunnlagOpt, tidslinjeIkkeRelevantForVilkåret);
    }

    public void validerAvkortBruktRiktig(Long behandlingId, LocalDateTimeline<?> perioderSattTilAvkortet, VilkårType vilkårType) {
        LocalDateTimeline<Boolean> perioderSomKanSettesTilAvkortet = utledTidslinjeForMuligAvkorting(behandlingId, vilkårType);
        LocalDateTimeline<Boolean> manglendeOverlapp = perioderSomKanSettesTilAvkortet.disjoint(perioderSattTilAvkortet);
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
        if (startdatoGrunnlag == null) {
            return LocalDateTimeline.empty();
        }
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
