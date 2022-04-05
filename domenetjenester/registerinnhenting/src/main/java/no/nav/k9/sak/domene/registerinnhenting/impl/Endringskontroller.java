package no.nav.k9.sak.domene.registerinnhenting.impl;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.StartpunktRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktTjeneste;
import no.nav.k9.sak.domene.registerinnhenting.KontrollerFaktaAksjonspunktUtleder;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/**
 * Denne klassen er en utvidelse av {@link BehandlingskontrollTjeneste} som håndterer oppdatering på åpen behandling.
 * <p>
 * Ikke endr denne klassen dersom du ikke har en komplett forståelse av hvordan denne protokollen fungerer.
 */
@Dependent
public class Endringskontroller {
    private static final Logger LOGGER = LoggerFactory.getLogger(Endringskontroller.class);
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private Instance<KontrollerFaktaAksjonspunktUtleder> kontrollerFaktaTjenester;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Instance<EndringStartpunktTjeneste> startpunktTjenester;

    Endringskontroller() {
        // For CDI proxy
    }

    @Inject
    public Endringskontroller(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                              @Any Instance<EndringStartpunktTjeneste> startpunktTjenester,
                              OppgaveTjeneste oppgaveTjeneste,
                              RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste,
                              @Any Instance<KontrollerFaktaAksjonspunktUtleder> kontrollerFaktaTjenester,
                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.startpunktTjenester = startpunktTjenester;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.kontrollerFaktaTjenester = kontrollerFaktaTjenester;
    }

    public boolean erRegisterinnhentingPassert(Behandling behandling) {
        return behandling.getType().erYtelseBehandlingType() && behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.INNHENT_REGISTEROPP);
    }

    public void spolTilStartpunkt(Behandling behandling, EndringsresultatDiff endringsresultat) {
        Long behandlingId = behandling.getId();
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        if (behandling.getFagsakYtelseType() == FagsakYtelseType.FRISINN) {
            //kun FRISINN som bruker gosys for produksjonsstyring, de andre ytelsene håndteres i k9-los
            avsluttOppgaverIGsak(behandling, behandling.getStatus());
        }

        StartpunktType startpunkt = FagsakYtelseTypeRef.Lookup.find(startpunktTjenester, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()))
            .utledStartpunktForDiffBehandlingsgrunnlag(ref, endringsresultat);

        if (startpunkt == null || startpunkt.equals(StartpunktType.UDEFINERT)) {
            LOGGER.info("Fant ingen endringer ved diff av grunnlag");
            return; // Ingen detekterte endringer - ingen tilbakespoling
        }

        doSpolTilStartpunkt(ref, behandling, startpunkt);
    }

    private void doSpolTilStartpunkt(BehandlingReferanse ref, Behandling behandling, StartpunktType startpunktType) {
        BehandlingStegType fraSteg = behandling.getAktivtBehandlingSteg();
        BehandlingStegType tilSteg = behandlingskontrollTjeneste.finnBehandlingSteg(startpunktType, behandling.getFagsakYtelseType(), behandling.getType());

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        // Inkluderer tilbakeføring samme steg UTGANG->INNGANG
        boolean tilbakeføres = skalTilbakeføres(behandling, fraSteg, tilSteg);

        // Gjør aksjonspunktutledning utenom steg kun hvis man har passert KOFAK og evt hopper tilbake. Dette pga KOARB.UT->INN
        if (harUtførtKontrollerFakta(behandling)) {
            BehandlingStegType funnetSteg = tilbakeføres ? tilSteg : fraSteg;
            List<AksjonspunktResultat> aksjonspunktResultater = utledAksjonspunkterTilHøyreForStartpunkt(ref, startpunktType);
            behandlingskontrollTjeneste.lagreAksjonspunktResultat(kontekst, funnetSteg, aksjonspunktResultater);
        }

        if (tilbakeføres) {
            // Eventuelt ta behandling av vent
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
            // Spol tilbake
            behandlingskontrollTjeneste.behandlingTilbakeføringHvisTidligereBehandlingSteg(kontekst, tilSteg);
        }
        loggSpoleutfall(behandling, fraSteg, tilSteg, tilbakeføres);
    }

    private boolean skalTilbakeføres(Behandling behandling, BehandlingStegType fraSteg, BehandlingStegType tilSteg) {
        // Dersom vi står i UTGANG, og skal til samme steg som vi står i, vil det også være en tilbakeføring siden vi går UTGANG -> INNGANG
        int sammenlign = behandlingskontrollTjeneste.sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(), fraSteg, tilSteg);
        return (sammenlign == 0 && BehandlingStegStatus.UTGANG.equals(behandling.getBehandlingStegStatus())) || sammenlign > 0;
    }

    private boolean harUtførtKontrollerFakta(Behandling behandling) {
        return behandling.harSattStartpunkt();
    }

    private void loggSpoleutfall(Behandling behandling, BehandlingStegType førSteg, BehandlingStegType etterSteg, boolean tilbakeført) {
        if (tilbakeført && !førSteg.equals(etterSteg)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForTilbakespoling(behandling, førSteg, etterSteg);
            LOGGER.info("Behandling {} har mottatt en endring som medførte spoling tilbake. Før-steg {}, etter-steg {}", behandling.getId(),
                førSteg.getNavn(), etterSteg.getNavn());// NOSONAR //$NON-NLS-1$
        } else {
            LOGGER.info("Behandling {} har mottatt en endring som ikke medførte spoling tilbake. Før-steg {}, etter-steg {}", behandling.getId(),
                førSteg.getNavn(), etterSteg.getNavn());// NOSONAR //$NON-NLS-1$
        }
    }

    private void avsluttOppgaverIGsak(Behandling behandling, BehandlingStatus før) {
        boolean behandlingIFatteVedtak = BehandlingStatus.FATTER_VEDTAK.equals(før);
        if (behandlingIFatteVedtak ) {
            oppgaveTjeneste.avslutt(behandling.getId(), OppgaveÅrsak.GODKJENN_VEDTAK_VL);
        }
    }

    // Orkestrerer aksjonspunktene for kontroll av fakta som utføres etter et startpunkt
    // Dersom ingen spesifikk KontrollerFaktaTjeneste er angitt for startpunktet, så utføres generell kontroll av fakta
    // (Det er forventet at protokoll for KontrollerFaktaTjeneste vil evolvere i senere leveranser)
    private List<AksjonspunktResultat> utledAksjonspunkterTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunkt) {
        List<AksjonspunktResultat> startpunktSpesfikkeApForKontrollAvFakta = StartpunktRef.Lookup.find(KontrollerFaktaAksjonspunktUtleder.class, kontrollerFaktaTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType(), startpunkt)
            .map(tjeneste -> tjeneste.utledAksjonspunkterTilHøyreForStartpunkt(ref, startpunkt))
            .orElse(Collections.emptyList());
        if (!startpunktSpesfikkeApForKontrollAvFakta.isEmpty()) {
            // Disse må utføres før de generelle kontrollene
            return startpunktSpesfikkeApForKontrollAvFakta;
        }

        List<AksjonspunktResultat> generelleApForKontrollAvFakta = StartpunktRef.Lookup.find(KontrollerFaktaAksjonspunktUtleder.class, kontrollerFaktaTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType(), null)
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + ref.getFagsakYtelseType().getKode() + " behandlingtype: " + ref.getBehandlingType().getKode()))
            .utledAksjonspunkterTilHøyreForStartpunkt(ref, startpunkt);
        return generelleApForKontrollAvFakta;
    }
}
