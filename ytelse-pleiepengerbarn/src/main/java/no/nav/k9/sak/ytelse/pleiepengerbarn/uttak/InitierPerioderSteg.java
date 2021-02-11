package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;

@ApplicationScoped
@BehandlingStegRef(kode = "INIT_PERIODER")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class InitierPerioderSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UttakRepository uttakRepository;

    protected InitierPerioderSteg() {
        // for proxy
    }

    @Inject
    public InitierPerioderSteg(BehandlingRepository behandlingRepository,
                               SøknadsperiodeRepository søknadsperiodeRepository,
                               MottatteDokumentRepository mottatteDokumentRepository,
                               UttakRepository uttakRepository) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.uttakRepository = uttakRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        if (behandling.erManueltOpprettet()) {
            var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
                .stream()
                .map(MottattDokument::getJournalpostId)
                .collect(Collectors.toSet());

            var relevanteSøknadsPerioder = søknadsperiodeRepository.hentPerioderKnyttetTilJournalpost(behandling.getId(), mottatteDokumenter);
            // Tar søknadsperioder fra søknader ankommet på fagsaken etter kronologisk rekkefølge og lagrer ned på søknadsperiodegrunnlag elns
        } else {
            var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
                .stream()
                .filter(it -> it.getBehandlingId().equals(behandlingId))
                .map(MottattDokument::getJournalpostId)
                .collect(Collectors.toSet());

            var relevanteSøknadsPerioder = søknadsperiodeRepository.hentPerioderKnyttetTilJournalpost(behandling.getId(), mottatteDokumenter);
            // Tar søknadsperioder fra søknader ankommet på behandlingen og lagrer ned på søknadsperiodegrunnlag elns
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
