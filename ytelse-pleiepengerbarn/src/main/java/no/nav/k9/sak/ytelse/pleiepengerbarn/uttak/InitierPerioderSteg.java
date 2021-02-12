package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperioderHolder;

@ApplicationScoped
@BehandlingStegRef(kode = "INIT_PERIODER")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class InitierPerioderSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;

    InitierPerioderSteg() {
        // for proxy
    }

    @Inject
    public InitierPerioderSteg(BehandlingRepository behandlingRepository,
                               MottatteDokumentRepository mottatteDokumentRepository,
                               SøknadsperiodeRepository søknadsperiodeRepository,
                               @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);
        var søknadsperiodeGrunnlag = søknadsperiodeRepository.hentGrunnlag(behandlingId);

        if (behandling.erManueltOpprettet()) {
            søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandlingId, søknadsperiodeGrunnlag.orElseThrow().getOppgitteSøknadsperioder());
        } else {
            var kravDokumenterMedPerioder = søknadsfristTjeneste.hentPerioderTilVurdering(referanse);
            var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
                .stream()
                .filter(it -> it.getBehandlingId().equals(behandlingId))
                .map(MottattDokument::getJournalpostId)
                .collect(Collectors.toSet());

            var entries = kravDokumenterMedPerioder.entrySet()
                .stream()
                .filter(it -> mottatteDokumenter.contains(it.getKey().getJournalpostId()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            var relevanteDokumenter = søknadsperiodeGrunnlag.orElseThrow().getOppgitteSøknadsperioder()
                .getPerioder()
                .stream()
                .filter(it -> entries.keySet().stream().map(KravDokument::getJournalpostId).anyMatch(at -> at.getJournalpostId().equals(it.getJournalpostId())))
                .collect(Collectors.toSet());

            var søknadsperioderHolder = new SøknadsperioderHolder(relevanteDokumenter.stream().map(Søknadsperioder::new).collect(Collectors.toSet()));
            søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandlingId, søknadsperioderHolder);

        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
