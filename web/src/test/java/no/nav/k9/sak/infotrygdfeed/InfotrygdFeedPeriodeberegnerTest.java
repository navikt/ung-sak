package no.nav.k9.sak.infotrygdfeed;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.infotrygdfeed.ytelser.OmsorgspengerParametere;
import no.nav.k9.sak.infotrygdfeed.ytelser.PleiepengerBarnParametere;
import no.nav.k9.sak.typer.Saksnummer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(Parameterized.class)
public class InfotrygdFeedPeriodeberegnerTest {

    @Parameterized.Parameters(name = "{0}")
    public static List<InfotrygdFeedPeriodeberegnerTestParametere> parameters() {
        return List.of(
            new PleiepengerBarnParametere(),
            new OmsorgspengerParametere()
        );
    }

    @Parameterized.Parameter
    public InfotrygdFeedPeriodeberegnerTestParametere param;

    private InfotrygdFeedPeriodeberegner beregner;

    @Before
    public void setUp() throws Exception {
        initMocks(param);
        beregner = newInfotrygdFeedPeriodeBeregner();
    }

    @Test
    @Ignore // midlertidig
    public void med_alle_data() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = fom.plusMonths(1);
        LocalDate etter = tom.plusMonths(1);

        Saksnummer saksnummer = new Saksnummer("x2345");

        mockHelper()
            .medInnvilgetPeriode(fom, tom)
            .medAvslåttPeriode(tom, etter)
            .medSaksnummer(saksnummer)
            .mock();

        InfotrygdFeedPeriode periode = getBeregner().finnInnvilgetPeriode(saksnummer);
        assertThat(periode).isEqualTo(new InfotrygdFeedPeriode(fom, tom));
    }

    @Test
    public void uten_perioder_i_uttak() {
        Saksnummer saksnummer = new Saksnummer("x2345");

        mockHelper()
            .medSaksnummer(saksnummer)
            .mock();

        assertThat(getBeregner().finnInnvilgetPeriode(saksnummer)).isEqualTo(InfotrygdFeedPeriode.annullert());
    }

    @Test
    public void med_bare_avslåtte_perioder() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = fom.plusMonths(1);

        Saksnummer saksnummer = new Saksnummer("x2345");

        mockHelper()
            .medAvslåttPeriode(fom, tom)
            .medSaksnummer(saksnummer)
            .mock();

        assertThat(getBeregner().finnInnvilgetPeriode(saksnummer)).isEqualTo(InfotrygdFeedPeriode.annullert());
    }

    @Test
    @Ignore // midlertidig
    public void med_flere_innvilgede_perioder() {
        LocalDate fomA = LocalDate.of(2020, 1, 1);
        LocalDate tomA = fomA.plusMonths(1);

        LocalDate fomB = LocalDate.of(2021, 1, 1);
        LocalDate tomB = fomB.plusMonths(1);

        Saksnummer saksnummer = new Saksnummer("x2345");

        mockHelper()
            .medInnvilgetPeriode(fomA, tomA)
            .medInnvilgetPeriode(fomB, tomB)
            .medSaksnummer(saksnummer)
            .mock();

        InfotrygdFeedPeriode periode = getBeregner().finnInnvilgetPeriode(saksnummer);
        assertThat(periode).isEqualTo(new InfotrygdFeedPeriode(fomA, tomB));
    }

    @Test
    public void uten_treff_i_tjeneste() {
        mockHelper()
            .utenTreffITjeneste()
            .mock();

        assertThat(getBeregner().finnInnvilgetPeriode(new Saksnummer("123"))).isEqualTo(InfotrygdFeedPeriode.annullert());
    }

    @Test
    public void støtter_fagsak() {
        var ytelsesTypeKode = getBeregner().getClass().getAnnotation(FagsakYtelseTypeRef.class).value();
        assertThat(ytelsesTypeKode).isNotNull();
        assertThat(ytelsesTypeKode).isEqualTo(fagsakYtelseType().getKode());
    }

    @Test
    public void riktig_infotrygdkode() {
        assertThat(getBeregner().getInfotrygdYtelseKode()).isNotNull();
        assertThat(getBeregner().getInfotrygdYtelseKode()).isEqualTo(infotrygdKode());
    }

    private InfotrygdFeedPeriodeberegner getBeregner() {
        return beregner;
    }

    private InfotrygdFeedPeriodeberegner newInfotrygdFeedPeriodeBeregner() {
        return param.newInfotrygdFeedPeriodeBeregner();
    }

    private FagsakYtelseType fagsakYtelseType() {
        return param.fagsakYtelseType();
    }

    private String infotrygdKode() {
        return param.infotrygdKode();
    }

    private InfotrygdFeedPeriodeberegnerMockHelper mockHelper() {
        return new InfotrygdFeedPeriodeberegnerMockHelper(param);
    }
}
