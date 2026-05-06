package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Tjeneste for opprettelse av etterlysning av type UTTALELSE_AUTOMATISK_OPPHOR
 * ved revurderinger med årsak RE_VARSEL_AUTOMATISK_OPPHOR.
 */
@Dependent
public class AutomatiskOpphørEtterlysningTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(AutomatiskOpphørEtterlysningTjeneste.class);

    private EtterlysningRepository etterlysningRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public AutomatiskOpphørEtterlysningTjeneste() {
    }

    @Inject
    public AutomatiskOpphørEtterlysningTjeneste(EtterlysningRepository etterlysningRepository,
                                                UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                ProsessTaskTjeneste prosessTaskTjeneste) {
        this.etterlysningRepository = etterlysningRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void opprettEtterlysningForAutomatiskOpphør(BehandlingReferanse behandlingReferanse) {
        var behandlingId = behandlingReferanse.getBehandlingId();

        // Sjekk om det allerede finnes en aktiv etterlysning
        var eksisterende = etterlysningRepository.hentSisteEtterlysning(
            behandlingId, EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR,
            EtterlysningStatus.VENTER, EtterlysningStatus.OPPRETTET);

        if (eksisterende.isPresent()) {
            logger.info("Etterlysning for automatisk opphør finnes allerede for behandling {}, oppretter ikke ny", behandlingId);
            return;
        }

        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Skal ha programperiodegrunnlag for behandling " + behandlingId));

        var periode = grunnlag.hentForEksaktEnPeriodeDersomFinnes()
            .orElseThrow(() -> new IllegalStateException("Skal ha minst én programperiode for behandling " + behandlingId));

        var etterlysning = Etterlysning.opprettForType(
            behandlingId,
            grunnlag.getGrunnlagsreferanse(),
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato()),
            EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR
        );

        etterlysningRepository.lagre(etterlysning);
        logger.info("Opprettet etterlysning for automatisk opphør for behandling {}", behandlingId);

        // Opprett task for å sende etterlysningen
        var prosessTaskGruppe = new ProsessTaskGruppe();
        var opprettEtterlysningTask = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        opprettEtterlysningTask.setBehandling(behandlingReferanse.getFagsakId(), behandlingId);
        prosessTaskGruppe.addNesteSekvensiell(opprettEtterlysningTask);
        prosessTaskTjeneste.lagre(prosessTaskGruppe);
    }

    /**
     * Avbryter eksisterende etterlysning for automatisk opphør.
     * Kalles ved Scenario 2 (utvidet kvote) og Scenario 3 (manuelt opphør).
     */
    public void avbrytEtterlysningForAutomatiskOpphør(BehandlingReferanse behandlingReferanse) {
        var behandlingId = behandlingReferanse.getBehandlingId();

        var eksisterende = etterlysningRepository.hentSisteEtterlysning(
            behandlingId, EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR,
            EtterlysningStatus.VENTER, EtterlysningStatus.OPPRETTET);

        if (eksisterende.isPresent()) {
            var etterlysning = eksisterende.get();
            etterlysning.skalAvbrytes();
            logger.info("Markert etterlysning {} for automatisk opphør som skal avbrytes for behandling {}", etterlysning.getId(), behandlingId);

            // Opprett task for å avbryte etterlysningen hos veileder-appen
            var avbrytTask = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
            avbrytTask.setBehandling(behandlingReferanse.getFagsakId(), behandlingId);
            prosessTaskTjeneste.lagre(avbrytTask);
        } else {
            logger.info("Ingen aktiv etterlysning for automatisk opphør å avbryte for behandling {}", behandlingId);
        }
    }
}



