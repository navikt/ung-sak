package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
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
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.mottak.dokumentmottak.Kompletthetskontroller;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(Brevkode.INNTEKTSMELDING_KODE)
public class DokumentmottakerInntektsmelding implements Dokumentmottaker {

    private Behandlingsoppretter behandlingsoppretter;
    private Kompletthetskontroller kompletthetskontroller;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private DokumentmottakerFelles dokumentMottakerFelles;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingRepository behandlingRepository;

    private BehandlingLåsRepository behandlingLåsRepository;

    private InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    DokumentmottakerInntektsmelding() {
        // for CDI
    }

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentMottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           BehandlingRepositoryProvider repositoryProvider) {
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;

        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
    }

    @Override
    public void mottaDokument(Collection<MottattDokument> mottattDokument, Fagsak fagsak) {
        doMottaDokument(fagsak, mottattDokument);
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument, FagsakYtelseType ytelseType) {
        inntektsmeldingParser.parseInntektsmeldinger(mottattDokument);
    }

    void asynkVurderKompletthetForÅpenBehandling(Behandling behandling) { // #I2
        dokumentMottakerFelles.leggTilBehandlingsårsak(behandling, getBehandlingÅrsakType());
        dokumentMottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyInntektsmelding(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT);
        kompletthetskontroller.asynkVurderKompletthet(behandling);
    }

    protected BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    private void doMottaDokument(Fagsak fagsak, Collection<MottattDokument> mottattDokument) {
        var fagsakId = fagsak.getId();
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());

        if (sisteYtelsesbehandling.isEmpty()) {
            // ingen tidligere behandling - Opprett ny førstegangsbehandling
            Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
            lagreDokumenter(mottattDokument, behandling);
            asynkStartBehandling(behandling);
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
                    lagreDokumenter(mottattDokument, nyFørstegangsbehandling);
                    asynkStartBehandling(nyFørstegangsbehandling);
                } else {
                    // oppretter ny behandling fra forrige (førstegangsbehandling eller revurdering)
                    var nyBehandling = behandlingsoppretter.opprettNyBehandlingFra(sisteBehandling, getBehandlingÅrsakType());
                    lagreDokumenter(mottattDokument, nyBehandling);
                    asynkStartBehandling(nyBehandling);
                }
            } else {
                lagreDokumenter(mottattDokument, sisteBehandling);
                asynkVurderKompletthetForÅpenBehandling(sisteBehandling);
            }
        }
    }

    private void lagreDokumenter(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        mottatteDokumentTjeneste.persisterInntektsmeldingForBehandling(behandling, mottattDokument);
        mottattDokument.forEach(m -> dokumentMottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), m.getJournalpostId(), m.getType()));
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
}
