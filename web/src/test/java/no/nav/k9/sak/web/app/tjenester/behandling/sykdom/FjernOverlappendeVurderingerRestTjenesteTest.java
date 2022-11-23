package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomPeriodeMedEndring;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomVurdering;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomVurderingVersjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.SykdomVurderingRepository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
class FjernOverlappendeVurderingerRestTjenesteTest {
    private SykdomVurderingRestTjeneste tjeneste;

    @Mock
    private SykdomVurderingRepository repo;
    @Captor
    private ArgumentCaptor<PleietrengendeSykdomVurderingVersjon> captor;

    private SykdomVurderingMapper.Sporingsinformasjon mockSporingsinformasjon = new SykdomVurderingMapper.Sporingsinformasjon(null, null, null, null);

    @BeforeEach
    void setUp() {
        tjeneste = new SykdomVurderingRestTjeneste(
            null,
            repo,
            null,
            null,
            null,
            null,
            null);
    }

    @Test
    void overlappIMidtenSkalDeleIntervall() {
        List<PleietrengendeSykdomPeriodeMedEndring> pleietrengendeSykdomPeriodeMedEndringer = Arrays.asList(new PleietrengendeSykdomPeriodeMedEndring(
            new Periode(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 10)),
            true, false,
            createSykdomVurderingOgVersjonMock(0, 1,
                new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 20)))));

        PleietrengendeSykdomVurderingVersjon fasit = createSykdomVurderingOgVersjonMock(1, 1,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 4)),
            new Periode(LocalDate.of(2021, 1, 11), LocalDate.of(2021, 1, 20)));

        tjeneste.fjernOverlappendePerioderFraOverskyggendeVurderinger(pleietrengendeSykdomPeriodeMedEndringer, mockSporingsinformasjon, LocalDateTime.now());
        verify(repo).lagre(captor.capture());

        sjekkPerioder(fasit, captor.getValue());
    }

    @Test
    void overlappIMidtenSkalDeleIntervallMultippel() {
        List<PleietrengendeSykdomPeriodeMedEndring> pleietrengendeSykdomPeriodeMedEndringer = Arrays.asList(
            new PleietrengendeSykdomPeriodeMedEndring(
                new Periode(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 10)),
                true, false,
                createSykdomVurderingOgVersjonMock(0, 1,
                    new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 20)))),
            new PleietrengendeSykdomPeriodeMedEndring(
                new Periode(LocalDate.of(2021, 2, 5), LocalDate.of(2021, 2, 10)),
                true, false,
                createSykdomVurderingOgVersjonMock(0, 1,
                    new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 20))))
        );

        PleietrengendeSykdomVurderingVersjon fasit1 = createSykdomVurderingOgVersjonMock(1, 1,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 4)),
            new Periode(LocalDate.of(2021, 1, 11), LocalDate.of(2021, 1, 20)));

        PleietrengendeSykdomVurderingVersjon fasit2 = createSykdomVurderingOgVersjonMock(1, 1,
            new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 4)),
            new Periode(LocalDate.of(2021, 2, 11), LocalDate.of(2021, 2, 20)));

        tjeneste.fjernOverlappendePerioderFraOverskyggendeVurderinger(pleietrengendeSykdomPeriodeMedEndringer, mockSporingsinformasjon, LocalDateTime.now());
        verify(repo, times(2)).lagre(captor.capture());
        List<PleietrengendeSykdomVurderingVersjon> faktisk = captor.getAllValues().stream().sorted(Comparator.comparing(p -> p.getPerioder().get(0).getFom())).collect(Collectors.toList());

        sjekkPerioder(fasit1, faktisk.get(0));
        sjekkPerioder(fasit2, faktisk.get(1));
    }

    @Test
    void toOverlappendePerioder() {

        PleietrengendeSykdomVurderingVersjon vurderingVersjon = createSykdomVurderingOgVersjonMock(0, 1,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 20)));

        List<PleietrengendeSykdomPeriodeMedEndring> pleietrengendeSykdomPeriodeMedEndringer = Arrays.asList(
            new PleietrengendeSykdomPeriodeMedEndring(
                new Periode(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 10)),
                true, false,
                vurderingVersjon),
            new PleietrengendeSykdomPeriodeMedEndring(
                new Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 19)),
                true, false,
                vurderingVersjon)
            );

        PleietrengendeSykdomVurderingVersjon fasit = createSykdomVurderingOgVersjonMock(1, 1,
            new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 4)),
            new Periode(LocalDate.of(2021, 1, 11), LocalDate.of(2021, 1, 14)),
            new Periode(LocalDate.of(2021, 1, 20), LocalDate.of(2021, 1, 20)));

        tjeneste.fjernOverlappendePerioderFraOverskyggendeVurderinger(pleietrengendeSykdomPeriodeMedEndringer, mockSporingsinformasjon, LocalDateTime.now());
        verify(repo).lagre(captor.capture());

        sjekkPerioder(fasit, captor.getValue());
    }

    private void sjekkPerioder(PleietrengendeSykdomVurderingVersjon fasit, PleietrengendeSykdomVurderingVersjon faktisk) {
        List<Periode> fasitPerioder = fasit.getPerioder().stream().map(p -> new Periode(p.getFom(), p.getTom())).collect(Collectors.toList());
        List<Periode> faktiskePerioder = faktisk.getPerioder().stream().map(p -> new Periode(p.getFom(), p.getTom())).collect(Collectors.toList());
        assertThat(faktiskePerioder.size()).isEqualTo(fasit.getPerioder().size());
        Iterator<Periode> fasiter = fasitPerioder.iterator();
        Iterator<Periode> faktiskiter = faktiskePerioder.iterator();
        while(fasiter.hasNext() && faktiskiter.hasNext()) {
            assertThat(fasiter.next()).isEqualTo(faktiskiter.next());
        }
    }

    private PleietrengendeSykdomVurderingVersjon createSykdomVurderingOgVersjonMock(long versjon, long rangering, Periode... perioder) {
        return new PleietrengendeSykdomVurderingVersjon(
            createSykdomVurderingMock(rangering),
            "",
            Resultat.OPPFYLT,
            Long.valueOf(versjon),
            "",
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            Arrays.asList(perioder)
        );
    }

    private PleietrengendeSykdomVurdering createSykdomVurderingMock(long rangering) {
        var s = new PleietrengendeSykdomVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, Collections.emptyList(), "", LocalDateTime.now());
        s.setRangering(Long.valueOf(rangering));
        return s;
    }
}
