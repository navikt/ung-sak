package no.nav.ung.kodeverk.varsel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EtterlysningTypeTest {

    /**
     * Både {@link EtterlysningType#tilAutopunktDefinisjon()} og {@link EtterlysningType#mapTilVenteårsak()} har
     * {@code default -> throw}. Testen fanger opp nye etterlysningstyper som ikke er koblet til autopunkt/venteårsak.
     */
    @Test
    void alle_etterlysningstyper_skal_ha_autopunktdefinisjon_og_venteaarsak() {
        for (EtterlysningType type : EtterlysningType.values()) {
            assertThatCode(type::tilAutopunktDefinisjon)
                .as("tilAutopunktDefinisjon() mangler for %s", type)
                .doesNotThrowAnyException();
            assertThatCode(type::mapTilVenteårsak)
                .as("mapTilVenteårsak() mangler for %s", type)
                .doesNotThrowAnyException();

            assertThat(type.tilAutopunktDefinisjon()).as("autopunkt for %s", type).isNotNull();
            assertThat(type.mapTilVenteårsak()).as("venteårsak for %s", type).isNotNull();
        }
    }
}
