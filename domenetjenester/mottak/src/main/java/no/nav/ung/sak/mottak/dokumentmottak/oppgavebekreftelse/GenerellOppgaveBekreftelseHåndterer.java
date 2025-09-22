package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.oppgave.bekreftelse.Bekreftelse;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.mottak.dokumentmottak.Trigger;
import no.nav.ung.sak.registerendringer.Endringstype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Dependent
@OppgaveTypeRef(Bekreftelse.Type.UNG_ENDRET_STARTDATO)
@OppgaveTypeRef(Bekreftelse.Type.UNG_ENDRET_SLUTTDATO)
@OppgaveTypeRef(Bekreftelse.Type.UNG_AVVIK_REGISTERINNTEKT)
public class GenerellOppgaveBekreftelseHåndterer implements BekreftelseHåndterer {

    private static final Logger log = LoggerFactory.getLogger(GenerellOppgaveBekreftelseHåndterer.class);
    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final EtterlysningRepository etterlysningRepository;
    private final UttalelseRepository uttalelseRepository;

    @Inject
    public GenerellOppgaveBekreftelseHåndterer(MottatteDokumentRepository mottatteDokumentRepository, EtterlysningRepository etterlysningRepository, UttalelseRepository uttalelseRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.uttalelseRepository = uttalelseRepository;
    }

    @Override
    public void håndter(OppgaveBekreftelseInnhold oppgaveBekreftelse) {
        Bekreftelse bekreftelse = oppgaveBekreftelse.oppgaveBekreftelse().getBekreftelse();
        Etterlysning etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(bekreftelse.getOppgaveReferanse());


        if (bekreftelse.harUttalelse()) {
            Objects.requireNonNull(bekreftelse.getUttalelseFraBruker(),
                "Uttalelsestekst fra bruker må være satt når bruker har uttalelse");
        }

        if (!etterlysning.getStatus().equals(EtterlysningStatus.VENTER)) {
            if (etterlysning.getStatus().equals(EtterlysningStatus.MOTTATT_SVAR)) {
                throw  new IllegalStateException("Etterlysning har allerede mottatt svar, kan ikke håndtere ny uttalelse fra bruker.");
            }
            // Dette kan skje dersom bruker bekrefte mens etterlysningen står i SKAL_AVBRYTES status og tasken for å avbryte etterlysningen ikke er kjørt enda.
            // I dette tifellet går vi videre uten å oppdatere etterlysningen
            log.warn("Forventet at status for etterlysning er VENTER, men var " + etterlysning.getStatus());

        } else {
            etterlysning.mottaSvar(
                oppgaveBekreftelse.mottattDokument().getJournalpostId(),
                bekreftelse.harUttalelse(),
                bekreftelse.getUttalelseFraBruker()
            );
            etterlysningRepository.lagre(etterlysning);

            UttalelseV2 nyUttalelse = new UttalelseV2(
                bekreftelse.harUttalelse(),
                bekreftelse.getUttalelseFraBruker(),
                etterlysning.getPeriode(),
                oppgaveBekreftelse.mottattDokument().getJournalpostId(),
                mapTilEndringsType(etterlysning.getType()),
                etterlysning.getGrunnlagsreferanse()
            );
            uttalelseRepository.lagre(etterlysning.getBehandlingId(), List.of(nyUttalelse));

        }

        mottatteDokumentRepository.oppdaterStatus(List.of(oppgaveBekreftelse.mottattDokument()), DokumentStatus.GYLDIG);
    }


    @Override
    public Optional<Trigger> utledTrigger(UUID oppgaveId) {
        Etterlysning etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(oppgaveId);
        return Optional.of(new Trigger(etterlysning.getPeriode(), BehandlingÅrsakType.UTTALELSE_FRA_BRUKER));
    }

    private EndringType mapTilEndringsType(EtterlysningType etterlysningType) {
        return switch (etterlysningType) {
            case UTTALELSE_KONTROLL_INNTEKT -> EndringType.ENDRET_INNTEKT;
            case UTTALELSE_ENDRET_STARTDATO -> EndringType.ENDRET_STARTDATO;
            case UTTALELSE_ENDRET_SLUTTDATO -> EndringType.ENDRET_SLUTTDATO;
        };
    }

}
