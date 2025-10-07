package no.nav.ung.sak.domene.registerinnhenting;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.ung.sak.domene.registerinnhenting.impl.RegisterinnhentingHistorikkinnslagTjeneste;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Oppdaterer registeropplysninger for engangsstønader og skrur behandlingsprosessen tilbake
 * til innhent-steget hvis det har skjedd endringer siden forrige innhenting.
 */
@Dependent
public class RegisterdataEndringshåndterer {

    public static final Set<BehandlingÅrsakType> REGISTEROPPLYSNING_BEHANDLINGÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD,
        BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
        BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM,
        BehandlingÅrsakType.RE_INNTEKTSOPPLYSNING);

    private TemporalAmount oppdatereRegisterdataTidspunkt;
    private BehandlingRepository behandlingRepository;
    private Endringskontroller endringskontroller;
    private EndringsresultatSjekker endringsresultatSjekker;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private BehandlingÅrsakTjeneste behandlingÅrsakTjeneste;

    RegisterdataEndringshåndterer() {
        // for CDI proxy
    }

    /**
     * @param oppdaterRegisterdataEtterPeriode - periode som angir hvor gammel registerdata skal være for at den skal oppdateres på nytt
     */
    @Inject
    public RegisterdataEndringshåndterer( // NOSONAR jobber med å redusere
                                          BehandlingRepositoryProvider repositoryProvider,
                                          @KonfigVerdi(value = "OPPDATERE_REGISTERDATA_TIDSPUNKT", defaultVerdi = "PT10H") String oppdaterRegisterdataEtterPeriode,
                                          Endringskontroller endringskontroller,
                                          EndringsresultatSjekker endringsresultatSjekker,
                                          RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste,
                                          BehandlingÅrsakTjeneste behandlingÅrsakTjeneste) {

        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.endringskontroller = endringskontroller;
        this.endringsresultatSjekker = endringsresultatSjekker;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.behandlingÅrsakTjeneste = behandlingÅrsakTjeneste;
        if (oppdaterRegisterdataEtterPeriode != null) {
            this.oppdatereRegisterdataTidspunkt = Duration.parse(oppdaterRegisterdataEtterPeriode);
        }
    }

    public boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling) {
        LocalDateTime midnatt = LocalDate.now().atStartOfDay();
        Optional<LocalDateTime> opplysningerOppdatertTidspunkt = behandlingRepository.hentSistOppdatertTidspunkt(behandling.getId());
        if (oppdatereRegisterdataTidspunkt == null) {
            // konfig-verdien er ikke satt
            return erOpplysningerOppdatertTidspunktFør(midnatt, opplysningerOppdatertTidspunkt);
        }
        LocalDateTime nårOppdatereRegisterdata = LocalDateTime.now().minus(oppdatereRegisterdataTidspunkt);
        if (nårOppdatereRegisterdata.isAfter(midnatt)) {
            // konfigverdien er etter midnatt, da skal midnatt gjelde
            return erOpplysningerOppdatertTidspunktFør(midnatt, opplysningerOppdatertTidspunkt);
        }
        // konfigverdien er før midnatt, da skal konfigverdien gjelde
        return erOpplysningerOppdatertTidspunktFør(nårOppdatereRegisterdata, opplysningerOppdatertTidspunkt);
    }

    public void sikreInnhentingRegisteropplysningerVedNesteOppdatering(Behandling behandling) {
        // Flytt oppdatert tidspunkt passe langt tilbale
        behandlingRepository.oppdaterSistOppdatertTidspunkt(behandling, LocalDateTime.now().minusWeeks(1).minusDays(1));
    }

    boolean erOpplysningerOppdatertTidspunktFør(LocalDateTime nårOppdatereRegisterdata,
                                                Optional<LocalDateTime> opplysningerOppdatertTidspunkt) {
        return opplysningerOppdatertTidspunkt.isPresent() && opplysningerOppdatertTidspunkt.get().isBefore(nårOppdatereRegisterdata);
    }

    public void reposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatDiff endringsresultat) {
        if (!endringskontroller.erRegisterinnhentingPassert(behandling)) {
            return;
        }
        doReposisjonerBehandlingVedEndringer(behandling, endringsresultat, false);
    }

    private void doReposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatDiff endringsresultat, boolean utledÅrsaker) {
        if (endringsresultat.erSporedeFeltEndret()) {
            if (utledÅrsaker) {
                lagBehandlingÅrsakerOgHistorikk(behandling, endringsresultat);
            }
            endringskontroller.spolTilStartpunkt(behandling, endringsresultat);
        }
    }

    public void utledDiffOgReposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatSnapshot grunnlagSnapshot, boolean utledÅrsaker) {
        // Utled diff hvis registerdata skal oppdateres
        // Finn alle endringer som registerinnhenting har gjort på behandlingsgrunnlaget
        EndringsresultatDiff endringsresultat = endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(behandling.getId(), grunnlagSnapshot);

        doReposisjonerBehandlingVedEndringer(behandling, endringsresultat, utledÅrsaker);
    }

    private void lagBehandlingÅrsakerOgHistorikk(Behandling behandling, EndringsresultatDiff endringsresultat) {
        var ref = BehandlingReferanse.fra(behandling);
        Set<BehandlingÅrsakType> behandlingÅrsakTyper = new HashSet<>(behandlingÅrsakTjeneste.utledBehandlingÅrsakerBasertPåDiff(ref, endringsresultat));
        leggTilBehandlingsårsaker(behandling, behandlingÅrsakTyper);
        lagHistorikkinnslag(behandling, behandlingÅrsakTyper);
    }

    private void leggTilBehandlingsårsaker(Behandling behandling, Set<BehandlingÅrsakType> årsakTyper) {
        BehandlingÅrsak.Builder builder = BehandlingÅrsak.builder(new ArrayList<>(årsakTyper));
        builder.buildFor(behandling);
    }

    private void lagHistorikkinnslag(Behandling behandling, Set<BehandlingÅrsakType> behandlingÅrsakTyper) {
        if (behandlingÅrsakTyper.isEmpty()) {
            return;
        }
        var registeropplysningårsaker = behandlingÅrsakTyper.stream().filter(REGISTEROPPLYSNING_BEHANDLINGÅRSAKER::contains).collect(Collectors.toSet());
        if (!registeropplysningårsaker.isEmpty()) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(behandling, registeropplysningårsaker);
        } else {
            historikkinnslagTjeneste.opprettHistorikkinnslagForNyeRegisteropplysninger(behandling);
        }

    }

}
