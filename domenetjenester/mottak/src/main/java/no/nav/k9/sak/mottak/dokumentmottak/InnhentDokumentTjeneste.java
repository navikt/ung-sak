package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.inntektsmelding.MottattInntektsmeldingException;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@Dependent
public class InnhentDokumentTjeneste {

    private DokumentmottakerProvider dokumentmottakerProvider;

    private Behandlingsoppretter behandlingsoppretter;
    private Kompletthetskontroller kompletthetskontroller;
    private DokumentmottakerFelles dokumentMottakerFelles;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingRepository behandlingRepository;

    private BehandlingLåsRepository behandlingLåsRepository;

    @Inject
    public InnhentDokumentTjeneste(DokumentmottakerProvider dokumentmottakerProvider,
                                   DokumentmottakerFelles dokumentMottakerFelles,
                                   Behandlingsoppretter behandlingsoppretter,
                                   Kompletthetskontroller kompletthetskontroller,
                                   BehandlingRepositoryProvider repositoryProvider) {
        this.dokumentmottakerProvider = dokumentmottakerProvider;
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
    }

    public void mottaDokument(Fagsak fagsak, Collection<MottattDokument> mottattDokument) {
        var behandlingÅrsak = getBehandlingÅrsakType(mottattDokument, fagsak);
        var resultat = finnEllerOpprettBehandling(fagsak, behandlingÅrsak);

        lagreDokumenter(mottattDokument, resultat.behandling);
        if (resultat.nyopprettet) {
            asynkStartBehandling(resultat.behandling);
        } else {
            asynkVurderKompletthetForÅpenBehandling(resultat.behandling, behandlingÅrsak);
        }
    }

    private BehandlingMedOpprettelseResultat finnEllerOpprettBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var fagsakId = fagsak.getId();
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());

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

    public void lagreDokumenter(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        Dokumentmottaker dokumentmottaker = getDokumentmottaker(mottattDokument);
        dokumentmottaker.lagreDokumentinnhold(mottattDokument, behandling);
    }


    void asynkVurderKompletthetForÅpenBehandling(Behandling behandling, BehandlingÅrsakType behandlingÅrsak) {
        dokumentMottakerFelles.leggTilBehandlingsårsak(behandling, behandlingÅrsak);
        dokumentMottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyInntektsmelding(behandling, behandlingÅrsak);
        kompletthetskontroller.asynkVurderKompletthet(behandling);
    }

    private BehandlingÅrsakType getBehandlingÅrsakType(Collection<MottattDokument> mottattDokument, Fagsak fagsak) {
        var dokumentmottaker = getDokumentmottaker(mottattDokument);
        var behandlingÅrsakType = dokumentmottaker.getBehandlingÅrsakType();
        return behandlingÅrsakType;
    }

    private void asynkStartBehandling(Behandling behandling) {
        dokumentMottakerFelles.opprettTaskForÅStarteBehandling(behandling);
    }

    private Boolean erBehandlingAvsluttet(Optional<Behandling> sisteYtelsesbehandling) {
        return sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.FALSE);
    }

    private void sjekkBehandlingKanLåses(Behandling behandling) {
        var lås = behandlingLåsRepository.taLåsHvisLedig(behandling.getId());
        if (lås == null) {
            // noen andre holder på siden vi ikke fikk fatt på lås, så avbryter denne gang
            throw MottattInntektsmeldingException.FACTORY.behandlingPågårAvventerKnytteMottattDokumentTilBehandling(behandling.getId());
        }
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

    private Dokumentmottaker getDokumentmottaker(Collection<MottattDokument> mottattDokument) {
        return dokumentmottakerProvider.getDokumentmottaker(mottattDokument);
    }
}
