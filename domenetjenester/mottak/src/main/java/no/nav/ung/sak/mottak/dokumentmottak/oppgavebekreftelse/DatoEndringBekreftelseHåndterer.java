package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.oppgave.bekreftelse.Bekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.DatoEndring;
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
@OppgaveTypeRef(Bekreftelse.Type.UNG_ENDRET_FOM_DATO)
@OppgaveTypeRef(Bekreftelse.Type.UNG_ENDRET_TOM_DATO)
public class DatoEndringBekreftelseHåndterer implements BekreftelseHåndterer {

    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final EtterlysningRepository etterlysningRepository;

    @Inject
    public DatoEndringBekreftelseHåndterer(MottatteDokumentRepository mottatteDokumentRepository, EtterlysningRepository etterlysningRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.etterlysningRepository = etterlysningRepository;
    }

    @Override
    public void håndter(OppgaveBekreftelseInnhold oppgaveBekreftelse) {
        DatoEndring bekreftelse = oppgaveBekreftelse.oppgaveBekreftelse().getBekreftelse();


        Etterlysning etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(bekreftelse.getOppgaveId());

        if (!etterlysning.getStatus().equals(EtterlysningStatus.VENTER)) {
            throw new IllegalStateException("Etterlysning må hå status VENTER for å motta bekreftelse. Status var " + etterlysning.getStatus());
        }

        if (!bekreftelse.harBrukerGodtattEndringen()) {
            Objects.requireNonNull(bekreftelse.getUttalelseFraBruker(),
                "Uttalelsestekst fra bruker må være satt når bruker ikke har godtatt endringen");
        }

        etterlysning.mottattUttalelse(
            oppgaveBekreftelse.mottattDokument().getJournalpostId(),
            bekreftelse.harBrukerGodtattEndringen(),
            bekreftelse.getUttalelseFraBruker()
        );

        etterlysningRepository.lagre(etterlysning);
        mottatteDokumentRepository.oppdaterStatus(List.of(oppgaveBekreftelse.mottattDokument()), DokumentStatus.GYLDIG);
    }


    @Override
    public Optional<Trigger> utledTrigger(UUID oppgaveId) {
        Etterlysning etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(oppgaveId);
        return Optional.of(new Trigger(etterlysning.getPeriode(), BehandlingÅrsakType.UTTALELSE_FRA_BRUKER));
    }

}
