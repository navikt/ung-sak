package no.nav.ung.sak.domene.person.pdl;

import no.nav.k9.felles.integrasjon.pdl.Behandlingsnummer;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BehandlingsnummerMapperTest {


    @Test
    void skalReturnereBehandlingsnummerForAlleKjenteYtelserNårYtelsetypeErUdefinert() {
        List<Behandlingsnummer> behandlingsnumre = BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.UDEFINERT);

        assertThat(behandlingsnumre).containsExactlyInAnyOrder(
            Behandlingsnummer.AKTIVITETSPENGER,
            Behandlingsnummer.UNGDOMSPROGRAMYTELSEN);
    }

    @Test
    void skalReturnereRiktigBehandlingsnummerForKjenteYtelser() {
        assertThat(BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.UNGDOMSYTELSE))
            .containsExactly(Behandlingsnummer.UNGDOMSPROGRAMYTELSEN);
        assertThat(BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.AKTIVITETSPENGER))
            .containsExactly(Behandlingsnummer.AKTIVITETSPENGER);
    }

    @Test
    void skalKasteForUkjentYtelsetype() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(FagsakYtelseType.OBSOLETE));
    }
}
