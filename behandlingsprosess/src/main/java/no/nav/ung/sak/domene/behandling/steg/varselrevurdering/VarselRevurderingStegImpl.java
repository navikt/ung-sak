package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
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
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VARSEL_REVURDERING;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

@BehandlingStegRef(value = VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VarselRevurderingStegImpl implements VarselRevurderingSteg {

    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EtterlysningRepository etterlysningRepository;
    private final Duration ventePeriode;

    @Inject
    public VarselRevurderingStegImpl(BehandlingRepository behandlingRepository,
                                     UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                     UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                     MottatteDokumentRepository mottatteDokumentRepository, ProsessTaskTjeneste prosessTaskTjeneste, EtterlysningRepository etterlysningRepository,
                                     @KonfigVerdi(value = "REVURDERING_ENDRET_PERIODE_VENTEFRIST", defaultVerdi = "P14D") String ventePeriode) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.etterlysningRepository = etterlysningRepository;
        this.ventePeriode = Duration.parse(ventePeriode);
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (behandling.getBehandlingÅrsaker().isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        final var ungdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId()).compress();
        LocalDateTimeline<Boolean> endretUngdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinje(BehandlingReferanse.fra(behandling));
        final var bekreftelser = finnBekreftelser(behandling);
        final var gyldigeDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId());

        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)) {
            opprettTaskForEtterlysning(EtterlysningType.UTTALELSE_ENDRET_STARTDATO, kontekst, endretUngdomsprogramTidslinje);
        } else if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM)) {
            // TODO: Utled om RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM burde trigge etterlysning første gang.
            opprettTaskForEtterlysning(EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO, kontekst, endretUngdomsprogramTidslinje);
        }

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

    private void opprettTaskForEtterlysning(EtterlysningType etterlysningType, BehandlingskontrollKontekst kontekst, LocalDateTimeline<Boolean> endretUngdomsprogramTidslinje) {
        UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(kontekst.getBehandlingId()).orElseThrow();
        UUID grunnlagsreferanse = ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse();
        UUID eksternReferanse = UUID.randomUUID();

        // TODO: Utled datoIntervallEntitet.
        LocalDateInterval localDateInterval = endretUngdomsprogramTidslinje.toSegments().descendingIterator().next().getLocalDateInterval();
        DatoIntervallEntitet datoIntervallEntitet = DatoIntervallEntitet.fra(localDateInterval.getFomDato(), localDateInterval.getTomDato());

        Etterlysning etterlysning;
        switch (etterlysningType) {
            case UTTALELSE_ENDRET_STARTDATO -> {
                etterlysning = Etterlysning.forEndretStartdatoUttalelse(
                    kontekst.getBehandlingId(),
                    grunnlagsreferanse,
                    eksternReferanse,
                    datoIntervallEntitet
                );
            }
            case UTTALELSE_ENDRET_SLUTTDATO -> {
                etterlysning = Etterlysning.forEndretSluttdatoUttalelse(
                    kontekst.getBehandlingId(),
                    grunnlagsreferanse,
                    eksternReferanse,
                    datoIntervallEntitet
                );
            }
            default -> throw new IllegalArgumentException("Ikke støttet etterlysningstype: " + etterlysningType);
        }

        etterlysningRepository.lagre(etterlysning);

        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, etterlysning.getType().getKode());
        prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
        prosessTaskTjeneste.lagre(prosessTaskData);
    }
}
