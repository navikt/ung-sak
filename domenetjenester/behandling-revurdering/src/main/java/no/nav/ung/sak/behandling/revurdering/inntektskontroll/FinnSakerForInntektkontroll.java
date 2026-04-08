package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class FinnSakerForInntektkontroll {

    private static final Logger LOG = LoggerFactory.getLogger(FinnSakerForInntektkontroll.class);
    private static final Set<FagsakYtelseType> STØTTEDE_YTELSE_TYPER = Set.of(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårTjeneste vilkårTjeneste;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;

    FinnSakerForInntektkontroll() {
    }

    @Inject
    public FinnSakerForInntektkontroll(BehandlingRepository behandlingRepository,
                                       FagsakRepository fagsakRepository,
                                       VilkårTjeneste vilkårTjeneste,
                                       TilkjentYtelseRepository tilkjentYtelseRepository,
                                       RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.relevanteKontrollperioderUtleder = relevanteKontrollperioderUtleder;
    }

    List<Fagsak> finnFagsaker(LocalDate fom, LocalDate tom) {
        var fagsaker = fagsakRepository.hentAlleFagsakerSomOverlapper(fom, tom).stream()
            .filter(f -> STØTTEDE_YTELSE_TYPER.contains(f.getYtelseType()))
            .toList();

        var behandlinger = fagsaker.stream().map(Fagsak::getId).map(fagsakId -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId)).flatMap(Optional::stream).toList();

        /* Filtrere ut behandlinger som skal ha inntektskontroll:
            - Har ikke allerede opprettet trigger for inntektskontroll
            - Har programdeltagelse og den startet ikke forrige måned. Og slutter ikke i inneværende måned
            - Har ikke avslåtte vilkår, men kan ha vilkår som er til vurdering
            - Har ikke avslått uttak, men kan ha uttak som er til vurdering
            - Har ikke utført kontroll for perioden tidligere
         */
        var behandlingerTilKontroll = behandlinger.stream()
            .filter(behandling -> erIkkeAlleredeMarkertForKontroll(behandling, fom, tom))
            .filter(behandling -> harPeriodeSomGirInntektskontroll(behandling, fom, tom))
            .filter(behandling -> harIkkeAvslåtteVilkår(behandling, fom, tom))
            .filter(behandling -> harIkkeUtførtKontroll(behandling, fom, tom))
            .toList();
        return behandlingerTilKontroll.stream().map(Behandling::getFagsak).toList();
    }

    private boolean harIkkeAvslåtteVilkår(Behandling behandling, LocalDate fom, LocalDate tom) {
        var samletResultat = vilkårTjeneste.samletVilkårsresultat(behandling.getId()).intersection(new LocalDateInterval(fom, tom));
        var harVilkårSomIkkeErVurdert = samletResultat.toSegments().stream().anyMatch(segment -> segment.getValue().getSamletUtfall().equals(Utfall.IKKE_VURDERT));
        if (samletResultat.isEmpty() || harVilkårSomIkkeErVurdert) {
            //behandling som ikke har fått behandlet vilkår
            LOG.warn("Behandling {} for Fagsak {} har ikke vilkår vurdert i perioden, oppretter likevel kontroll av inntekt", behandling.getId(), behandling.getFagsak().getId());
            return true;
        }
        return erInnvilget(samletResultat);
    }

    private static boolean erInnvilget(LocalDateTimeline<VilkårUtfallSamlet> samletResultat) {
        return samletResultat.toSegments().stream().noneMatch(segment -> segment.getValue().getSamletUtfall().equals(Utfall.IKKE_OPPFYLT));
    }

    // Inntektsrapportering gjelder bare fra måned nr 2 og inkluderer ikke evt. opphørsmåned
    private boolean harPeriodeSomGirInntektskontroll(Behandling behandling, LocalDate fom, LocalDate tom) {
        var relevantePerioderForKontroll = relevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(behandling.getId());
        return !relevantePerioderForKontroll.intersection(new LocalDateInterval(fom, tom)).isEmpty();
    }

    private boolean erIkkeAlleredeMarkertForKontroll(Behandling behandling, LocalDate fom, LocalDate tom) {
        return relevanteKontrollperioderUtleder.utledPerioderForKontrollAvInntekt(behandling.getId()).intersection(new LocalDateInterval(fom, tom)).isEmpty();
    }

    private boolean harIkkeUtførtKontroll(Behandling behandling, LocalDate fom, LocalDate tom) {
        var kontrollertInntektTidslinje = tilkjentYtelseRepository.hentKontrollerInntektTidslinje(behandling.getId());
        return kontrollertInntektTidslinje.intersection(new LocalDateInterval(fom, tom)).isEmpty();
    }


}
