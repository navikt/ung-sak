package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;


public class InntektskategoriTilAktivitetstatusMapperTest {

    @Test
    public void skal_verifisere_at_det_finnes_mapping_for_alle_inntektskategorier_bortsett_fra_udefinert() {
        of(Inntektskategori.values())
            .filter(k -> !k.equals(Inntektskategori.UDEFINERT))
            .forEach(k ->
                assertThatCode(() ->
                    TilkjentYtelseOppdaterer.InntektskategoriTilAktivitetstatusMapper.aktivitetStatusFor(k)
                )
                    .as("Finner ingen mapping for inntekskategori %s, ta stilling til om denen skal legges til i %s", k, TilkjentYtelseOppdaterer.InntektskategoriTilAktivitetstatusMapper.class)
                    .doesNotThrowAnyException()
            );
    }

    @Test
    public void skal_ikke_finnes_mapping_for_inntekskategori_udefinert_og_derfor_feile() {
        assertThatThrownBy(
            () -> TilkjentYtelseOppdaterer.InntektskategoriTilAktivitetstatusMapper.aktivitetStatusFor(Inntektskategori.UDEFINERT)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Mangler mapping for inntektskategori")
            .hasMessageContaining("UDEFINERT");
    }
}
