package no.nav.ung.ytelse.aktivitetspenger.prosess.initperioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.INIT_PERIODER;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;

@ApplicationScoped
@BehandlingStegRef(value = INIT_PERIODER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(AKTIVITETSPENGER)
public class InitierPerioderSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;

    @Inject
    public InitierPerioderSteg(BehandlingRepository behandlingRepository,
                               MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    public InitierPerioderSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        initierRelevanteSøknader(kontekst);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void initierRelevanteSøknader(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> it.getBehandlingId().equals(behandlingId))
            .filter(it -> it.getType().equals(Brevkode.AKTIVITETSPENGER_SOKNAD))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        //utlede perioder

    }


}
