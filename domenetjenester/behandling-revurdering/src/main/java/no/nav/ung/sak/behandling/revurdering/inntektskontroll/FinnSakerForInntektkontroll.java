package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.vilkår.VilkårTjeneste;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT;

@ApplicationScoped
public class FinnSakerForInntektkontroll {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private VilkårTjeneste vilkårTjeneste;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    FinnSakerForInntektkontroll() {
    }

    @Inject
    public FinnSakerForInntektkontroll(BehandlingRepository behandlingRepository,
                                       FagsakRepository fagsakRepository,
                                       UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                       ProsessTriggereRepository prosessTriggereRepository,
                                       VilkårTjeneste vilkårTjeneste,
                                       UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    List<Fagsak> finnFagsaker(LocalDate fom, LocalDate tom) {
        var fagsaker = fagsakRepository.hentAlleFagsakerSomOverlapper(fom, tom);

        var behandlinger = fagsaker.stream().map(Fagsak::getId).map(fagsakId -> {
            var avsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
            if (avsluttetBehandling.isPresent()) {
                return avsluttetBehandling;
            }
            return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        }).flatMap(Optional::stream).toList();

        /* Filtrere ut behandlinger som skal ha inntektskontroll:
            - Har ikke allerede opprettet trigger for inntektskontroll
            - Har programdeltagelse og den startet ikke forrige måned. Og slutter ikke i inneværende måned
            - Har ikke avslåtte vilkår, men kan ha vilkår som er til vurdering
            - Har ikke avslått uttak, men kan ha uttak som er til vurdering
         */
        var behandlingerTilKontroll = behandlinger.stream()
            .filter(behandling -> harIkkeOpprettetTrigger(behandling, fom, tom))
            .filter(behandling -> harProgramdeltagelseSomGirInntektskontroll(behandling, fom, tom))
            .filter(behandling -> harIkkeAvslåtteVilkår(behandling, fom, tom))
            .filter(behandling -> harIkkeAvslåttUttak(behandling, fom, tom))
            .toList();
        return behandlingerTilKontroll.stream().map(Behandling::getFagsak).toList();
    }

    private boolean harIkkeAvslåttUttak(Behandling behandling, LocalDate fom, LocalDate tom) {
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId());
        if (ungdomsytelseGrunnlag.isEmpty()) {
            // Hvis ikke vurdert uttak
            return true;
        }
        return ungdomsytelseGrunnlag.get().getAvslagstidslinjeFraUttak().filterValue(it -> it.avslagsårsak() != null).intersection(new LocalDateInterval(fom, tom)).isEmpty();
    }

    private boolean harIkkeAvslåtteVilkår(Behandling behandling, LocalDate fom, LocalDate tom) {
        var samletResultat = vilkårTjeneste.samletVilkårsresultat(behandling.getId()).intersection(new LocalDateInterval(fom, tom));
        var harVilkårSomIkkeErVurdert = samletResultat.toSegments().stream().anyMatch(segment -> segment.getValue().getSamletUtfall().equals(Utfall.IKKE_VURDERT));
        if (samletResultat.isEmpty() || harVilkårSomIkkeErVurdert) {
            //førstegangsbehandling som ikke har fått behandlet vilkår
            return true;
        }
        return erInnvilget(samletResultat);
    }

    private static boolean erInnvilget(LocalDateTimeline<VilkårUtfallSamlet> samletResultat) {
        return samletResultat.toSegments().stream().noneMatch(segment -> segment.getValue().getSamletUtfall().equals(Utfall.IKKE_OPPFYLT));
    }

    // Inntektsrapportering gjelder bare fra måned nr 2 og inkluderer ikke evt. opphørsmåned
    private boolean harProgramdeltagelseSomGirInntektskontroll(Behandling behandling, LocalDate fom, LocalDate tom) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
        if (ungdomsprogramPeriodeGrunnlag.isEmpty()) {
            return false;
        }

        var perioder = ungdomsprogramPeriodeGrunnlag.get().getUngdomsprogramPerioder().getPerioder();
        if (perioder.isEmpty()) {
            return false;
        }
        var startdato = perioder.stream().map(p -> p.getPeriode().getFomDato()).min(Comparator.naturalOrder()).orElseThrow();
        var sluttdato = perioder.stream().map(p -> p.getPeriode().getTomDato()).max(Comparator.naturalOrder()).orElseThrow();
        return startdato.isBefore(fom) && sluttdato.isAfter(tom);
    }

    private boolean harIkkeOpprettetTrigger(Behandling behandling, LocalDate fom, LocalDate tom) {
        return prosessTriggereRepository.hentGrunnlag(behandling.getId()).stream().flatMap(it -> it.getTriggere().stream()).noneMatch(it -> it.getÅrsak().equals(RE_KONTROLL_REGISTER_INNTEKT) && it.getPeriode().overlapper(fom, tom));
    }

}
