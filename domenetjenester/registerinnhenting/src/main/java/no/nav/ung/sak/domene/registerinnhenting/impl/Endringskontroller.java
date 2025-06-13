package no.nav.ung.sak.domene.registerinnhenting.impl;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktTjeneste;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
    private Instance<EndringStartpunktTjeneste> startpunktTjenester;

    Endringskontroller() {
        // For CDI proxy
    }

    @Inject
    public Endringskontroller(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                              @Any Instance<EndringStartpunktTjeneste> startpunktTjenester,
                              OppgaveTjeneste oppgaveTjeneste,
                              RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.startpunktTjenester = startpunktTjenester;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    public boolean erRegisterinnhentingPassert(Behandling behandling) {
        return behandling.getType().erYtelseBehandlingType() && behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.INNHENT_REGISTEROPP);
    }

    public void spolTilStartpunkt(Behandling behandling, EndringsresultatDiff endringsresultat) {
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);

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

        doSpolTilStartpunkt(behandling, startpunkt);
    }

    private void doSpolTilStartpunkt(Behandling behandling, StartpunktType startpunktType) {
        BehandlingStegType fraSteg = Optional.ofNullable(behandling.getAktivtBehandlingSteg()).orElse(behandling.getSisteBehandlingStegTilstand().map(BehandlingStegTilstand::getBehandlingSteg).orElseThrow());
        BehandlingStegType tilSteg = behandlingskontrollTjeneste.finnBehandlingSteg(startpunktType, behandling.getFagsakYtelseType(), behandling.getType());

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        // Inkluderer tilbakeføring samme steg UTGANG->INNGANG
        boolean tilbakeføres = skalTilbakeføres(behandling, fraSteg, tilSteg);

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
        if (behandlingIFatteVedtak) {
            oppgaveTjeneste.avslutt(behandling.getId(), OppgaveÅrsak.GODKJENN_VEDTAK_VL);
        }
    }

}
