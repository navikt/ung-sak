package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsprogramBekreftetPeriodeEndring;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.apache.commons.lang3.stream.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VARSEL_REVURDERING;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

@BehandlingStegRef(value = VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VarselRevurderingStegImpl implements VarselRevurderingSteg {
    private static final Logger logger = LoggerFactory.getLogger(VarselRevurderingStegImpl.class);

    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private final Duration ventePeriode;

    @Inject
    public VarselRevurderingStegImpl(BehandlingRepository behandlingRepository,
                                     UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                     UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                     MottatteDokumentRepository mottatteDokumentRepository, ProsessTaskTjeneste prosessTaskTjeneste, EtterlysningRepository etterlysningRepository, ProsessTriggereRepository prosessTriggereRepository,
                                     @KonfigVerdi(value = "REVURDERING_ENDRET_PERIODE_VENTEFRIST", defaultVerdi = "P14D") String ventePeriode) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.ventePeriode = Duration.parse(ventePeriode);
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (behandling.getBehandlingÅrsaker().isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        List<BehandlingÅrsakType> behandlingÅrsakerTyper = behandling.getBehandlingÅrsakerTyper();

        boolean skalOppretteEtterlysning = behandlingÅrsakerTyper.stream()
            .anyMatch(årsak ->
                BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM == årsak ||
                    BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM == årsak
            );

        if (skalOppretteEtterlysning) {
            opprettTaskForEtterlysning(kontekst);
        }

        final var bekreftelser = finnBekreftelser(behandling);
        final var gyldigeDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId());
        final var ungdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId()).compress();

        return VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(
                behandlingÅrsakerTyper,
                ungdomsprogramTidslinje,
                gyldigeDokumenter,
                bekreftelser,
                ventePeriode,
                behandling.getAksjonspunktMedDefinisjonOptional(AUTO_SATT_PÅ_VENT_REVURDERING)

            ).map(BehandleStegResultat::utførtMedAksjonspunktResultater)
            .orElse(BehandleStegResultat.utførtUtenAksjonspunkter());

    }

    private List<UngdomsprogramBekreftetPeriodeEndring> finnBekreftelser(Behandling behandling) {
        final var ungdomsytelseStartdatoGrunnlag = ungdomsytelseStartdatoRepository.hentGrunnlag(behandling.getId());
        return ungdomsytelseStartdatoGrunnlag.stream()
            .flatMap(it -> it.getBekreftetPeriodeEndringer().stream()).toList();
    }

    private void opprettTaskForEtterlysning(BehandlingskontrollKontekst kontekst) {
        List<Trigger> relevanteTriggerForEtterlysning = prosessTriggereRepository.hentGrunnlag(kontekst.getBehandlingId())
            .stream()
            .map(ProsessTriggere::getTriggere)
            .flatMap(Collection::stream)
            .filter(trigger ->
                trigger.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM ||
                    trigger.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM
            )
            .toList();

        List<Etterlysning> relevanteEtterlysninger = Streams.of(EtterlysningType.UTTALELSE_ENDRET_STARTDATO, EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO)
            .flatMap(type -> etterlysningRepository.hentEtterlysninger(kontekst.getBehandlingId(), type).stream())
            .filter(e -> e.getStatus() == EtterlysningStatus.VENTER || e.getStatus() == EtterlysningStatus.OPPRETTET || e.getStatus() == EtterlysningStatus.MOTTATT_SVAR)
            .toList();

        List<EtterLysningUngdomsPeriodeGrunnlag> etterLysningerUngdomsPeriodeGrunnlag = relevanteEtterlysninger.stream()
            .map(e -> {
                Optional<Set<DatoIntervallEntitet>> periodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(e.getGrunnlagsreferanse())
                    .map(it -> it.getUngdomsprogramPerioder().getPerioder().stream()
                        .map(UngdomsprogramPeriode::getPeriode)
                        .collect(Collectors.toSet()));

                return EtterLysningUngdomsPeriodeGrunnlag.of(e, periodeGrunnlag.orElseThrow(() -> new IllegalStateException("Fant ikke grunnlag for etterlysning " + e.getGrunnlagsreferanse())));
            })
            .toList();

        etterLysningerUngdomsPeriodeGrunnlag.forEach(etterLysningUngdomsPeriodeGrunnlag ->
            {
                if (etterLysningUngdomsPeriodeGrunnlag.perioder.isEmpty()) {
                    // TODO: Hvorfor har en eksisterende etterlysning ingen perioder?
                } else {
                    //TOOD: Avbryt etterlysning dersom det er en overlap med eksisterende etterlysning
                }
            }
        );

        Optional<Trigger> sisteEndretStartdatoTrigger = relevanteTriggerForEtterlysning.stream()
            .filter(t -> t.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)
            .max(Comparator.comparing(Trigger::getOpprettetTidspunkt));


        UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(kontekst.getBehandlingId()).orElseThrow();

        List<Etterlysning> etterlysningerSomSkalAvbrytes = new ArrayList<>();
        List<Etterlysning> etterlysningerSomSkalOpprettes = new ArrayList<>();
        final var prosessTaskGruppe = new ProsessTaskGruppe();

        relevanteTriggerForEtterlysning.stream()
            .map(trigger -> mapTilEtterlysning(kontekst, trigger, ungdomsprogramPeriodeGrunnlag))
            .forEach(etterlysning -> {
                List<Etterlysning> eksiterendeEtterLysninger = etterlysningRepository.hentEtterlysninger(kontekst.getBehandlingId(), etterlysning.getType());

                if (eksiterendeEtterLysninger.isEmpty()) {
                    etterlysningerSomSkalOpprettes.add(etterlysning);
                } else {
                    logger.info("Etterlysning {} finnes allerede for behandling {}", etterlysning.getType(), kontekst.getBehandlingId());

                    eksiterendeEtterLysninger
                        .stream()
                        .filter(eksiterendeEtterLysning -> eksiterendeEtterLysning.getType() == etterlysning.getType())
                        .forEach(eksiterendeEtterLysning -> {
                            eksiterendeEtterLysning.skalAvbrytes();
                            etterlysningerSomSkalAvbrytes.add(eksiterendeEtterLysning);
                        });
                }
            });

        if (!etterlysningerSomSkalAvbrytes.isEmpty()) {
            logger.info("Avbryter etterlysninger {}", etterlysningerSomSkalAvbrytes);
            etterlysningRepository.lagre(etterlysningerSomSkalAvbrytes);
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForAvbrytelseAvEtterlysning(kontekst));
        }
        if (!etterlysningerSomSkalOpprettes.isEmpty()) {
            logger.info("Oppretter etterlysninger {}", etterlysningerSomSkalOpprettes);
            etterlysningRepository.lagre(etterlysningerSomSkalOpprettes);
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForOpprettingAvEtterlysning(kontekst));
        }
        if (!prosessTaskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(prosessTaskGruppe);
        }
    }

    private static Etterlysning mapTilEtterlysning(BehandlingskontrollKontekst kontekst, Trigger trigger, UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag) {
        BehandlingÅrsakType behandlingÅrsakType = trigger.getÅrsak();
        DatoIntervallEntitet periode = trigger.getPeriode();
        final Long behandlingId = kontekst.getBehandlingId();

        UUID grunnlagsreferanse = ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse();
        UUID eksternReferanse = UUID.randomUUID();

        return switch (behandlingÅrsakType) {
            case RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM -> Etterlysning.forEndretStartdatoUttalelse(
                behandlingId,
                grunnlagsreferanse,
                eksternReferanse,
                periode
            );
            case RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM -> Etterlysning.forEndretSluttdatoUttalelse(
                behandlingId,
                grunnlagsreferanse,
                eksternReferanse,
                periode
            );
            case null, default ->
                throw new IllegalStateException("Ugyldig behandling årsak type: " + trigger.getÅrsak());
        };
    }

    private ProsessTaskData lagTaskForAvbrytelseAvEtterlysning(BehandlingskontrollKontekst kontekst) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
        prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
        return prosessTaskData;
    }

    private ProsessTaskData lagTaskForOpprettingAvEtterlysning(BehandlingskontrollKontekst kontekst) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT.getKode());
        prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
        return prosessTaskData;
    }

    record EtterLysningUngdomsPeriodeGrunnlag(Etterlysning etterlysning, Set<DatoIntervallEntitet> perioder) {
        public static EtterLysningUngdomsPeriodeGrunnlag of(Etterlysning etterlysning, Set<DatoIntervallEntitet> perioder) {
            return new EtterLysningUngdomsPeriodeGrunnlag(etterlysning, perioder);
        }
    }
}
