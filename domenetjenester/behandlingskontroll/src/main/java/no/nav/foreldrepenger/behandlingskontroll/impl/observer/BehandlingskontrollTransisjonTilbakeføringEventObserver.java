package no.nav.foreldrepenger.behandlingskontroll.impl.observer;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktTilbakeførtEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;

/**
 * Håndtere opprydding i Aksjonspunkt og Vilkår ved overhopp framover eller tilbakeføring.
 */
@ApplicationScoped
public class BehandlingskontrollTransisjonTilbakeføringEventObserver {

    private BehandlingskontrollEventPubliserer eventPubliserer = BehandlingskontrollEventPubliserer.NULL_EVENT_PUB;
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    public BehandlingskontrollTransisjonTilbakeføringEventObserver(BehandlingskontrollServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        this.eventPubliserer = serviceProvider.getEventPubliserer();
    }

    protected BehandlingskontrollTransisjonTilbakeføringEventObserver() {
        // for CDI proxy
    }

    public void observerBehandlingSteg(@Observes BehandlingStegTilbakeføringEvent event) {
        Long behandlingId = event.getBehandlingId();
        Behandling behandling = serviceProvider.hentBehandling(behandlingId);
        BehandlingModell modell = getModell(behandling);
        guardIngenÅpneAutopunkter(behandling);

        BehandlingStegType førsteSteg = event.getFørsteSteg();
        BehandlingStegType sisteSteg = event.getSisteSteg();

        Optional<BehandlingStegStatus> førsteStegStatus = event.getFørsteStegStatus();

        boolean medInngangFørsteSteg = !førsteStegStatus.isPresent() || førsteStegStatus.get().erVedInngang();

        Set<String> aksjonspunktDefinisjonerEtterFra = modell.finnAksjonspunktDefinisjonerFraOgMed(førsteSteg, medInngangFørsteSteg);

        List<Aksjonspunkt> endredeAksjonspunkter = håndterAksjonspunkter(behandling, aksjonspunktDefinisjonerEtterFra, event,
            new HåndterRyddingAvAksjonspunktVedTilbakeføring(serviceProvider, førsteSteg, modell));

        modell.hvertStegFraOgMedTil(førsteSteg, sisteSteg, true)
            .collect(Collectors.toCollection(ArrayDeque::new))
            .descendingIterator() // stepper bakover
            .forEachRemaining(s -> hoppBakover(s, event, førsteSteg, sisteSteg));

        aksjonspunkterTilbakeført(event.getKontekst(), endredeAksjonspunkter, event.getFraStegType());
    }

    protected void hoppBakover(BehandlingStegModell s, BehandlingStegTilbakeføringEvent event, BehandlingStegType førsteSteg,
                               BehandlingStegType sisteSteg) {
        s.getSteg().vedTransisjon(event.getKontekst(), s, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, førsteSteg, sisteSteg);
    }

    protected BehandlingModell getModell(Behandling behandling) {
        return serviceProvider.getBehandlingModellRepository().getModell(behandling.getType(), behandling.getFagsakYtelseType());
    }

    private List<Aksjonspunkt> håndterAksjonspunkter(Behandling behandling, Set<String> mellomliggendeAksjonspunkt,
                                                     BehandlingStegTilbakeføringEvent event, Consumer<Aksjonspunkt> action) {
        List<Aksjonspunkt> endredeAksjonspunkter = behandling.getAksjonspunkter().stream()
            .filter(a -> !a.erAutopunkt()) // Autopunkt skal ikke håndteres; skal alltid være lukket ved tilbakehopp
            .filter(a -> mellomliggendeAksjonspunkt.contains(a.getAksjonspunktDefinisjon().getKode()))
            .collect(Collectors.toList());

        endredeAksjonspunkter.forEach(action);

        serviceProvider.getBehandlingRepository().lagre(behandling, event.getKontekst().getSkriveLås());
        return endredeAksjonspunkter;
    }

    private void guardIngenÅpneAutopunkter(Behandling behandling) {
        Optional<Aksjonspunkt> autopunkt = behandling.getAksjonspunkter().stream()
            .filter(Aksjonspunkt::erAutopunkt)
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .findFirst();

        if (autopunkt.isPresent()) {
            throw new IllegalStateException(
                "Utvikler-feil: Tilbakehopp ikke tillatt for autopunkt '" + //$NON-NLS-1$
                    autopunkt.get().getAksjonspunktDefinisjon().getNavn() + "'"); //$NON-NLS-1$
        }
    }

    private void aksjonspunkterTilbakeført(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter, BehandlingStegType behandlingStegType) {
        if (!aksjonspunkter.isEmpty()) {
            eventPubliserer.fireEvent(new AksjonspunktTilbakeførtEvent(kontekst, aksjonspunkter, behandlingStegType));
        }
    }

}
