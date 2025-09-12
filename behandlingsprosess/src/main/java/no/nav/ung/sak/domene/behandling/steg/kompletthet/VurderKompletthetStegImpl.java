package no.nav.ung.sak.domene.behandling.steg.kompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningOppretter;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.RapporteringsfristAutopunktUtleder;
import no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_KOMPLETTHET;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

/**
 * Implementasjon av steg for å vurdere kompletthet i en behandling.
 * Dette steget oppretter nødvendige etterlysninger, utleder aksjonspunkter,
 * og håndterer utløpte etterlysninger basert på fristen til eksisterende etterlysninger.
 * <p>
 * Opprettelse av oppgave hos deltaker skjer a-sync. Frist for etterlysning settes først når oppgaven har blitt opprettet og kan løses av deltaker. Etterlysningene vil derfor ikke har frist her ved første gjennomkjøring. Fristen som brukes er definert i miljøvariabelen `VENTEFRIST_UTTALELSE`, som har standardverdi på 14 dager (P14D).
 * Steget kan returere tre ventepunkter:
 * <ul>
 *     <li>Venter på rapporteringsfrist</li>
 *     <li>Venter på etterlysning av uttalelse for kontroll av inntekt</li>
 *     <li>Venter på etterlysning av uttalelse for endring av programperiode</li>
 * </ul>
 */
@BehandlingStegRef(value = VURDER_KOMPLETTHET)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderKompletthetStegImpl implements VurderKompletthetSteg {

    private static final Logger log = LoggerFactory.getLogger(VurderKompletthetStegImpl.class);
    private EtterlysningRepository etterlysningRepository;
    private BehandlingRepository behandlingRepository;
    private KontrollerInntektEtterlysningOppretter kontrollerInntektEtterlysningOppretter;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;
    private RapporteringsfristAutopunktUtleder rapporteringsfristAutopunktUtleder;
    private Duration ventePeriode;


    VurderKompletthetStegImpl() {
    }

    @Inject
    public VurderKompletthetStegImpl(EtterlysningRepository etterlysningRepository,
                                     BehandlingRepository behandlingRepository,
                                     KontrollerInntektEtterlysningOppretter kontrollerInntektEtterlysningOppretter,
                                     ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste,
                                     RapporteringsfristAutopunktUtleder rapporteringsfristAutopunktUtleder,
                                     @KonfigVerdi(value = "VENTEFRIST_UTTALELSE", defaultVerdi = "P14D") String ventePeriode) {
        this.etterlysningRepository = etterlysningRepository;
        this.behandlingRepository = behandlingRepository;
        this.kontrollerInntektEtterlysningOppretter = kontrollerInntektEtterlysningOppretter;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
        this.rapporteringsfristAutopunktUtleder = rapporteringsfristAutopunktUtleder;
        this.ventePeriode = Duration.parse(ventePeriode);
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        final var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        final var behandlingReferanse = BehandlingReferanse.fra(behandling);

        // Steg 1: Opprett etterlysninger
        kontrollerInntektEtterlysningOppretter.opprettEtterlysninger(behandlingReferanse);
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);

        // Steg 2: Utled aksjonspunkter
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        // Sjekker rapporteringsfrist
        rapporteringsfristAutopunktUtleder.utledAutopunktForRapporteringsfrist(behandlingReferanse)
            .ifPresent(aksjonspunktResultater::add);

        // Sjekker etterlysninger opprettet i steg 1
        final var etterlysningerSomVenterPåSvar = etterlysningRepository.hentEtterlysningerSomVenterPåSvar(kontekst.getBehandlingId());
        aksjonspunktResultater.addAll(utledFraEtterlysninger(etterlysningerSomVenterPåSvar));

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
    }

    private List<AksjonspunktResultat> utledFraEtterlysninger(List<Etterlysning> etterlysningerSomVenterPåSvar) {
        final var lengsteFristPrType = etterlysningerSomVenterPåSvar.stream().collect(Collectors.toMap(Etterlysning::getType, Function.identity(), BinaryOperator.maxBy(Comparator.comparing(Etterlysning::getFrist, Comparator.nullsLast(Comparator.naturalOrder())))));
        final var aksjonspunktresultater = lengsteFristPrType.entrySet()
            .stream()
            .map(e -> AksjonspunktResultat.opprettForAksjonspunktMedFrist(mapTilDefinisjon(e.getKey()), mapTilVenteårsak(e.getKey()), e.getValue().getFrist() == null ? LocalDateTime.now().plus(ventePeriode) : e.getValue().getFrist())).toList();
        log.info("Aksjonspunktresultatfrist={}, etterlysningfrister={}, harPassertFrist={}, now={}",
            aksjonspunktresultater.stream().map(AksjonspunktResultat::getFrist).toList(),
            etterlysningerSomVenterPåSvar.stream().map(Etterlysning::getFrist).toList(),
            etterlysningerSomVenterPåSvar.stream().map(it -> harPassertFrist(it.getFrist())).toList(),
            LocalDateTime.now());
        return aksjonspunktresultater;
    }

    private static boolean harPassertFrist(LocalDateTime frist) {
        return frist != null && frist.isBefore(LocalDateTime.now());
    }

    private static AksjonspunktDefinisjon mapTilDefinisjon(EtterlysningType type) {
        switch (type) {
            case UTTALELSE_KONTROLL_INNTEKT -> {
                return AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE;
            }
            case UTTALELSE_ENDRET_STARTDATO, UTTALELSE_ENDRET_SLUTTDATO -> {
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
            case UTTALELSE_ENDRET_STARTDATO, UTTALELSE_ENDRET_SLUTTDATO -> {
                return Venteårsak.VENTER_BEKREFTELSE_ENDRET_UNGDOMSPROGRAMPERIODE;
            }
            default -> throw new IllegalArgumentException("Ukjent etterlysningstype: " + type);
        }
    }

}
