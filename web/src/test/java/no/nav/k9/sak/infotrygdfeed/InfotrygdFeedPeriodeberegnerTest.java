package no.nav.k9.sak.infotrygdfeed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.infotrygdfeed.ytelser.OmsorgspengerParametere;
import no.nav.k9.sak.infotrygdfeed.ytelser.PleiepengerBarnParametere;
import no.nav.k9.sak.typer.Saksnummer;

public class InfotrygdFeedPeriodeberegnerTest {

    public static Stream<Arguments> provideArguments() {
        return List.of(
            Arguments.of(new PleiepengerBarnParametere()),
            Arguments.of(new OmsorgspengerParametere())
        ).stream();
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void med_alle_data(InfotrygdFeedPeriodeberegnerTestParametere param) {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = fom.plusMonths(1);
        LocalDate etter = tom.plusMonths(1);

        Saksnummer saksnummer = new Saksnummer("x2345");

        mockHelper(param)
            .medInnvilgetPeriode(fom, tom)
            .medAvslåttPeriode(tom, etter)
            .medSaksnummer(saksnummer)
            .mock();

        InfotrygdFeedPeriode periode = getBeregner(param).finnInnvilgetPeriode(saksnummer);
        assertThat(periode).isEqualTo(new InfotrygdFeedPeriode(fom, tom));
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void uten_perioder_i_uttak(InfotrygdFeedPeriodeberegnerTestParametere param) {
        Saksnummer saksnummer = new Saksnummer("x2345");

        mockHelper(param)
            .medSaksnummer(saksnummer)
            .mock();

        assertThat(getBeregner(param).finnInnvilgetPeriode(saksnummer)).isEqualTo(InfotrygdFeedPeriode.annullert());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void med_bare_avslåtte_perioder(InfotrygdFeedPeriodeberegnerTestParametere param) {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = fom.plusMonths(1);

        Saksnummer saksnummer = new Saksnummer("x2345");

        mockHelper(param)
            .medAvslåttPeriode(fom, tom)
            .medSaksnummer(saksnummer)
            .mock();

        assertThat(getBeregner(param).finnInnvilgetPeriode(saksnummer)).isEqualTo(InfotrygdFeedPeriode.annullert());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void med_flere_innvilgede_perioder(InfotrygdFeedPeriodeberegnerTestParametere param) {
        LocalDate fomA = LocalDate.of(2020, 1, 1);
        LocalDate tomA = fomA.plusMonths(1);

        LocalDate fomB = LocalDate.of(2021, 1, 1);
        LocalDate tomB = fomB.plusMonths(1);

        Saksnummer saksnummer = new Saksnummer("x2345");

        mockHelper(param)
            .medInnvilgetPeriode(fomA, tomA)
            .medInnvilgetPeriode(fomB, tomB)
            .medSaksnummer(saksnummer)
            .mock();

        InfotrygdFeedPeriode periode = getBeregner(param).finnInnvilgetPeriode(saksnummer);
        assertThat(periode).isEqualTo(new InfotrygdFeedPeriode(fomA, tomB));
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void uten_treff_i_tjeneste(InfotrygdFeedPeriodeberegnerTestParametere param) {
        mockHelper(param)
            .utenTreffITjeneste()
            .mock();

        assertThat(getBeregner(param).finnInnvilgetPeriode(new Saksnummer("123"))).isEqualTo(InfotrygdFeedPeriode.annullert());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void støtter_fagsak(InfotrygdFeedPeriodeberegnerTestParametere param) {
        var ytelsesTypeKode = getBeregner(param).getClass().getAnnotation(FagsakYtelseTypeRef.class).value();
        assertThat(ytelsesTypeKode).isNotNull();
        assertThat(ytelsesTypeKode).isEqualTo(param.fagsakYtelseType().getKode());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void riktig_infotrygdkode(InfotrygdFeedPeriodeberegnerTestParametere param) {
        assertThat(getBeregner(param).getInfotrygdYtelseKode(etSaksnummer())).isNotNull();
        assertThat(getBeregner(param).getInfotrygdYtelseKode(etSaksnummer())).isEqualTo(param.infotrygdKode());
    }

    private Saksnummer etSaksnummer() {
        return new Saksnummer("12345");
    }

    private InfotrygdFeedPeriodeberegner getBeregner(InfotrygdFeedPeriodeberegnerTestParametere param) {
        return param.newInfotrygdFeedPeriodeBeregner();
    }

    private InfotrygdFeedPeriodeberegnerMockHelper mockHelper(InfotrygdFeedPeriodeberegnerTestParametere param) {
        return new InfotrygdFeedPeriodeberegnerMockHelper(param);
    }
}
