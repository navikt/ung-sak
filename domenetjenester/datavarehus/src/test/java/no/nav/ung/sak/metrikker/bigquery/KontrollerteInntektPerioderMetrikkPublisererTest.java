package no.nav.ung.sak.metrikker.bigquery;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class KontrollerteInntektPerioderMetrikkPublisererTest {

    private final KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste = mock(KontrollerteInntektperioderTjeneste.class);
    private final BehandlingVedtakRepository vedtakRepository = mock(BehandlingVedtakRepository.class);
    private final BigQueryKlient bigQueryKlient = mock(BigQueryKlient.class);

    private final KontrollerteInntektPerioderMetrikkPubliserer publiserer =
        new KontrollerteInntektPerioderMetrikkPubliserer(kontrollerteInntektperioderTjeneste, vedtakRepository, bigQueryKlient, true);

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"OBSOLETE", "UDEFINERT"})
    void skal_ikke_publisere_for_ytelse_som_ikke_er_støttet(FagsakYtelseType ytelse) {
        publiserer.publiserKontrollertePerioderMetrikker(behandlingReferanse(ytelse));

        verifyNoInteractions(kontrollerteInntektperioderTjeneste, vedtakRepository, bigQueryKlient);
    }

    private static BehandlingReferanse behandlingReferanse(FagsakYtelseType ytelse) {
        return BehandlingReferanse.fra(
            ytelse,
            BehandlingType.REVURDERING,
            BehandlingResultatType.INNVILGET,
            AktørId.dummy(),
            new Saksnummer("ABC123"),
            1L,
            2L,
            UUID.randomUUID(),
            Optional.empty(),
            BehandlingStatus.IVERKSETTER_VEDTAK,
            null);
    }
}
