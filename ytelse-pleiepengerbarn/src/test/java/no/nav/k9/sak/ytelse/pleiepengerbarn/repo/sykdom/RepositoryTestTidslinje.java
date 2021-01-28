package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
class RepositoryTestTidslinje {
    @Mock
    private SykdomVurderingRepository repo;


    @Test
    void hentAlleSaksnummer() {
        repo = mock(SykdomVurderingRepository.class);
        AktørId pleietrengendeAktør = new AktørId(123L);
        Saksnummer s1 = new Saksnummer("456");
        Saksnummer s2 = new Saksnummer("789");

        when(repo.hentAlleSaksnummer(pleietrengendeAktør)).thenReturn(Arrays.asList(s1, s2));

        when(repo.hentAlleSøktePerioder(s1))
            .thenReturn(Arrays.asList(
                new SykdomSøktPeriode(null, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15)),
                new SykdomSøktPeriode(null, LocalDate.of(2021, 1, 16), LocalDate.of(2021, 1, 20))
            ));

        when(repo.hentAlleSøktePerioder(s2))
            .thenReturn(Arrays.asList(
                new SykdomSøktPeriode(null, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 10)),
                new SykdomSøktPeriode(null, LocalDate.of(2021, 1, 16), LocalDate.of(2021, 1, 20))
            ));

        when(repo.hentSaksnummerForSøktePerioder(pleietrengendeAktør)).thenCallRealMethod();

        LocalDateTimeline<HashSet<Saksnummer>> timeline = repo.hentSaksnummerForSøktePerioder(pleietrengendeAktør);

        timeline.stream().count();

    }
}
