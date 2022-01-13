package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.typer.JournalpostId;

@Dependent
public class InntektsmeldingerPerioderTjeneste {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public InntektsmeldingerPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                             BehandlingRepository behandlingRepository,
                                             InntektArbeidYtelseTjeneste iayTjeneste) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
    }

    public Set<Inntektsmelding> hentUtInntektsmeldingerRelevantForBehandling(BehandlingReferanse referanse) {
        Behandling behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());

        Set<JournalpostId> inntektsmeldingerJournalposter = hentUtIMJournalposterKnyttetTilFagsak(behandling);

        if (inntektsmeldingerJournalposter.isEmpty()) {
            return Set.of();
        }

        return iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer(), referanse.getAktørId(), referanse.getFagsakYtelseType())
            .stream()
            .filter(it -> inntektsmeldingerJournalposter.contains(it.getJournalpostId()))
            .collect(Collectors.toSet());
    }

    public Set<Inntektsmelding> hentUtInntektsmeldingerKnyttetTilBehandling(BehandlingReferanse referanse) {
        Behandling behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());

        Set<JournalpostId> inntektsmeldingerJournalposter = hentUtIMJournalposterKnyttetTilBehandling(behandling);

        if (inntektsmeldingerJournalposter.isEmpty()) {
            return Set.of();
        }

        return iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer(), referanse.getAktørId(), referanse.getFagsakYtelseType())
            .stream()
            .filter(it -> inntektsmeldingerJournalposter.contains(it.getJournalpostId()))
            .collect(Collectors.toSet());
    }

    private Set<JournalpostId> hentUtIMJournalposterKnyttetTilFagsak(Behandling behandling) {
        return mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> it.getBehandlingId() != null)
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());
    }

    private Set<JournalpostId> hentUtIMJournalposterKnyttetTilBehandling(Behandling behandling) {
        return mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> Objects.equals(it.getBehandlingId(), behandling.getId()))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());
    }
}
