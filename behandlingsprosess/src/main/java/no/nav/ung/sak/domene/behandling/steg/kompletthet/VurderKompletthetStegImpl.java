package no.nav.ung.sak.domene.behandling.steg.kompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.etterlysning.SettEtterlysningTilUtløptTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_KOMPLETTHET;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

@BehandlingStegRef(value = VURDER_KOMPLETTHET)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderKompletthetStegImpl implements VurderKompletthetSteg {

    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private Duration ventePeriode;


    VurderKompletthetStegImpl() {
    }

    @Inject
    public VurderKompletthetStegImpl(EtterlysningRepository etterlysningRepository, ProsessTaskTjeneste prosessTaskTjeneste, @KonfigVerdi(value = "VENTEFRIST_UTTALELSE", defaultVerdi = "P14D") String ventePeriode) {
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.ventePeriode = Duration.parse(ventePeriode);
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var etterlysningerSomVenterPåSvar = etterlysningRepository.hentEtterlysningerSomVenterPåSvar(kontekst.getBehandlingId());
        final var lengsteFristPrType = etterlysningerSomVenterPåSvar.stream().collect(Collectors.toMap(Etterlysning::getType, Function.identity(), BinaryOperator.maxBy(Comparator.comparing(Etterlysning::getFrist, Comparator.nullsLast(Comparator.naturalOrder())))));
        final var aksjonspunktresultater = lengsteFristPrType.entrySet()
            .stream()
            .filter(e -> !harPassertFrist(e.getValue().getFrist()))
            .map(e -> AksjonspunktResultat.opprettForAksjonspunktMedFrist(mapTilDefinisjon(e.getKey()), mapTilVenteårsak(e.getKey()), e.getValue().getFrist() == null ? LocalDateTime.now().plus(ventePeriode) : e.getValue().getFrist())).toList();

        final var harUtløpteEtterlysninger = etterlysningerSomVenterPåSvar.stream()
            .anyMatch(e -> harPassertFrist(e.getFrist()));

        if (harUtløpteEtterlysninger) {
            // Dersom vi har utløpte etterlysninger ønsker vi å oppdatere status på disse
            var prosessTaskData = ProsessTaskData.forProsessTask(SettEtterlysningTilUtløptTask.class);
            prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
            prosessTaskTjeneste.lagre(prosessTaskData);
        }

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktresultater);
    }

    private static boolean harPassertFrist(LocalDateTime frist) {
        return frist != null && frist.isBefore(LocalDateTime.now());
    }

    private static AksjonspunktDefinisjon mapTilDefinisjon(EtterlysningType type) {
        switch (type) {
            case UTTALELSE_KONTROLL_INNTEKT -> {
                return AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE;
            }
            case UTTALELSE_ENDRET_PROGRAMPERIODE -> {
                return AUTO_SATT_PÅ_VENT_REVURDERING;
            }
            default -> throw new IllegalArgumentException("Ukjent etterlysningstype: " + type);
        }
    }

    private Venteårsak mapTilVenteårsak(EtterlysningType type) {
        switch (type) {
            case UTTALELSE_KONTROLL_INNTEKT -> {
                return Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE;
            }
            case UTTALELSE_ENDRET_PROGRAMPERIODE -> {
                return Venteårsak.VENTER_BEKREFTELSE_ENDRET_UNGDOMSPROGRAMPERIODE;
            }
            default -> throw new IllegalArgumentException("Ukjent etterlysningstype: " + type);
        }
    }

}
