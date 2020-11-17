package no.nav.k9.sak.infotrygdfeed.ytelser;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.infotrygdfeed.InfotrygdFeedPeriodeberegnerTestParametere;
import no.nav.k9.sak.infotrygdfeed.TestPeriode;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.OmsorgspengerInfotrygdFeedPeriodeberegner;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OmsorgspengerParametere implements InfotrygdFeedPeriodeberegnerTestParametere {

    @Mock
    private ÅrskvantumTjeneste årskvantumTjeneste;

    @Override
    public InfotrygdFeedPeriodeberegner newInfotrygdFeedPeriodeBeregner() {
        return new OmsorgspengerInfotrygdFeedPeriodeberegner(årskvantumTjeneste);
    }

    @Override
    public FagsakYtelseType fagsakYtelseType() {
        return FagsakYtelseType.OMSORGSPENGER;
    }

    @Override
    public String infotrygdKode() {
        return "OM";
    }

    @Override
    public void konfigurerMock(Saksnummer saksnummer, List<TestPeriode> perioder, boolean harTreffITjeneste) {

        årskvantumTjeneste = mock(ÅrskvantumTjeneste.class);

        List<TestPeriode> testPerioder = perioder.stream()
            .filter(TestPeriode::isInnvilget)
            .collect(Collectors.toList());

        Periode periode = null;
        if(harTreffITjeneste) {
            LocalDate fom = testPerioder.stream().map(TestPeriode::getFom).min(Comparator.nullsFirst(Comparator.naturalOrder())).orElse(Tid.TIDENES_BEGYNNELSE);
            LocalDate tom = testPerioder.stream().map(TestPeriode::getTom).max(Comparator.nullsLast(Comparator.naturalOrder())).orElse(Tid.TIDENES_ENDE);
            periode = new Periode(fom, tom);
        }

        when(årskvantumTjeneste.hentPeriodeForFagsak(saksnummer))
            .thenReturn(periode);
    }
}
