package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.omsorg.BarnRelasjon;
import no.nav.k9.sak.kontrakt.omsorg.OmsorgenForDto;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;

@ExtendWith(CdiAwareExtension.class)
class OmsorgenForDtoMapperTest {
    @Inject
    OmsorgenForDtoMapper dtoMapper;

    @Test
    public void toOmsorgenForDtoListeCase1Test() {
        List<OmsorgenForPeriode> omsorgenForPerioder = Arrays.asList(mockOmsorgenForPeriode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 15)));

        LocalDateTimeline<Boolean> tidslinjeTilVurdering = SykdomUtils.toLocalDateTimeline(Arrays.asList(new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 20))));

        List<OmsorgenForDto> omsorgenForDtos = dtoMapper.toOmsorgenForDtoListe(omsorgenForPerioder, true, tidslinjeTilVurdering);

        assertThat(omsorgenForDtos.size()).isEqualTo(2);

        assertThat(omsorgenForDtos.get(0).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 9)));
        assertThat(omsorgenForDtos.get(0).isReadOnly()).isTrue();

        assertThat(omsorgenForDtos.get(1).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 15)));
        assertThat(omsorgenForDtos.get(1).isReadOnly()).isFalse();
    }

    @Test
    public void toOmsorgenForDtoListeCase2Test() {
        List<OmsorgenForPeriode> omsorgenForPerioder = Arrays.asList(mockOmsorgenForPeriode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 20)));

        LocalDateTimeline<Boolean> tidslinjeTilVurdering =
            SykdomUtils.toLocalDateTimeline(Arrays.asList(
                new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 5)),
                new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 25))));

        List<OmsorgenForDto> omsorgenForDtos = dtoMapper.toOmsorgenForDtoListe(omsorgenForPerioder, true, tidslinjeTilVurdering);

        assertThat(omsorgenForDtos.size()).isEqualTo(3);

        assertThat(omsorgenForDtos.get(0).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 5)));
        assertThat(omsorgenForDtos.get(0).isReadOnly()).isFalse();

        assertThat(omsorgenForDtos.get(1).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 6), LocalDate.of(2021, 2, 9)));
        assertThat(omsorgenForDtos.get(1).isReadOnly()).isTrue();

        assertThat(omsorgenForDtos.get(2).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 20)));
        assertThat(omsorgenForDtos.get(2).isReadOnly()).isFalse();
    }

    private OmsorgenForPeriode mockOmsorgenForPeriode(LocalDate fom, LocalDate tom) {
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        return new OmsorgenForPeriode(periode, BarnRelasjon.FAR, "relasjon", "fordi test", Resultat.IKKE_OPPFYLT);
    }
}
