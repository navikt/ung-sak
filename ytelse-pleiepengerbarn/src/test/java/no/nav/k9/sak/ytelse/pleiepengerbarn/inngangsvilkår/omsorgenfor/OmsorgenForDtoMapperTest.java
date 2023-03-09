package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.inngangsvilkår.omsorg.OmsorgenForDtoMapper;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForPeriode;
import no.nav.k9.sak.kontrakt.omsorg.BarnRelasjon;
import no.nav.k9.sak.kontrakt.omsorg.OmsorgenForDto;
import no.nav.k9.sak.typer.Periode;

@ExtendWith(CdiAwareExtension.class)
class OmsorgenForDtoMapperTest {
    @Inject
    OmsorgenForDtoMapper dtoMapper;

    @Test
    public void toOmsorgenForDtoListeCase1Test() {
        List<OmsorgenForPeriode> vurdertOmsorgenForPerioder = List.of(mockOmsorgenForPeriode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 15)));

        LocalDateTimeline<Boolean> tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(Arrays.asList(new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 20))));

        List<OmsorgenForDto> omsorgenForDtos = dtoMapper.toOmsorgenForDtoListe(vurdertOmsorgenForPerioder, true, tidslinjeTilVurdering);

        assertThat(omsorgenForDtos.size()).isEqualTo(2);

        assertThat(omsorgenForDtos.get(0).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 15)));
        assertThat(omsorgenForDtos.get(0).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(0).getResultat()).isEqualTo(Resultat.IKKE_OPPFYLT);

        assertThat(omsorgenForDtos.get(1).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 16), LocalDate.of(2021, 2, 20)));
        assertThat(omsorgenForDtos.get(1).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(1).getResultat()).isEqualTo(Resultat.IKKE_VURDERT);
    }

    @Test
    public void toOmsorgenForDtoListeCase2Test() {
        List<OmsorgenForPeriode> vurdertOmsorgenForPerioder = List.of(mockOmsorgenForPeriode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 20)));

        LocalDateTimeline<Boolean> tidslinjeTilVurdering =
            TidslinjeUtil.tilTidslinjeKomprimert(Arrays.asList(
                new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 5)),
                new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 25))));

        List<OmsorgenForDto> omsorgenForDtos = dtoMapper.toOmsorgenForDtoListe(vurdertOmsorgenForPerioder, true, tidslinjeTilVurdering);

        assertThat(omsorgenForDtos.size()).isEqualTo(3);

        assertThat(omsorgenForDtos.get(0).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 5)));
        assertThat(omsorgenForDtos.get(0).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(0).getResultat()).isEqualTo(Resultat.IKKE_OPPFYLT);

        assertThat(omsorgenForDtos.get(1).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 20)));
        assertThat(omsorgenForDtos.get(1).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(1).getResultat()).isEqualTo(Resultat.IKKE_OPPFYLT);

        assertThat(omsorgenForDtos.get(2).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 21), LocalDate.of(2021, 2, 25)));
        assertThat(omsorgenForDtos.get(2).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(2).getResultat()).isEqualTo(Resultat.IKKE_VURDERT);
    }

    @Test
    public void flere_perioder_i_tidslinjen_hvor_kun_en_er_vurdert() {
        List<OmsorgenForPeriode> vurdertOmsorgenForPerioder = List.of(mockOmsorgenForPeriode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 15)));

        LocalDateTimeline<Boolean> tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(Arrays.asList(
            new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 15)),
            new Periode(LocalDate.of(2021, 2, 17), LocalDate.of(2021, 2, 20))));

        List<OmsorgenForDto> omsorgenForDtos = dtoMapper.toOmsorgenForDtoListe(vurdertOmsorgenForPerioder, true, tidslinjeTilVurdering);

        assertThat(omsorgenForDtos.size()).isEqualTo(2);

        assertThat(omsorgenForDtos.get(0).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 15)));
        assertThat(omsorgenForDtos.get(0).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(0).getResultat()).isEqualTo(Resultat.IKKE_OPPFYLT);

        assertThat(omsorgenForDtos.get(1).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 17), LocalDate.of(2021, 2, 20)));
        assertThat(omsorgenForDtos.get(1).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(1).getResultat()).isEqualTo(Resultat.IKKE_VURDERT);
    }

    @Test
    public void en_lang_tidslinje_hvor_bare_et_subset_er_vurdert_av_saksbehandler() {
        List<OmsorgenForPeriode> vurdertOmsorgenForPerioder = List.of(mockOmsorgenForPeriode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 15)));

        LocalDateTimeline<Boolean> tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(Arrays.asList(new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 20))));

        List<OmsorgenForDto> omsorgenForDtos = dtoMapper.toOmsorgenForDtoListe(vurdertOmsorgenForPerioder, true, tidslinjeTilVurdering);

        assertThat(omsorgenForDtos.size()).isEqualTo(3);

        assertThat(omsorgenForDtos.get(0).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 9)));
        assertThat(omsorgenForDtos.get(0).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(0).getResultat()).isEqualTo(Resultat.IKKE_VURDERT);

        assertThat(omsorgenForDtos.get(1).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 15)));
        assertThat(omsorgenForDtos.get(1).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(1).getResultat()).isEqualTo(Resultat.IKKE_OPPFYLT);

        assertThat(omsorgenForDtos.get(2).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 16), LocalDate.of(2021, 2, 20)));
        assertThat(omsorgenForDtos.get(2).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(2).getResultat()).isEqualTo(Resultat.IKKE_VURDERT);
    }

    @Test
    public void en_lang_tidslinje_hvor_halve_perioden_er_vurdert_av_saksbehandler_samt_lenger_frem_i_tid() {
        List<OmsorgenForPeriode> vurdertOmsorgenForPerioder = List.of(mockOmsorgenForPeriode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 25)));

        LocalDateTimeline<Boolean> tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(Arrays.asList(new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 20))));

        List<OmsorgenForDto> omsorgenForDtos = dtoMapper.toOmsorgenForDtoListe(vurdertOmsorgenForPerioder, true, tidslinjeTilVurdering);

        assertThat(omsorgenForDtos.size()).isEqualTo(2);

        assertThat(omsorgenForDtos.get(0).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 9)));
        assertThat(omsorgenForDtos.get(0).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(0).getResultat()).isEqualTo(Resultat.IKKE_VURDERT);

        assertThat(omsorgenForDtos.get(1).getPeriode()).isEqualTo(new Periode(LocalDate.of(2021, 2, 10), LocalDate.of(2021, 2, 20)));
        assertThat(omsorgenForDtos.get(1).isReadOnly()).isFalse();
        assertThat(omsorgenForDtos.get(1).getResultat()).isEqualTo(Resultat.IKKE_OPPFYLT);

        // her skal ikke den siste perioden fra 20 til 25 returneres fordi det ikke er mulig med omsorgen for-perioder utenfor tidslinjen
    }

    private OmsorgenForPeriode mockOmsorgenForPeriode(LocalDate fom, LocalDate tom) {
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        return new OmsorgenForPeriode(periode, BarnRelasjon.FAR, "relasjon", "fordi test", Resultat.IKKE_OPPFYLT);
    }
}
