package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandlingprosessSporing;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.EtterlysningOgGrunnlag;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.EtterlysningStatusOgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Dependent
public class MaksdatoEtterlysningTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(MaksdatoEtterlysningTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EtterlysningRepository etterlysningRepository;
    private BehandingprosessSporingRepository behandingprosessSporingRepository;

    public MaksdatoEtterlysningTjeneste() {
    }

    @Inject
    public MaksdatoEtterlysningTjeneste(BehandlingRepository behandlingRepository, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, ProsessTaskTjeneste prosessTaskTjeneste, EtterlysningRepository etterlysningRepository, BehandingprosessSporingRepository behandingprosessSporingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.etterlysningRepository = etterlysningRepository;
        this.behandingprosessSporingRepository = behandingprosessSporingRepository;
    }

    public void opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(BehandlingReferanse behandlingReferanse) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());

        if (!behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO)) {
            return;
        }

        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Skal ha innhentet perioder"));

        LocalDate maksdato = ungdomsprogramPeriodeGrunnlag.getPeriodeMaksDato().orElseThrow(() -> new IllegalStateException("Forventer at maksdato er satt"));
        DatoIntervallEntitet periode = ungdomsprogramPeriodeGrunnlag.hentForEksaktEnPeriode();
        LocalDate tomDato = periode.getTomDato();


        var eksisterende = etterlysningRepository.hentSisteEtterlysning(
            behandlingReferanse.getBehandlingId(), EtterlysningType.UTTALELSE_OPPHOR_VED_MAKSDATO,
            EtterlysningStatus.VENTER, EtterlysningStatus.OPPRETTET, EtterlysningStatus.MOTTATT_SVAR, EtterlysningStatus.UTLØPT);

        Optional<EtterlysningOgGrunnlag> etterlysningOgGrunnlag = eksisterende.map(e -> new EtterlysningOgGrunnlag(
            new EtterlysningStatusOgType(e.getStatus(), e.getType()),
            ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(e.getGrunnlagsreferanse())));

        EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(tomDato, maksdato, LocalDate.now(),
            etterlysningOgGrunnlag.orElse(null));

        // Utled resultat
        EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        // Håndter resultat
        switch (resultat) {
            case OPPRETT_ETTERLYSNING ->
                opprettEtterlysningForOpphør(behandlingReferanse, ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse(), periode.getFomDato(), maksdato);
            case ERSTATT_EKSISTERENDE -> {
                avbrytEtterlysningForOpphørVedMaksdato(behandlingReferanse);
                opprettEtterlysningForOpphør(behandlingReferanse, ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse(), periode.getFomDato(), maksdato);
            }
            case AVBRYT_ETTERLYSNING -> avbrytEtterlysningForOpphørVedMaksdato(behandlingReferanse);
            case INGEN_ENDRING -> logger.info("Ingen endring i etterlysning for opphør nødvendig");
        }

        // Logg for sporing
        try {
            behandingprosessSporingRepository.lagreSporing(new BehandlingprosessSporing(
                behandling.getId(),
                JsonObjectMapper.getJson(input),
                JsonObjectMapper.getJson(resultat),
                EtterlysningType.UTTALELSE_OPPHOR_VED_MAKSDATO + "_UTLEDER"
            ));
        } catch (IOException e) {
            logger.warn("Feil ved lagring av sporing for etterlysning opphør ved maksdato", e);
        }

        // Sikkerhetsnett: et rent varsel-om-opphør-ved-maksdato-løp (behandlingen har KUN denne årsaken)
        // må ha opprettet en etterlysning slik at behandlingen settes på vent og deltaker varsles før vedtak/brev om opphør.
        // Dersom det ikke finnes noen etterlysning av denne typen her, ville behandlingen gått videre til opphør uten kontradiksjon.
        // Har behandlingen andre årsaker i tillegg (f.eks. inntektskontroll eller forlenget periode/opphør), skal den ikke hardfeile her.
        if (erKunVarselOpphørVedMaksdato(behandling)) {
            boolean harEtterlysning = switch (resultat) {
                case OPPRETT_ETTERLYSNING, ERSTATT_EKSISTERENDE -> true;
                case INGEN_ENDRING -> eksisterende.isPresent();
                case AVBRYT_ETTERLYSNING -> false;
            };
            if (!harEtterlysning) {
                throw new IllegalStateException("Forventet etterlysning om opphør ved maksdato for behandling "
                    + behandlingReferanse.getBehandlingId() + ", men ingen ble opprettet (resultat=" + resultat + "). "
                    + "Behandlingen skal ikke gå videre til vedtak/brev om opphør uten at deltaker er varslet.");
            }
        }

    }

    private static boolean erKunVarselOpphørVedMaksdato(Behandling behandling) {
        var årsaker = behandling.getBehandlingÅrsakerTyper();
        return !årsaker.isEmpty() && årsaker.stream().allMatch(å -> å == BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO);
    }

    /**
     * Avbryter eksisterende etterlysning for opphør ved maksdato.
     * Kalles ved forlenget periode og manuelt opphør.
     */
    private void avbrytEtterlysningForOpphørVedMaksdato(BehandlingReferanse behandlingReferanse) {
        var behandlingId = behandlingReferanse.getBehandlingId();

        var eksisterende = etterlysningRepository.hentSisteEtterlysning(
            behandlingId, EtterlysningType.UTTALELSE_OPPHOR_VED_MAKSDATO,
            EtterlysningStatus.VENTER, EtterlysningStatus.OPPRETTET);

        if (eksisterende.isPresent()) {
            var etterlysning = eksisterende.get();
            etterlysning.skalAvbrytes();
            etterlysningRepository.lagre(etterlysning);
            logger.info("Markert etterlysning {} for opphør ved maksdato som skal avbrytes for behandling {}", etterlysning.getId(), behandlingId);

            // Opprett task for å avbryte etterlysningen
            var avbrytTask = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
            avbrytTask.setBehandling(behandlingReferanse.getFagsakId(), behandlingId);
            prosessTaskTjeneste.lagre(avbrytTask);
        } else {
            logger.info("Ingen aktiv etterlysning for opphør ved maksdato å avbryte for behandling {}", behandlingId);
        }
    }

    private void opprettEtterlysningForOpphør(
        BehandlingReferanse behandlingReferanse,
        UUID grunnlagsreferanse,
        LocalDate fomDato,
        LocalDate maksdato) {
        var behandlingId = behandlingReferanse.getBehandlingId();

        var etterlysning = Etterlysning.opprettForType(
            behandlingId,
            grunnlagsreferanse,
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, maksdato),
            EtterlysningType.UTTALELSE_OPPHOR_VED_MAKSDATO
        );

        etterlysningRepository.lagre(etterlysning);
        logger.info("Opprettet etterlysning for opphør ved maksdato for behandling {}", behandlingId);

        // Opprett task for å sende etterlysningen
        var opprettEtterlysningTask = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        opprettEtterlysningTask.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_OPPHOR_VED_MAKSDATO.getKode());
        opprettEtterlysningTask.setBehandling(behandlingReferanse.getFagsakId(), behandlingId);
        prosessTaskTjeneste.lagre(opprettEtterlysningTask);
    }


}
