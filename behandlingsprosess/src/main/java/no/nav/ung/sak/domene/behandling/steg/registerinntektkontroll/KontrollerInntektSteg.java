package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelse.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import no.nav.ung.sak.ytelse.uttalelse.RegisterinntektUttalelseTjeneste;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@ApplicationScoped
@BehandlingStegRef(value = KONTROLLER_REGISTER_INNTEKT)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class KontrollerInntektSteg implements BehandlingSteg {

    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private RapportertInntektMapper rapportertInntektMapper;
    private RegisterinntektUttalelseTjeneste registerinntektUttalelseTjeneste;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private BehandlingRepository behandlingRepository;


    @Inject
    public KontrollerInntektSteg(ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                 RapportertInntektMapper rapportertInntektMapper,
                                 RegisterinntektUttalelseTjeneste registerinntektUttalelseTjeneste,
                                 KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste, BehandlingRepository behandlingRepository) {
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.registerinntektUttalelseTjeneste = registerinntektUttalelseTjeneste;
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public KontrollerInntektSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var rapporterteInntekterTidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(kontekst.getBehandlingId());
        final var prosessTriggerTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(kontekst.getBehandlingId());
        final var uttalelser = registerinntektUttalelseTjeneste.hentUttalelser(kontekst.getBehandlingId());
        final var registerinntekterForIkkeGodkjentUttalelse = rapportertInntektMapper.finnRegisterinntekterForUttalelse(kontekst.getBehandlingId(), uttalelser);
        final var kontrollResultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, rapporterteInntekterTidslinje, registerinntekterForIkkeGodkjentUttalelse);

        return switch (kontrollResultat) {
            case BRUK_INNTEKT_FRA_BRUKER -> {
                kontrollerteInntektperioderTjeneste.opprettKontrollerteInntekterPerioderFraBruker(kontekst.getBehandlingId(), rapporterteInntekterTidslinje.mapValue(RapporterteInntekter::getBrukerRapporterteInntekter), prosessTriggerTidslinje);
                yield BehandleStegResultat.utførtUtenAksjonspunkter();
            }
            case OPPRETT_AKSJONSPUNKT -> BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.KONTROLLER_INNTEKT));
            case SETT_PÅ_VENT_TIL_RAPPORTERINGSFRIST -> BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_RAPPORTERINGSFRIST, Venteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST, utledVentefrist(prosessTriggerTidslinje)));
            case OPPRETT_OPPGAVE_TIL_BRUKER -> BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE, Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE, finnEksisterendeFrist(kontekst.getBehandlingId())));
            case OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST -> BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE, Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE, LocalDateTime.now().plusDays(14)));
        };

    }

    private LocalDateTime finnEksisterendeFrist(Long behandlingId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE).getFristTid();
    }

    private LocalDateTime utledVentefrist(LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje) {
        final var tidslinjeRelevanteÅrsaker = prosessTriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT) || it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var harIkkePassertRapporteringsfrist = tidslinjeRelevanteÅrsaker.filterValue(it -> !it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var sisteDatoForRapportertInntekt = harIkkePassertRapporteringsfrist.getMaxLocalDate();
        if (sisteDatoForRapportertInntekt.getDayOfMonth() < 7) {
            return sisteDatoForRapportertInntekt.withDayOfMonth(7).atStartOfDay();
        } else {
            return sisteDatoForRapportertInntekt.plusMonths(1).withDayOfMonth(7).atStartOfDay();
        }
    }

}
