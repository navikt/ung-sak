package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

/** Sjekk at alle konfigurasjoner fungerer og har definerte steg implementasjoner. */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingModellTest {

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void skal_sjekke_alle_definerte_behandlingsteg_konfigurasjoner_har_matchende_steg_implementasjoner(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        @SuppressWarnings("resource")
        BehandlingModellRepository behandlingModellRepository = CDI.current().select(BehandlingModellRepository.class).get();
        BehandlingModell modell = behandlingModellRepository.getModell(behandlingType, ytelseType);
        for (BehandlingStegType stegType : modell.getAlleBehandlingStegTyper()) {
            BehandlingStegModell steg = modell.finnSteg(stegType);
            String description = String.format("Feilet for %s, %s, %s", ytelseType.getKode(), behandlingType.getKode(), stegType.getKode());
            assertThat(steg).as(description).isNotNull();
            BehandlingSteg behandlingSteg = steg.getSteg();
            assertThat(behandlingSteg).as(description).isNotNull();

            @SuppressWarnings("rawtypes")
            Class targetClass = ((org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy) behandlingSteg).weld_getTargetClass();
            assertThat(targetClass).as(description).hasAnnotation(ApplicationScoped.class);
        }
    }

    public static Stream<Arguments> provideArguments() {
        List<Arguments> params = new ArrayList<>();
        List<FagsakYtelseType> ytelseTyper = List.of(FagsakYtelseType.OMSORGSPENGER_KS, FagsakYtelseType.OMSORGSPENGER_MA);
        List<BehandlingType> behandlingTyper = List.of(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING);
        for (FagsakYtelseType a : ytelseTyper) {
            for (BehandlingType b : behandlingTyper) {
                params.add(Arguments.of(a, b));
            }
        }
        return params.stream();
    }
}
