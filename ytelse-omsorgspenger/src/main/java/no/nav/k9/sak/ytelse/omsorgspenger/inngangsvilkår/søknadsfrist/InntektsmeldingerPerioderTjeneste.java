package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class InntektsmeldingerPerioderTjeneste {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public InntektsmeldingerPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository, InntektArbeidYtelseTjeneste iayTjeneste) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
    }

    public Set<Inntektsmelding> hentUtInntektsmeldingerRelevantForBehandling(BehandlingReferanse referanse) {
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(referanse.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> it.getBehandlingId() != null)
            .filter(it -> referanse.getBehandlingId().equals(it.getBehandlingId()))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        if (inntektsmeldingerJournalposter.isEmpty()) {
            return Set.of();
        }

        return iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer(), referanse.getAktørId(), referanse.getFagsakYtelseType())
            .stream()
            .filter(it -> inntektsmeldingerJournalposter.stream().anyMatch(at -> at.getJournalpostId().equals(it.getJournalpostId())))
            .collect(Collectors.toSet());
    }
}
