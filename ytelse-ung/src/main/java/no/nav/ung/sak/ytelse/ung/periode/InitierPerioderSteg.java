package no.nav.ung.sak.ytelse.ung.periode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.ytelse.ung.registerdata.UngdomsprogramTjeneste;
import no.nav.ung.sak.ytelse.ung.startdatoer.UngdomsytelseSøknadGrunnlag;
import no.nav.ung.sak.ytelse.ung.startdatoer.UngdomsytelseSøknader;
import no.nav.ung.sak.ytelse.ung.startdatoer.UngdomsytelseSøknadsperiodeRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.INIT_PERIODER;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@ApplicationScoped
@BehandlingStegRef(value = INIT_PERIODER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class InitierPerioderSteg implements BehandlingSteg {

    private UngdomsprogramTjeneste ungdomsprogramTjeneste;
    private BehandlingRepository behandlingRepository;
    private UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public InitierPerioderSteg(UngdomsprogramTjeneste ungdomsprogramTjeneste,
                               BehandlingRepository behandlingRepository,
                               UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository,
                               MottatteDokumentRepository mottatteDokumentRepository,
                               UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.ungdomsprogramTjeneste = ungdomsprogramTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }

    public InitierPerioderSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        ungdomsprogramTjeneste.innhentOpplysninger(kontekst);
        initierRelevanteSøknader(kontekst);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void initierRelevanteSøknader(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var søknadsperiodeGrunnlag = søknadsperiodeRepository.hentGrunnlag(behandlingId).orElseThrow();
        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> it.getBehandlingId().equals(behandlingId))
            .filter(it -> it.getType().equals(Brevkode.UNGDOMSYTELSE_SOKNAD))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        var søknadsperioder = mapSøknadsperioderRelevantForBehandlingen(behandling, mottatteDokumenter, søknadsperiodeGrunnlag);
        søknadsperiodeRepository.lagreRelevanteSøknader(behandlingId, søknadsperioder);
    }


    /**
     * Lager aggregat av perioder som er relevant for denne behandlingen, altså perioder fra journalposter som har kommet inn i denne behandlingen.
     *
     * @param behandling
     * @param journalposterMottattIDenneBehandlingen Journalposter som er mottatt i denne behandlingen
     * @param grunnlag                               Søknadsperiodegrunnlag
     * @return Aggregat for perioder som er relevant for denne behandlingen
     */
    private UngdomsytelseSøknader mapSøknadsperioderRelevantForBehandlingen(Behandling behandling, Set<JournalpostId> journalposterMottattIDenneBehandlingen,
                                                                            UngdomsytelseSøknadGrunnlag grunnlag) {

        List<LocalDate> gyldigeStartdatoer = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId())
            .getLocalDateIntervals().stream().map(it -> it.getFomDato()).toList();

        var relevantePerioder = grunnlag.getOppgitteSøknader()
            .getStartdatoer()
            .stream()
            .filter(it -> gyldigeStartdatoer.contains(it.getStartdato())) // Filtrer ut søknader med startdato som ikke matcher ungdomsprogramperioden.
            .filter(it -> journalposterMottattIDenneBehandlingen.stream().anyMatch(at -> at.equals(it.getJournalpostId())))
            .collect(Collectors.toSet());

        return new UngdomsytelseSøknader(relevantePerioder);
    }

}
