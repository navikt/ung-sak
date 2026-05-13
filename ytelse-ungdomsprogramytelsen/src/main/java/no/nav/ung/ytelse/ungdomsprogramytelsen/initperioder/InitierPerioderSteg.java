package no.nav.ung.ytelse.ungdomsprogramytelsen.initperioder;

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
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.typer.JournalpostId;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.INIT_PERIODER;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@ApplicationScoped
@BehandlingStegRef(value = INIT_PERIODER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class InitierPerioderSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private UngdomsytelseStartdatoRepository startdatoRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;

    @Inject
    public InitierPerioderSteg(BehandlingRepository behandlingRepository,
                               UngdomsytelseStartdatoRepository startdatoRepository,
                               MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.startdatoRepository = startdatoRepository;
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
        var søknadsperiodeGrunnlag = startdatoRepository.hentGrunnlag(behandlingId).orElseThrow();
        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> it.getBehandlingId().equals(behandlingId))
            .filter(it -> it.getType().equals(Brevkode.UNGDOMSYTELSE_SOKNAD))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        var alleredeKjenteStartdatoer = finnAlleredeKjenteStartdatoer(behandling.getFagsakId(), behandlingId);
        var søknadsperioder = mapStartdatoerRelevantForBehandlingen(mottatteDokumenter, søknadsperiodeGrunnlag, alleredeKjenteStartdatoer);
        startdatoRepository.lagreRelevanteSøknader(behandlingId, søknadsperioder);
    }

    /**
     * Henter startdatoer som allerede er kjent fra forrige avsluttede ikke-henlagte ytelsesbehandling på fagsaken.
     * <p>
     * Brukes for å unngå at en startdato som er identisk med tidligere godkjent grunnlag (typisk fra papirsøknad
     * generert i forbindelse med revurdering) markeres som "relevant" på nytt og dermed blåser opp tidslinjen
     * for perioder til vurdering. Førstegangsbehandling og ekte nye startdatoer påvirkes ikke.
     */
    private Set<LocalDate> finnAlleredeKjenteStartdatoer(Long fagsakId, Long behandlingId) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsakId)
            .filter(b -> !b.getId().equals(behandlingId))
            .flatMap(b -> startdatoRepository.hentGrunnlag(b.getId()))
            .map(this::hentRelevanteStartdatoerSomLocalDate)
            .orElse(Set.of());
    }

    private Set<LocalDate> hentRelevanteStartdatoerSomLocalDate(UngdomsytelseStartdatoGrunnlag grunnlag) {
        var relevante = Optional.ofNullable(grunnlag.getRelevanteStartdatoer())
            .orElse(grunnlag.getOppgitteStartdatoer());
        if (relevante == null) {
            return Set.of();
        }
        return relevante.getStartdatoer().stream()
            .map(UngdomsytelseSøktStartdato::getStartdato)
            .collect(Collectors.toSet());
    }


    /**
     * Lager aggregat av perioder som er relevant for denne behandlingen, altså perioder fra journalposter som har kommet inn i denne behandlingen.
     * <p>
     * Startdatoer som allerede er kjent fra forrige avsluttede behandling filtreres bort, slik at kun reelt
     * nye startdatoer markeres som relevante. Dette hindrer at f.eks. en papirsøknad mottatt i forbindelse
     * med en forlengelse-revurdering (med samme startdato som forrige vedtak) re-vurderer hele programperioden.
     *
     * @param journalposterMottattIDenneBehandlingen Journalposter som er mottatt i denne behandlingen
     * @param grunnlag                               Søknadsperiodegrunnlag
     * @param alleredeKjenteStartdatoer              Startdatoer som er kjent fra forrige avsluttede behandling
     * @return Aggregat for perioder som er relevant for denne behandlingen
     */
    private UngdomsytelseStartdatoer mapStartdatoerRelevantForBehandlingen(Set<JournalpostId> journalposterMottattIDenneBehandlingen,
                                                                           UngdomsytelseStartdatoGrunnlag grunnlag,
                                                                           Set<LocalDate> alleredeKjenteStartdatoer) {

        var relevantePerioder = grunnlag.getOppgitteStartdatoer()
            .getStartdatoer()
            .stream()
            .filter(it -> journalposterMottattIDenneBehandlingen.stream().anyMatch(at -> at.equals(it.getJournalpostId())))
            .filter(it -> !alleredeKjenteStartdatoer.contains(it.getStartdato()))
            .collect(Collectors.toSet());

        return new UngdomsytelseStartdatoer(relevantePerioder);
    }

}
