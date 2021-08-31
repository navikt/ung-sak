package no.nav.k9.sak.domene.registerinnhenting;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.k9.sak.domene.registerinnhenting.impl.RegisterinnhentingHistorikkinnslagTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/**
 * Oppdaterer registeropplysninger for engangsstønader og skrur behandlingsprosessen tilbake
 * til innhent-steget hvis det har skjedd endringer siden forrige innhenting.
 */
@Dependent
public class RegisterdataEndringshåndterer {

    private TemporalAmount oppdatereRegisterdataTidspunkt;
    private BehandlingRepository behandlingRepository;
    private Endringskontroller endringskontroller;
    private EndringsresultatSjekker endringsresultatSjekker;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private BehandlingÅrsakTjeneste behandlingÅrsakTjeneste;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    RegisterdataEndringshåndterer() {
        // for CDI proxy
    }

    /**
     * @param periode - Periode for hvor ofte registerdata skal oppdateres
     */
    @Inject
    public RegisterdataEndringshåndterer( // NOSONAR jobber med å redusere
                                          BehandlingRepositoryProvider repositoryProvider,
                                          @KonfigVerdi(value = "oppdatere.registerdata.tidspunkt", defaultVerdi = "PT10H") String oppdaterRegisterdataEtterPeriode,
                                          Endringskontroller endringskontroller,
                                          EndringsresultatSjekker endringsresultatSjekker,
                                          RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste,
                                          BehandlingÅrsakTjeneste behandlingÅrsakTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {

        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
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

    public void utledDiffOgReposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatSnapshot grunnlagSnapshot) {
        // Utled diff hvis registerdata skal oppdateres
        EndringsresultatDiff endringsresultat = oppdaterRegisteropplysninger(behandling, grunnlagSnapshot);

        doReposisjonerBehandlingVedEndringer(behandling, endringsresultat, true);
    }

    private EndringsresultatDiff oppdaterRegisteropplysninger(Behandling behandling, EndringsresultatSnapshot grunnlagSnapshot) {
        // Finn alle endringer som registerinnhenting har gjort på behandlingsgrunnlaget
        EndringsresultatDiff endringsresultat = endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(behandling.getId(), grunnlagSnapshot);
        return endringsresultat;
    }

    private void lagBehandlingÅrsakerOgHistorikk(Behandling behandling, EndringsresultatDiff endringsresultat) {
        var ref = BehandlingReferanse.fra(behandling, this.skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        Set<BehandlingÅrsakType> behandlingÅrsakTyper = new HashSet<>(behandlingÅrsakTjeneste.utledBehandlingÅrsakerBasertPåDiff(ref, endringsresultat));
        leggTilBehandlingsårsaker(behandling, behandlingÅrsakTyper);
        lagHistorikkinnslag(behandling, behandlingÅrsakTyper);
    }

    private void leggTilBehandlingsårsaker(Behandling behandling, Set<BehandlingÅrsakType> årsakTyper) {
        BehandlingÅrsak.Builder builder = BehandlingÅrsak.builder(new ArrayList<>(årsakTyper));
        builder.buildFor(behandling);
    }

    private void lagHistorikkinnslag(Behandling behandling, Set<BehandlingÅrsakType> behandlingÅrsakTyper) {
        if (behandlingÅrsakTyper.contains(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD);
            return;
        }
        if (behandlingÅrsakTyper.contains(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_YTELSER)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_YTELSER);
            return;
        }
        if (behandlingÅrsakTyper.contains(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON) && harEnÅrsakSomIndikererHvaViHarInnhentet(behandlingÅrsakTyper)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(behandling, utledEndringFraAnnenSak(behandlingÅrsakTyper));
            return;
        }
        var redusert = behandlingÅrsakTyper.stream()
            .filter(it -> !BehandlingÅrsakType.ANNEN_OMSORGSPERSON_TYPER.contains(it))
            .filter(it -> !Objects.equals(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON, it))
            .collect(Collectors.toList());
        if (redusert.isEmpty()) {
            return;
        }
        historikkinnslagTjeneste.opprettHistorikkinnslagForNyeRegisteropplysninger(behandling);
    }

    private boolean harEnÅrsakSomIndikererHvaViHarInnhentet(Set<BehandlingÅrsakType> behandlingÅrsakTyper) {
        return behandlingÅrsakTyper.stream().anyMatch(BehandlingÅrsakType.ANNEN_OMSORGSPERSON_TYPER::contains);
    }

    private BehandlingÅrsakType utledEndringFraAnnenSak(Set<BehandlingÅrsakType> behandlingÅrsakTyper) {
        return behandlingÅrsakTyper.stream()
            .filter(BehandlingÅrsakType.ANNEN_OMSORGSPERSON_TYPER::contains)
            .findAny()
            .orElse(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON);
    }
}
