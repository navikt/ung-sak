package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsprogramBekreftetPeriodeEndring;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollResultat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

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

        LocalDateTimeline<Boolean> endretUngdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinje(BehandlingReferanse.fra(behandling));
        opprettTaskForEtterlysning(kontekst, endretUngdomsprogramTidslinje);

        final var bekreftelser = finnBekreftelser(behandling);
        final var gyldigeDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId());
        final var ungdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId()).compress();

        return VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(
                behandling.getBehandlingÅrsakerTyper(),
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

    private void opprettTaskForEtterlysning(BehandlingskontrollKontekst kontekst, LocalDateTimeline<Boolean> endretUngdomsprogramTidslinje) {
        UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(kontekst.getBehandlingId()).orElseThrow();

        List<Trigger> triggere = prosessTriggereRepository.hentGrunnlag(kontekst.getBehandlingId())
            .stream()
            .map(ProsessTriggere::getTriggere)
            .flatMap(Collection::stream)
            .toList();

        LocalDateTimeline<Set<Etterlysning>> etterlysningerLocalDateTimeline = triggere
            .stream()
            .map(trigger -> {
                    Etterlysning etterlysning = mapTilEtterlysning(kontekst, trigger, ungdomsprogramPeriodeGrunnlag);
                    if (etterlysning == null) {
                        return null;
                    }
                    return new LocalDateTimeline(
                        trigger.getPeriode().getFomDato(),
                        trigger.getPeriode().getTomDato(),
                        Set.of(etterlysning));
                }
            )
            .filter(Objects::nonNull)
            .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::union))
            .orElse(LocalDateTimeline.empty());

        etterlysningerLocalDateTimeline.stream()
            .filter(Objects::nonNull)
            .forEach(etterlysningSegment -> {
                Etterlysning etterlysning = etterlysningSegment.getValue().iterator().next();
                List<Etterlysning> eksiterendeEtterLysninger = etterlysningRepository.hentEtterlysninger(kontekst.getBehandlingId(), etterlysning.getType());
                if (eksiterendeEtterLysninger.isEmpty()) {
                    etterlysningRepository.lagre(etterlysning);

                    var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
                    prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, etterlysning.getType().getKode());
                    prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
                    prosessTaskTjeneste.lagre(prosessTaskData);
                } else {
                    // TODO: Håndter eksisterende etterlysning av samme type som ikke er besvart.
                    logger.info("Etterlysning {} finnes allerede for behandling {}", etterlysning.getType(), kontekst.getBehandlingId());
                }
            });
    }

    @Nullable
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
            case null, default -> null;
        };
    }

    private List<Etterlysning> avbrytDersomEksisterendeEtterlysning(List<Etterlysning> etterlysninger, LocalDateSegment<KontrollResultat> segment) {
        var etterlysningerSomSkalAvbrytes = etterlysninger.stream()
            .filter(etterlysning ->
                etterlysning.getPeriode().toLocalDateInterval().overlaps(segment.getLocalDateInterval()))
            .filter(e -> e.getStatus().equals(EtterlysningStatus.VENTER))
            .toList();
        etterlysningerSomSkalAvbrytes.forEach(Etterlysning::skalAvbrytes);

        return etterlysningerSomSkalAvbrytes;
    }
}
