package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramTjeneste;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VARSEL_REVURDERING;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak.VENTER_BEKREFTELSE_ENDRET_UNGDOMSPROGRAMPERIODE;

@BehandlingStegRef(value = VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VarselRevurderingStegImpl implements VarselRevurderingSteg {

    private BehandlingRepository behandlingRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProgramperiodeendringEtterlysningTjeneste etterlysningTjeneste;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private final Duration ventePeriode;

    @Inject
    public VarselRevurderingStegImpl(BehandlingRepository behandlingRepository,
                                     EtterlysningRepository etterlysningRepository,
                                     ProgramperiodeendringEtterlysningTjeneste etterlysningTjeneste, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                     ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                     @KonfigVerdi(value = "REVURDERING_ENDRET_PERIODE_VENTEFRIST", defaultVerdi = "P14D") String ventePeriode) {
        this.behandlingRepository = behandlingRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
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
            etterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(kontekst.getBehandlingId(), kontekst.getFagsakId());
        } else {
            final var endretTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinjeFraOriginal(BehandlingReferanse.fra(behandling));
            final var nySøktPerioderTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId()).filterValue(it -> it.contains(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE));
            if (!endretTidslinje.disjoint(nySøktPerioderTidslinje).isEmpty()) {
                throw new IllegalStateException("Fant endring i programperiode uten trigger. Legg til trigger manuelt for perioder: " + endretTidslinje.getLocalDateIntervals());
            }
        }

        final var endretProgramperiodeEtterlysninger = etterlysningRepository.hentEtterlysninger(kontekst.getBehandlingId(), EtterlysningType.UTTALELSE_ENDRET_PROGRAMPERIODE);
        final var nyopprettetEtterlysning = endretProgramperiodeEtterlysninger.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.OPPRETTET)).findFirst();
        if (nyopprettetEtterlysning.isPresent()) {
            final var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AUTO_SATT_PÅ_VENT_REVURDERING,
                VENTER_BEKREFTELSE_ENDRET_UNGDOMSPROGRAMPERIODE,
                LocalDateTime.now().plus(ventePeriode));
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
        }


        final var etterlysningSomVentesPå = endretProgramperiodeEtterlysninger
            .stream().filter(it -> it.getStatus().equals(EtterlysningStatus.VENTER))
            .toList();
        if (!etterlysningSomVentesPå.isEmpty()) {
            final var lengsteFristEtterlysning = etterlysningSomVentesPå.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.VENTER))
                .max(Comparator.comparing(Etterlysning::getFrist)).orElseThrow(() -> new IllegalStateException("Forventer å finne en etterlysning på vent"));
            final var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AUTO_SATT_PÅ_VENT_REVURDERING,
                VENTER_BEKREFTELSE_ENDRET_UNGDOMSPROGRAMPERIODE,
                lengsteFristEtterlysning.getFrist());
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


}
