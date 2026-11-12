package no.nav.ung.sak.kontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.NavigableSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ManglendeKontrollperioderTjenesteTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void skal_utlede_manglende_kontroll_kun_for_perioder_uten_fritak(boolean fritattForKontroll) {
        var november = YearMonth.of(2026, 11);
        var nå = ZonedDateTime.parse("2026-12-09T12:00:00+01:00[Europe/Oslo]");
        var behandling = Behandling.forFørstegangssøknad(Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, AktørId.dummy())).build();
        var behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        var perioder = mock(MånedsvisTidslinjeUtleder.class);
        when(perioder.finnMånedsvisPeriodisertePerioder(behandling.getId())).thenReturn(
            new LocalDateTimeline<>(november.atDay(10), november.atDay(20), november));
        when(perioder.finnInitielleMånedsvisPeriodisertePerioder(behandling.getId())).thenReturn(LocalDateTimeline.empty());
        InntektskontrollFritakUtleder fritakUtleder = tidslinje -> fritattForKontroll
            ? tidslinje.mapValue(_ -> true) : LocalDateTimeline.empty();
        var kontrollperioder = spy(new RelevanteKontrollperioderUtleder(
            null, perioder, behandlingRepository, new UnitTestLookupInstanceImpl<>(fritakUtleder)));
        doReturn(LocalDateTimeline.empty()).when(kontrollperioder).utledPerioderForKontrollAvInntekt(behandling.getId());
        var tilkjent = mock(TilkjentYtelseRepository.class);
        when(tilkjent.hentKontrollerInntektTidslinje(behandling.getId())).thenReturn(LocalDateTimeline.<KontrollerteInntekter>empty());
        var tjeneste = new ManglendeKontrollperioderTjeneste(perioder, "0 0 7 8 * *", tilkjent, kontrollperioder);

        NavigableSet<DatoIntervallEntitet> manglende;
        try (var tid = mockStatic(ZonedDateTime.class, CALLS_REAL_METHODS)) {
            tid.when(ZonedDateTime::now).thenReturn(nå);
            manglende = tjeneste.finnPerioderForManglendeKontroll(behandling);
        }

        if (!fritattForKontroll) {
            assertThat(manglende).containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(november.atDay(1), november.atEndOfMonth()));
        } else {
            assertThat(manglende).isEmpty();
        }
    }
}
