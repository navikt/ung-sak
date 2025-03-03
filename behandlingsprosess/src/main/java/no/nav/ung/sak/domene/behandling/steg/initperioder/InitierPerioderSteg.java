package no.nav.ung.sak.domene.behandling.steg.initperioder;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.INIT_PERIODER;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramTjeneste;

@ApplicationScoped
@BehandlingStegRef(value = INIT_PERIODER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class InitierPerioderSteg implements BehandlingSteg {

    /**
     * Angir maksimalt antall tillatte dager mellom startdato for ungdomsprogrammet og startdato oppgitt i søknad
     */
    public static final int MAKS_GODKJENT_AVSTAND_TIL_STARTDATO = 56;
    private UngdomsprogramTjeneste ungdomsprogramTjeneste;
    private BehandlingRepository behandlingRepository;
    private UngdomsytelseStartdatoRepository startdatoRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public InitierPerioderSteg(UngdomsprogramTjeneste ungdomsprogramTjeneste,
                               BehandlingRepository behandlingRepository,
                               UngdomsytelseStartdatoRepository startdatoRepository,
                               MottatteDokumentRepository mottatteDokumentRepository,
                               UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.ungdomsprogramTjeneste = ungdomsprogramTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.startdatoRepository = startdatoRepository;
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
        var søknadsperiodeGrunnlag = startdatoRepository.hentGrunnlag(behandlingId).orElseThrow();
        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> it.getBehandlingId().equals(behandlingId))
            .filter(it -> it.getType().equals(Brevkode.UNGDOMSYTELSE_SOKNAD))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        var søknadsperioder = mapStartdatoerRelevantForBehandlingen(behandling, mottatteDokumenter, søknadsperiodeGrunnlag);
        startdatoRepository.lagreRelevanteSøknader(behandlingId, søknadsperioder);
    }


    /**
     * Lager aggregat av perioder som er relevant for denne behandlingen, altså perioder fra journalposter som har kommet inn i denne behandlingen.
     *
     * @param behandling
     * @param journalposterMottattIDenneBehandlingen Journalposter som er mottatt i denne behandlingen
     * @param grunnlag                               Søknadsperiodegrunnlag
     * @return Aggregat for perioder som er relevant for denne behandlingen
     */
    private UngdomsytelseStartdatoer mapStartdatoerRelevantForBehandlingen(Behandling behandling, Set<JournalpostId> journalposterMottattIDenneBehandlingen,
                                                                           UngdomsytelseStartdatoGrunnlag grunnlag) {

        List<LocalDate> startdatoerUngdomsprogram = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId())
            .getLocalDateIntervals().stream().map(it -> it.getFomDato()).toList();

        var relevantePerioder = grunnlag.getOppgitteStartdatoer()
            .getStartdatoer()
            .stream()
            .filter(it -> startdatoerUngdomsprogram.stream().anyMatch(p ->  !p.isBefore(it.getStartdato()) && p.until(it.getStartdato().plusDays(1), ChronoUnit.DAYS) <= MAKS_GODKJENT_AVSTAND_TIL_STARTDATO)) // Filtrer ut søknader med startdato som ikke matcher ungdomsprogramperioden.
            .filter(it -> journalposterMottattIDenneBehandlingen.stream().anyMatch(at -> at.equals(it.getJournalpostId())))
            .collect(Collectors.toSet());

        return new UngdomsytelseStartdatoer(relevantePerioder);
    }

}
