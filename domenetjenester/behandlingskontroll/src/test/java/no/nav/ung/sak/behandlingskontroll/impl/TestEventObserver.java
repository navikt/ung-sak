package no.nav.ung.sak.behandlingskontroll.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.ung.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegOvergangEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingEvent;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;

@ApplicationScoped
public class TestEventObserver {

    public static final AtomicBoolean enabled = new AtomicBoolean();
    public static final List<BehandlingEvent> allEvents = new ArrayList<>();

    public static void reset() {
        enabled.set(false);
        allEvents.clear();
    }

    public static void startCapture() {
        reset();
        enabled.set(true);
    }

    public void observer(@Observes BehandlingStegOvergangEvent event) {
        addEvent(event);
    }

    private void addEvent(BehandlingEvent event) {
        if (enabled.get()) {
            allEvents.add(event);
        }
    }

    public void observer(@Observes BehandlingStatusEvent event) {
        addEvent(event);
    }

    public void observer(@Observes AksjonspunktStatusEvent event) {
        addEvent(event);
    }

    public void observer(@Observes BehandlingskontrollEvent event) {
        addEvent(event);
    }

    public void observer(@Observes BehandlingStegStatusEvent event) {
        addEvent(event);
    }

    public static void containsExactly(AksjonspunktDefinisjon[]... ads) {
        List<AksjonspunktStatusEvent> aksjonspunkterEvents = getEvents(AksjonspunktStatusEvent.class);
        assertThat(aksjonspunkterEvents).hasSize(ads.length);
        for (int i = 0; i < ads.length; i++) {
            List<Aksjonspunkt> aps = aksjonspunkterEvents.get(i).getAksjonspunkter();
            assertThat(aps).hasSize(ads[i].length);

            for (int j = 0; j < ads[i].length; j++) {
                assertThat(aps.get(j).getAksjonspunktDefinisjon()).as("(%s, %s)", i, j).isEqualTo(ads[i][j]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static<V> List<V> getEvents(Class<?> cls) {
        return (List<V>) allEvents.stream().filter(p -> cls.isAssignableFrom(p.getClass()))
                .collect(Collectors.toList());
    }

    public static void containsExactly(BehandlingskontrollEvent... bke) {
        List<BehandlingskontrollEvent> behandlingskontrollEvents = getEvents(BehandlingskontrollEvent.class);
        assertThat(behandlingskontrollEvents).hasSize(bke.length);
        for (int i = 0; i < bke.length; i++) {
            BehandlingskontrollEvent minEvent = bke[i];
            assertThat(behandlingskontrollEvents.get(i).getStegType()).as("%s", i).isEqualTo(minEvent.getStegType());
            assertThat(behandlingskontrollEvents.get(i).getStegStatus()).as("%s", i).isEqualTo(minEvent.getStegStatus());
        }
    }

    public static void containsExactly(BehandlingStegOvergangEvent... bsoe) {
        List<BehandlingStegOvergangEvent> overgangEvents = getEvents(BehandlingStegOvergangEvent.class);
        assertThat(overgangEvents).hasSize(bsoe.length);
        for (int i = 0; i < bsoe.length; i++) {
            BehandlingStegOvergangEvent minEvent = bsoe[i];
            assertThat(overgangEvents.get(i).getFraStegType()).as("%s", i).isEqualTo(minEvent.getFraStegType());
            assertThat(hentKode(overgangEvents.get(i).getFraTilstand())).as("%s", i)
                .isEqualTo(hentKode(minEvent.getFraTilstand()));
            assertThat(overgangEvents.get(i).getTilStegType()).as("%s", i).isEqualTo(minEvent.getTilStegType());
            assertThat(hentKode(overgangEvents.get(i).getTilTilstand())).as("%s", i)
                .isEqualTo(hentKode(minEvent.getTilTilstand()));
        }
    }

    public static void containsExactly(BehandlingStegStatusEvent... bsoe) {
        List<BehandlingStegStatusEvent> behandlingStegStatusEvents = getEvents(BehandlingStegStatusEvent.class);
        assertThat(behandlingStegStatusEvents).hasSize(bsoe.length);
        for (int i = 0; i < bsoe.length; i++) {
            BehandlingStegStatusEvent minEvent = bsoe[i];
            assertThat(behandlingStegStatusEvents.get(i).getForrigeStatus()).as("%s:%s", i, minEvent.getStegType()).isEqualTo(minEvent.getForrigeStatus());
            assertThat(behandlingStegStatusEvents.get(i).getNyStatus()).as("%s:%s", i, minEvent.getStegType()).isEqualTo(minEvent.getNyStatus());
        }
    }

    private static String hentKode(Optional<BehandlingStegTilstandSnapshot> behandlingStegTilstand) {
        return behandlingStegTilstand
            .map(BehandlingStegTilstandSnapshot::getStatus)
            .map(Kodeverdi::getKode)
            .orElse("");
    }

}
