package no.nav.k9.sak.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.typer.Periode;

class SøknadDtoTjenesteTest {

    @Test
    void skal_håndtere_overlapp_i_perioden() {
        LocalDate førsteJuni = LocalDate.of(2021, 6, 1);
        LocalDate midtIJuni = LocalDate.of(2021, 6, 16);
        LocalDate førsteJuli = LocalDate.of(2021, 7, 1);


        LocalDateTimeline<Boolean> tidslinje1 = new LocalDateTimeline<>(førsteJuni, førsteJuli, true);
        LocalDateTimeline<Boolean> tidslinje2 = new LocalDateTimeline<>(midtIJuni, førsteJuli, true);

        List<Periode> periode = SøknadDtoTjeneste.slårSammenPerioderMedHensynTilOverlapp(List.of(tidslinje1, tidslinje2));

        Assertions.assertThat(periode).hasSize(1);
        Assertions.assertThat(periode.get(0).getFom()).isEqualTo(førsteJuni);
        Assertions.assertThat(periode.get(0).getTom()).isEqualTo(førsteJuli);
    }

    @Test
    void skal_få_to_perioder_hvis_de_ikke_er_siden_av_hverandre() {
        LocalDate førsteJuni = LocalDate.of(2021, 6, 1);
        LocalDate førsteJuli = LocalDate.of(2021, 7, 1);
        LocalDate førsteAugust = LocalDate.of(2021, 8, 1);
        LocalDate førsteSeptember = LocalDate.of(2021, 9, 1);

        LocalDateTimeline<Boolean> tidslinje1 = new LocalDateTimeline<>(førsteJuni, førsteJuli, true);
        LocalDateTimeline<Boolean> tidslinje2 = new LocalDateTimeline<>(førsteAugust, førsteSeptember, true);

        List<Periode> periode = SøknadDtoTjeneste.slårSammenPerioderMedHensynTilOverlapp(List.of(tidslinje1, tidslinje2));

        Assertions.assertThat(periode).hasSize(2);
    }

    @Test
    void skal_få_e_periode_hvis_de_er_siden_av_hverandre() {
        LocalDate førsteJuni = LocalDate.of(2021, 6, 1);
        LocalDate midtIJuni = LocalDate.of(2021, 6, 16);
        LocalDate dagenEttermidtIJuni = LocalDate.of(2021, 6, 17);
        LocalDate førsteJuli = LocalDate.of(2021, 7, 1);

        LocalDateTimeline<Boolean> tidslinje1 = new LocalDateTimeline<>(førsteJuni, midtIJuni, true);
        LocalDateTimeline<Boolean> tidslinje2 = new LocalDateTimeline<>(dagenEttermidtIJuni, førsteJuli, true);

        List<Periode> periode = SøknadDtoTjeneste.slårSammenPerioderMedHensynTilOverlapp(List.of(tidslinje1, tidslinje2));

        Assertions.assertThat(periode).hasSize(1);
        Assertions.assertThat(periode.get(0).getFom()).isEqualTo(førsteJuni);
        Assertions.assertThat(periode.get(0).getTom()).isEqualTo(førsteJuli);
    }
}
