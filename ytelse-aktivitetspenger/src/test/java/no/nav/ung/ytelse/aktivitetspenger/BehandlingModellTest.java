package no.nav.ung.ytelse.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.ytelse.aktivitetspenger.prosess.ProsessModell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Sjekk at alle konfigurasjoner fungerer og har definerte steg implementasjoner. */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingModellTest {

    @ParameterizedTest
    @EnumSource(value = BehandlingType.class, names = {"FØRSTEGANGSSØKNAD", "REVURDERING", "KONTROLL"})
    public void skal_sjekke_alle_definerte_behandlingsteg_konfigurasjoner_har_matchende_steg_implementasjoner(BehandlingType behandlingType) {
        @SuppressWarnings("resource")
        BehandlingModellRepository behandlingModellRepository = CDI.current().select(BehandlingModellRepository.class).get();
        BehandlingModell modell = behandlingModellRepository.getModell(behandlingType, FagsakYtelseType.AKTIVITETSPENGER);
        for (BehandlingStegType stegType : modell.getAlleBehandlingStegTyper()) {
            BehandlingStegModell steg = modell.finnSteg(stegType);
            String description = String.format("Feilet for %s, %s", behandlingType.getKode(), stegType.getKode());
            assertThat(steg).as(description).isNotNull();
            BehandlingSteg behandlingSteg = steg.getSteg();
            assertThat(behandlingSteg).as(description).isNotNull();

            @SuppressWarnings("rawtypes")
            Class targetClass = ((org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy) behandlingSteg).weld_getTargetClass();
            assertThat(targetClass).as(description).hasAnnotation(ApplicationScoped.class);
        }
    }

    @Test
    public void ventepunkt_for_vedtaksprioritet_står_rett_før_beregning_av_ytelse_i_ordinære_modeller_bare_når_bryteren_for_ytelsen_er_på() {
        var prosessModell = new ProsessModell();
        var påForAktivitetspenger = new KontrollbehandlingBryter(false, true);
        var påForUngdomsytelse = new KontrollbehandlingBryter(true, false);

        assertThat(stegFørBeregnYtelse(prosessModell.førstegangsbehandling(påForAktivitetspenger))).isEqualTo(BehandlingStegType.VURDER_VEDTAKSPRIORITET);
        assertThat(stegFørBeregnYtelse(prosessModell.revurdering(påForAktivitetspenger))).isEqualTo(BehandlingStegType.VURDER_VEDTAKSPRIORITET);
        assertThat(prosessModell.førstegangsbehandling(påForUngdomsytelse).getAlleBehandlingStegTyper()).doesNotContain(BehandlingStegType.VURDER_VEDTAKSPRIORITET);
        assertThat(prosessModell.revurdering(påForUngdomsytelse).getAlleBehandlingStegTyper()).doesNotContain(BehandlingStegType.VURDER_VEDTAKSPRIORITET);
    }

    @Test
    public void kontrollbehandling_har_ventepunkt_for_vedtaksprioritet_rett_før_beregning_av_ytelse_og_starter_alle_endringer_i_kompletthetssteget() {
        BehandlingModell modell = new ProsessModell().kontroll();

        assertThat(stegFørBeregnYtelse(modell)).isEqualTo(BehandlingStegType.VURDER_VEDTAKSPRIORITET);
        for (StartpunktType startpunkt : StartpunktType.values()) {
            assertThat(modell.finnBehandlingSteg(startpunkt)).as(startpunkt.getKode()).isEqualTo(BehandlingStegType.VURDER_KOMPLETTHET);
        }
    }

    private static BehandlingStegType stegFørBeregnYtelse(BehandlingModell modell) {
        return modell.finnForrigeSteg(BehandlingStegType.BEREGN_YTELSE).getBehandlingStegType();
    }
}
