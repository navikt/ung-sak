package no.nav.k9.sak.web.app.tjenester.dokument;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.dokument.Kommunikasjonsretning;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.kontrakt.dokument.DokumentDto;

class DokumentRestTjenesteTest {

    private DokumentRestTjeneste tjeneste = new DokumentRestTjeneste();

    @Test
    void utgående_dokument() {
        var nå = LocalDateTime.now();

        var dto = new DokumentDto("");
        dto.setKommunikasjonsretning(Kommunikasjonsretning.UT);
        dto.setTidspunkt(nå);


        var periode1 = new BehandlingPeriode(nå.minusDays(10), nå.minusDays(5), 1L);
        var periode2 = new BehandlingPeriode(nå.minusDays(5).plusNanos(1), nå.minusMinutes(10), 2L);
        var periode3 = new BehandlingPeriode(nå, Tid.TIDENES_ENDE.atStartOfDay(), 3L);

        var set = new TreeSet<>(Set.of(periode1, periode2, periode3));

        var behandling = tjeneste.utledBehandling(dto, set);

        assertThat(behandling).isEqualTo(periode2.getBehandlingId());
    }

    @Test
    void annet_dokument() {
        var nå = LocalDateTime.now();

        var dto = new DokumentDto("");
        dto.setKommunikasjonsretning(Kommunikasjonsretning.INN);
        dto.setTidspunkt(nå);


        var periode1 = new BehandlingPeriode(nå.minusDays(10), nå.minusDays(5), 1L);
        var periode2 = new BehandlingPeriode(nå.minusDays(5).plusNanos(1), nå.minusMinutes(10), 2L);
        var periode3 = new BehandlingPeriode(nå, Tid.TIDENES_ENDE.atStartOfDay(), 3L);

        var set = new TreeSet<>(Set.of(periode1, periode2, periode3));

        var behandling = tjeneste.utledBehandling(dto, set);

        assertThat(behandling).isEqualTo(periode3.getBehandlingId());
    }
}
