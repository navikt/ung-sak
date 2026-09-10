package no.nav.ung.sak.domene.person.pdl;

import no.nav.k9.felles.integrasjon.pdl.Behandlingsnummer;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BehandlingsnummerMapperTest {

    private static final String FEATURE_FLAG = "BRUK_PDL_SPESIFIKKE_BEHANDLINGNUMRE";

    @AfterEach
    void ryddOpp() {
        System.clearProperty(FEATURE_FLAG);
    }

    @Test
    void skalReturnereKunUngdomsprogramytelsenNårFeatureFlaggetErAvUansettYtelsetype() {
        System.setProperty(FEATURE_FLAG, "false");

        assertThat(BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.UDEFINERT))
            .containsExactly(Behandlingsnummer.UNGDOMSPROGRAMYTELSEN);
        assertThat(BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.AKTIVITETSPENGER))
            .containsExactly(Behandlingsnummer.UNGDOMSPROGRAMYTELSEN);
    }

    @Test
    void skalReturnereBehandlingsnummerForAlleKjenteYtelserNårYtelsetypeErUdefinertOgFeatureFlaggetErPå() {
        System.setProperty(FEATURE_FLAG, "true");

        List<Behandlingsnummer> behandlingsnumre = BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.UDEFINERT);

        assertThat(behandlingsnumre).containsExactlyInAnyOrder(
            Behandlingsnummer.AKTIVITETSPENGER,
            Behandlingsnummer.UNGDOMSPROGRAMYTELSEN);
    }

    @Test
    void skalReturnereRiktigBehandlingsnummerForKjenteYtelserNårFeatureFlaggetErPå() {
        System.setProperty(FEATURE_FLAG, "true");

        assertThat(BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.UNGDOMSYTELSE))
            .containsExactly(Behandlingsnummer.UNGDOMSPROGRAMYTELSEN);
        assertThat(BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.AKTIVITETSPENGER))
            .containsExactly(Behandlingsnummer.AKTIVITETSPENGER);
    }

    @Test
    void skalKasteForUkjentYtelsetypeNårFeatureFlaggetErPå() {
        System.setProperty(FEATURE_FLAG, "true");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.OBSOLETE));
    }
}
