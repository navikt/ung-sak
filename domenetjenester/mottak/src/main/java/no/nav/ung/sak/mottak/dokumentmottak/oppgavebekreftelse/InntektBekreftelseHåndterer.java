package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.oppgave.bekreftelse.Bekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.mottak.dokumentmottak.Trigger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Dependent
@OppgaveTypeRef(Bekreftelse.Type.UNG_AVVIK_REGISTERINNTEKT)
public class InntektBekreftelseHåndterer implements BekreftelseHåndterer {


    private final EtterlysningRepository etterlysningRepository;
    private final MottatteDokumentRepository mottatteDokumentRepository;


    @Inject
    public InntektBekreftelseHåndterer(EtterlysningRepository etterlysningRepository,
                                       MottatteDokumentRepository mottatteDokumentRepository) {
        this.etterlysningRepository = etterlysningRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    public void håndter(OppgaveBekreftelseInnhold oppgaveBekreftelseInnhold) {
        InntektBekreftelse inntektBekreftelse = oppgaveBekreftelseInnhold.oppgaveBekreftelse().getBekreftelse();

        Etterlysning etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(inntektBekreftelse.getOppgaveReferanse());

        if (!etterlysning.getStatus().equals(EtterlysningStatus.VENTER)) {
            throw new IllegalStateException("Etterlysning må hå status VENTER for å motta bekreftelse. Status var " + etterlysning.getStatus());
        }

        mottatteDokumentRepository.oppdaterStatus(List.of(oppgaveBekreftelseInnhold.mottattDokument()), DokumentStatus.GYLDIG);
        if (!inntektBekreftelse.harBrukerGodtattEndringen()) {
            Objects.requireNonNull(inntektBekreftelse.getUttalelseFraBruker(),
                "Uttalelsestekst fra bruker må være satt når bruker ikke har godtatt endringen");
        }

        etterlysning.mottattUttalelse(
            oppgaveBekreftelseInnhold.mottattDokument().getJournalpostId(),
            inntektBekreftelse.harBrukerGodtattEndringen(),
            inntektBekreftelse.getUttalelseFraBruker()
        );

        etterlysningRepository.lagre(etterlysning);
    }

    @Override
    public Optional<Trigger> utledTrigger(UUID oppgaveId) {
        Etterlysning etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(oppgaveId);

        return Optional.of(new Trigger(etterlysning.getPeriode(), BehandlingÅrsakType.UTTALELSE_FRA_BRUKER));
    }

}
