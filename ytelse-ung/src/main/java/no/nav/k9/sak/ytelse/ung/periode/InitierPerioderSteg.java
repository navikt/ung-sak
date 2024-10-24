package no.nav.k9.sak.ytelse.ung.periode;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.INIT_PERIODER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.ung.registerdata.UngdomsprogramTjeneste;
import no.nav.k9.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperioderHolder;

@ApplicationScoped
@BehandlingStegRef(value = INIT_PERIODER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class InitierPerioderSteg implements BehandlingSteg {

    private UngdomsprogramTjeneste ungdomsprogramTjeneste;
    private BehandlingRepository behandlingRepository;
    private UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;

    @Inject
    public InitierPerioderSteg(UngdomsprogramTjeneste ungdomsprogramTjeneste,
                               BehandlingRepository behandlingRepository,
                               UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository,
                               MottatteDokumentRepository mottatteDokumentRepository) {
        this.ungdomsprogramTjeneste = ungdomsprogramTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    public InitierPerioderSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        ungdomsprogramTjeneste.innhentOpplysninger(kontekst);
        initierRelevanteSøknadsperioder(kontekst);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void initierRelevanteSøknadsperioder(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var søknadsperiodeGrunnlag = søknadsperiodeRepository.hentGrunnlag(behandlingId).orElseThrow();
        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> it.getBehandlingId().equals(behandlingId))
            .filter(it -> it.getType().equals(Brevkode.UNGDOMSYTELSE_SOKNAD))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        var søknadsperioderHolder = mapSøknadsperioderRelevantForBehandlingen(mottatteDokumenter, søknadsperiodeGrunnlag);
        søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandlingId, søknadsperioderHolder);
    }


    /** Lager aggregat av perioder som er relevant for denne behandlingen, altså perioder fra journalposter som har kommet inn i denne behandlingen.
     * @param journalposterMottattIDenneBehandlingen Journalposter som er mottatt i denne behandlingen
     * @param grunnlag Søknadsperiodegrunnlag
     * @return Aggregat for perioder som er relevant for denne behandlingen
     */
    private UngdomsytelseSøknadsperioderHolder mapSøknadsperioderRelevantForBehandlingen(Set<JournalpostId> journalposterMottattIDenneBehandlingen,
                                                                                         UngdomsytelseSøknadsperiodeGrunnlag grunnlag) {
        var relevantePerioder = grunnlag.getOppgitteSøknadsperioder()
            .getPerioder()
            .stream()
            .filter(it -> journalposterMottattIDenneBehandlingen.stream().anyMatch(at -> at.equals(it.getJournalpostId())))
            .collect(Collectors.toSet());

        return new UngdomsytelseSøknadsperioderHolder(relevantePerioder);
    }

}
