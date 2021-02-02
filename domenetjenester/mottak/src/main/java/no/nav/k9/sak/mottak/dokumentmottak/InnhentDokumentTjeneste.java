package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.inntektsmelding.MottattInntektsmeldingFeil;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@Dependent
public class InnhentDokumentTjeneste {

    private Instance<Dokumentmottaker> mottakere;

    private Behandlingsoppretter behandlingsoppretter;
    private Kompletthetskontroller kompletthetskontroller;
    private DokumentmottakerFelles dokumentMottakerFelles;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingRepository behandlingRepository;

    private BehandlingLåsRepository behandlingLåsRepository;

    @Inject
    public InnhentDokumentTjeneste(@Any Instance<Dokumentmottaker> mottakere,
                                   DokumentmottakerFelles dokumentMottakerFelles,
                                   Behandlingsoppretter behandlingsoppretter,
                                   Kompletthetskontroller kompletthetskontroller,
                                   BehandlingRepositoryProvider repositoryProvider) {
        this.mottakere = mottakere;
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
        Dokumentmottaker dokumentmottaker = getDokumentmottaker(mottattDokument, behandling.getFagsak());
        dokumentmottaker.lagreDokumentinnhold(mottattDokument, behandling);
    }


    void asynkVurderKompletthetForÅpenBehandling(Behandling behandling, BehandlingÅrsakType behandlingÅrsak) {
        dokumentMottakerFelles.leggTilBehandlingsårsak(behandling, behandlingÅrsak);
        dokumentMottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyInntektsmelding(behandling, behandlingÅrsak);
        kompletthetskontroller.asynkVurderKompletthet(behandling);
    }

    private BehandlingÅrsakType getBehandlingÅrsakType(Collection<MottattDokument> mottattDokument, Fagsak fagsak) {
        var dokumentmottaker = getDokumentmottaker(mottattDokument, fagsak);
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
            throw MottattInntektsmeldingFeil.FACTORY.behandlingPågårAvventerKnytteMottattDokumentTilBehandling(behandling.getId()).toException();
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

    private Dokumentmottaker getDokumentmottaker(Collection<MottattDokument> mottattDokument, Fagsak fagsak) {
        var brevkode = DokumentBrevkodeUtil.unikBrevkode(mottattDokument);
        return finnMottaker(brevkode, fagsak.getYtelseType());
    }

    private Dokumentmottaker finnMottaker(Brevkode brevkode, FagsakYtelseType fagsakYtelseType) {
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(brevkode.getKode()));

        return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Dokumentmottaker for ytelseType=" + fagsakYtelseTypeKode + ", dokumentgruppe=" + brevkode));
    }
}
