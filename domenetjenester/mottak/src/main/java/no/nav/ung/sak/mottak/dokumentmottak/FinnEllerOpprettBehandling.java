package no.nav.ung.sak.mottak.dokumentmottak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.*;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.mottak.Behandlingsoppretter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@Dependent
public class FinnEllerOpprettBehandling {

    private static final Logger log = LoggerFactory.getLogger(FinnEllerOpprettBehandling.class);

    private final Behandlingsoppretter behandlingsoppretter;
    private final BehandlingRevurderingRepository revurderingRepository;
    private final BehandlingRepository behandlingRepository;
    private final BehandlingLåsRepository behandlingLåsRepository;
    private final FagsakProsessTaskRepository fagsakProsessTaskRepository;


    @Inject
    public FinnEllerOpprettBehandling(Behandlingsoppretter behandlingsoppretter,
                                      BehandlingRepositoryProvider repositoryProvider,
                                      BehandlingLåsRepository behandlingLåsRepository, FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.behandlingsoppretter = behandlingsoppretter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingLåsRepository = behandlingLåsRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    @Override
    public BehandlingMedOpprettelseResultat finnEllerOpprettBehandling(Fagsak fagsak, List<Trigger> triggere) {
        var fagsakId = fagsak.getId();
        final var erKontrollbehandling = triggere.stream().map(Trigger::behandlingÅrsak).anyMatch(it -> Set.of(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT).contains(it));
        Optional<Behandling> sisteYtelsesbehandling = finnSisteBehandling(fagsak, erKontrollbehandling);


        if (sisteYtelsesbehandling.isEmpty()) {
            // ingen tidligere behandling - Opprett ny førstegangsbehandling
            log.info("Ingen tidligere behandling for fagsak {}, oppretter ny førstegangsbehandling", fagsakId);
            Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
            return BehandlingMedOpprettelseResultat.nyBehandling(behandling);
        } else {
            var sisteBehandling = sisteYtelsesbehandling.get();
            sjekkBehandlingKanLåses(sisteBehandling); // sjekker at kan låses (dvs ingen andre prosesserer den samtidig, hvis ikke kommer vi tilbake senere en gang)
            if (erBehandlingAvsluttet(sisteYtelsesbehandling)) {
                // siste behandling er avsluttet, oppretter ny behandling
                Optional<Behandling> sisteAvsluttetBehandling = finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId, erKontrollbehandling);
                sisteBehandling = sisteAvsluttetBehandling.orElse(sisteBehandling);

                if (erKontrollbehandling) {

                }

                // Håndter avsluttet behandling
                var sisteHenlagteFørstegangsbehandling = erKontrollbehandling ? Optional.empty() : behandlingsoppretter.sisteHenlagteFørstegangsbehandling(sisteBehandling.getFagsak());
                if (sisteHenlagteFørstegangsbehandling.isPresent()) {
                    // oppretter ny behandling når siste var henlagt førstegangsbehandling
                    var nyFørstegangsbehandling = behandlingsoppretter.opprettNyFørstegangsbehandling(sisteHenlagteFørstegangsbehandling.get().getFagsak(), sisteHenlagteFørstegangsbehandling.get());
                    return BehandlingMedOpprettelseResultat.nyBehandling(nyFørstegangsbehandling);
                } else {
                    // oppretter ny behandling fra forrige (førstegangsbehandling eller revurdering)
                    var nyBehandling = behandlingsoppretter.opprettNyBehandlingFra(sisteBehandling, triggere.getFirst().behandlingÅrsak());
                    return BehandlingMedOpprettelseResultat.nyBehandling(nyBehandling);
                }
            } else {
                sjekkBehandlingKanHoppesTilbake(sisteBehandling);
                sjekkBehandlingHarIkkeÅpneTasks(sisteBehandling);
                return BehandlingMedOpprettelseResultat.eksisterendeBehandling(sisteBehandling);
            }
        }
    }

    private Optional<Behandling> finnSisteAvsluttedeIkkeHenlagteBehandling(Long fagsakId, boolean erKontrollbehandling) {
        return erKontrollbehandling ? behandlingRepository.finnSisteAvsluttedeIkkeHenlagteKontrollbehandling(fagsakId) : behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
    }

    private Optional<Behandling> finnSisteBehandling(Fagsak fagsak, boolean erKontrollbehandling) {
        return erKontrollbehandling ? behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.KONTROLLBEHANDLING) : revurderingRepository.hentSisteBehandling(fagsak.getId());
    }

    private Boolean erBehandlingAvsluttet(Optional<Behandling> sisteYtelsesbehandling) {
        return sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.FALSE);
    }

    private void sjekkBehandlingKanLåses(Behandling behandling) {
        int forsøk = 3;

        BehandlingLås lås;
        while (--forsøk >= 0) {
            lås = behandlingLåsRepository.taLåsHvisLedig(behandling.getId());
            if (lås != null) {
                return; // OK - Fikk lås
            }
            try {
                Thread.sleep(1 * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // noen andre holder på siden vi ikke fikk fatt på lås, så avbryter denne gang
        throw DokumentmottakMidlertidigFeil.FACTORY.behandlingPågårAvventerKnytteMottattDokumentTilBehandling(behandling.getId()).toException();
    }

    private void sjekkBehandlingKanHoppesTilbake(Behandling behandling) {
        boolean underIverksetting = behandling.getStatus() == BehandlingStatus.IVERKSETTER_VEDTAK;
        if (underIverksetting) {
            //vedtak er fattet og behandlingen kan derfor ikke oppdateres. Må vente til behandlingen er avsluttet, og det vil så opprettes ny behandling når dokumentet sendes på nytt
            throw DokumentmottakMidlertidigFeil.FACTORY.behandlingUnderIverksettingAvventerKnytteMottattDokumentTilBehandling(behandling.getId()).toException();
        }
    }

    private void sjekkBehandlingHarIkkeÅpneTasks(Behandling behandling) {
        final Set<ProsessTaskStatus> aktuelleStatuser = EnumSet.of(ProsessTaskStatus.KLAR, ProsessTaskStatus.VENTER_SVAR, ProsessTaskStatus.VETO);
        final LocalDateTime fom = Tid.TIDENES_BEGYNNELSE.atStartOfDay();
        final LocalDateTime tom = Tid.TIDENES_ENDE.plusDays(1).atStartOfDay();
        //merk at denne bare finner tasks med gruppesekvensnummer != null (hindrer at den finner seg selv eller andre av typen innhentsaksopplysninger.håndterMottattDokument)
        final List<ProsessTaskData> åpneTasks = fagsakProsessTaskRepository.finnAlleForAngittSøk(behandling.getFagsakId(), behandling.getId().toString(), null, aktuelleStatuser, true, fom, tom);
        if (!åpneTasks.isEmpty()) {
            //behandlingen har åpne tasks og mottak av dokument kan føre til parallelle prosesser som går i beina på hverandre
            log.info("Fant følgende åpne tasks: [" + åpneTasks.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]");
            throw DokumentmottakMidlertidigFeil.FACTORY.behandlingPågårAvventerKnytteMottattDokumentTilBehandling(behandling.getId()).toException();
        }
    }


}
