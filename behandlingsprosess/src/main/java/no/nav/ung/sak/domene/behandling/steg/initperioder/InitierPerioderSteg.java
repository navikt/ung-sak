package no.nav.ung.sak.domene.behandling.steg.initperioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramTjeneste;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.INIT_PERIODER;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;
import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;

@ApplicationScoped
@BehandlingStegRef(value = INIT_PERIODER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class InitierPerioderSteg implements BehandlingSteg {

    private UngdomsprogramTjeneste ungdomsprogramTjeneste;
    private BehandlingRepository behandlingRepository;
    private UngdomsytelseStartdatoRepository startdatoRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private FagsakRepository fagsakRepository;

    @Inject
    public InitierPerioderSteg(UngdomsprogramTjeneste ungdomsprogramTjeneste,
                               BehandlingRepository behandlingRepository,
                               UngdomsytelseStartdatoRepository startdatoRepository,
                               MottatteDokumentRepository mottatteDokumentRepository,
                               UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, FagsakRepository fagsakRepository) {
        this.ungdomsprogramTjeneste = ungdomsprogramTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.startdatoRepository = startdatoRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    public InitierPerioderSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        ungdomsprogramTjeneste.innhentOpplysninger(kontekst);
        initierRelevanteSøknader(kontekst);
        var periodeTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(kontekst.getBehandlingId());
        if (!periodeTidslinje.isEmpty()) {
            final var fagsak = fagsakRepository.finnEksaktFagsak(kontekst.getFagsakId());
            final var maksdato = periodeTidslinje.getMaxLocalDate().equals(TIDENES_ENDE) ? fagsak.getPeriode().getTomDato() : periodeTidslinje.getMaxLocalDate();
            fagsakRepository.utvidPeriode(kontekst.getFagsakId(), periodeTidslinje.getMinLocalDate(), maksdato);
        }
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

        var søknadsperioder = mapStartdatoerRelevantForBehandlingen(mottatteDokumenter, søknadsperiodeGrunnlag);
        startdatoRepository.lagreRelevanteSøknader(behandlingId, søknadsperioder);
    }


    /**
     * Lager aggregat av perioder som er relevant for denne behandlingen, altså perioder fra journalposter som har kommet inn i denne behandlingen.
     *
     * @param journalposterMottattIDenneBehandlingen Journalposter som er mottatt i denne behandlingen
     * @param grunnlag                               Søknadsperiodegrunnlag
     * @return Aggregat for perioder som er relevant for denne behandlingen
     */
    private UngdomsytelseStartdatoer mapStartdatoerRelevantForBehandlingen(Set<JournalpostId> journalposterMottattIDenneBehandlingen,
                                                                           UngdomsytelseStartdatoGrunnlag grunnlag) {

        var relevantePerioder = grunnlag.getOppgitteStartdatoer()
            .getStartdatoer()
            .stream()
            .filter(it -> journalposterMottattIDenneBehandlingen.stream().anyMatch(at -> at.equals(it.getJournalpostId())))
            .collect(Collectors.toSet());

        return new UngdomsytelseStartdatoer(relevantePerioder);
    }

}
