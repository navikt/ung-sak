package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.inntektsmelding.MottattInntektsmeldingException;

@Dependent
public class InnhentDokumentTjeneste {

    private Instance<Dokumentmottaker> mottakere;

    private Behandlingsoppretter behandlingsoppretter;
    private Kompletthetskontroller kompletthetskontroller;
    private DokumentmottakerFelles dokumentMottakerFelles;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingRepository behandlingRepository;

    private BehandlingLåsRepository behandlingLåsRepository;
    private ProsessTaskRepository taskRepository;

    @Inject
    public InnhentDokumentTjeneste(@Any Instance<Dokumentmottaker> mottakere,
                                   DokumentmottakerFelles dokumentMottakerFelles,
                                   Behandlingsoppretter behandlingsoppretter,
                                   Kompletthetskontroller kompletthetskontroller,
                                   BehandlingRepositoryProvider repositoryProvider,
                                   ProsessTaskRepository taskRepository) {
        this.mottakere = mottakere;
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
        this.taskRepository = taskRepository;
    }

    public void mottaDokument(Fagsak fagsak, Collection<MottattDokument> mottattDokument) {
        var brevkodeMap = mottattDokument
            .stream()
            .collect(Collectors.groupingBy(MottattDokument::getType));
        var behandlingÅrsak = brevkodeMap.keySet()
            .stream()
            .sorted(Brevkode.COMP_REKKEFØLGE)
            .map(it -> getBehandlingÅrsakType(it, fagsak))
            .findFirst()
            .orElseThrow();

        var resultat = finnEllerOpprettBehandling(fagsak, behandlingÅrsak);

        ProsessTaskData startTask;
        if (resultat.nyopprettet) {
            startTask = asynkStartBehandling(resultat.behandling);
        } else if (prosessenStårStillePåAksjonspunktForSøknadsfrist(resultat.behandling)) {
            startTask = restartBehandling(resultat.behandling, behandlingÅrsak);
        } else {
            startTask = asynkVurderKompletthetForÅpenBehandling(resultat.behandling, behandlingÅrsak);
        }
        lagreDokumenter(brevkodeMap, resultat.behandling);

        taskRepository.lagre(startTask);
    }

    private ProsessTaskData restartBehandling(Behandling behandling, BehandlingÅrsakType behandlingÅrsak) {
        dokumentMottakerFelles.leggTilBehandlingsårsak(behandling, behandlingÅrsak);
        dokumentMottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyInntektsmelding(behandling, behandlingÅrsak);
        return dokumentMottakerFelles.opprettTaskForÅSpoleTilbakeTilStartOgStartePåNytt(behandling);
    }

    private boolean prosessenStårStillePåAksjonspunktForSøknadsfrist(Behandling behandling) {
        return BehandlingStegType.VURDER_SØKNADSFRIST.equals(behandling.getAktivtBehandlingSteg())
            && (behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE).map(Aksjonspunkt::erÅpentAksjonspunkt).orElse(false)
            || behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE).map(Aksjonspunkt::erÅpentAksjonspunkt).orElse(false));
    }

    private BehandlingMedOpprettelseResultat finnEllerOpprettBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var fagsakId = fagsak.getId();
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteBehandling(fagsak.getId());

        if (sisteYtelsesbehandling.isEmpty()) {
            // ingen tidligere behandling - Opprett ny førstegangsbehandling
            Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
            return BehandlingMedOpprettelseResultat.nyBehandling(behandling);
        } else {
            var sisteBehandling = sisteYtelsesbehandling.get();
            sjekkBehandlingKanLåses(sisteBehandling); // sjekker at kan låses (dvs ingen andre prosesserer den samtidig, hvis ikke kommer vi tilbake senere en gang)
            if (erBehandlingAvsluttet(sisteYtelsesbehandling)) {
                // siste behandling er avsluttet, oppretter ny behandling
                Optional<Behandling> sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
                sisteBehandling = sisteAvsluttetBehandling.orElse(sisteBehandling);

                // Håndter avsluttet behandling
                var sisteHenlagteFørstegangsbehandling = behandlingsoppretter.sisteHenlagteFørstegangsbehandling(sisteBehandling.getFagsak());
                if (sisteHenlagteFørstegangsbehandling.isPresent()) {
                    // oppretter ny behandling når siste var henlagt førstegangsbehandling
                    var nyFørstegangsbehandling = behandlingsoppretter.opprettNyFørstegangsbehandling(sisteHenlagteFørstegangsbehandling.get().getFagsak(), sisteHenlagteFørstegangsbehandling.get());
                    return BehandlingMedOpprettelseResultat.nyBehandling(nyFørstegangsbehandling);
                } else {
                    // oppretter ny behandling fra forrige (førstegangsbehandling eller revurdering)
                    var nyBehandling = behandlingsoppretter.opprettNyBehandlingFra(sisteBehandling, behandlingÅrsakType);
                    return BehandlingMedOpprettelseResultat.nyBehandling(nyBehandling);
                }
            } else {
                return BehandlingMedOpprettelseResultat.eksisterendeBehandling(sisteBehandling);
            }
        }
    }

    public void lagreDokumenter(Map<Brevkode, List<MottattDokument>> mottattDokument, Behandling behandling) {
        mottattDokument.keySet()
            .stream()
            .sorted(Brevkode.COMP_REKKEFØLGE)
            .forEach(key -> {
                Dokumentmottaker dokumentmottaker = getDokumentmottaker(key, behandling.getFagsak());
                dokumentmottaker.lagreDokumentinnhold(mottattDokument.get(key), behandling);
            });
    }

    ProsessTaskData asynkVurderKompletthetForÅpenBehandling(Behandling behandling, BehandlingÅrsakType behandlingÅrsak) {
        dokumentMottakerFelles.leggTilBehandlingsårsak(behandling, behandlingÅrsak);
        dokumentMottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyInntektsmelding(behandling, behandlingÅrsak);
        return kompletthetskontroller.asynkVurderKompletthet(behandling);
    }

    private BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode, Fagsak fagsak) {
        var dokumentmottaker = getDokumentmottaker(brevkode, fagsak);
        return dokumentmottaker.getBehandlingÅrsakType(brevkode);
    }

    private ProsessTaskData asynkStartBehandling(Behandling behandling) {
        return dokumentMottakerFelles.opprettTaskForÅStarteBehandling(behandling);
    }

    private Boolean erBehandlingAvsluttet(Optional<Behandling> sisteYtelsesbehandling) {
        return sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.FALSE);
    }

    private void sjekkBehandlingKanLåses(Behandling behandling) {
        int forsøk = 3;

        BehandlingLås lås = null;
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
        throw MottattInntektsmeldingException.FACTORY.behandlingPågårAvventerKnytteMottattDokumentTilBehandling(behandling.getId());
    }

    private Dokumentmottaker getDokumentmottaker(Brevkode brevkode, Fagsak fagsak) {
        return finnMottaker(brevkode, fagsak.getYtelseType());
    }

    private Dokumentmottaker finnMottaker(Brevkode brevkode, FagsakYtelseType fagsakYtelseType) {
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(brevkode.getKode()));

        return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Dokumentmottaker for ytelseType=" + fagsakYtelseTypeKode + ", dokumentgruppe=" + brevkode));
    }

    private static class BehandlingMedOpprettelseResultat {
        private Behandling behandling;
        private boolean nyopprettet;

        private BehandlingMedOpprettelseResultat(Behandling behandling, boolean nyopprettet) {
            this.behandling = behandling;
            this.nyopprettet = nyopprettet;
        }

        private static BehandlingMedOpprettelseResultat nyBehandling(Behandling behandling) {
            return new BehandlingMedOpprettelseResultat(behandling, true);
        }

        private static BehandlingMedOpprettelseResultat eksisterendeBehandling(Behandling behandling) {
            return new BehandlingMedOpprettelseResultat(behandling, false);
        }
    }
}
