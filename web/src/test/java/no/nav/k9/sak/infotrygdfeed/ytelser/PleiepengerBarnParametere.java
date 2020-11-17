package no.nav.k9.sak.infotrygdfeed.ytelser;

import static org.mockito.Mockito.mock;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplanperiode;
import no.nav.k9.sak.infotrygdfeed.InfotrygdFeedPeriodeberegnerTestParametere;
import no.nav.k9.sak.infotrygdfeed.TestPeriode;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.PleiepengerBarnInfotrygdFeedPeriodeberegner;
import org.mockito.Mock;
import org.mockito.Mockito;


import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.mockito.Mockito.when;

public class PleiepengerBarnParametere implements InfotrygdFeedPeriodeberegnerTestParametere {
    @Mock
    UttakTjeneste uttakTjeneste;

    @Override
    public InfotrygdFeedPeriodeberegner newInfotrygdFeedPeriodeBeregner() {
        return new PleiepengerBarnInfotrygdFeedPeriodeberegner(uttakTjeneste);
    }

    @Override
    public FagsakYtelseType fagsakYtelseType() {
        return FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
    }

    @Override
    public String infotrygdKode() {
        return "PN";
    }

    @Override
    public void konfigurerMock(Saksnummer saksnummer, List<TestPeriode> perioder, boolean harTreffITjeneste) {

        uttakTjeneste = mock(UttakTjeneste.class);

        if(!harTreffITjeneste) {
            return;
        }
        if(saksnummer == null) {
            return;
        }
        Uttaksplan uttaksplan = mockUttaksplan(perioder);
        when(uttakTjeneste.hentUttaksplaner(List.of(saksnummer)))
            .thenReturn(Map.of(saksnummer, uttaksplan));
    }

    private Uttaksplan mockUttaksplan(List<TestPeriode> testPerioder) {
        Uttaksplan uttaksplan = Mockito.mock(Uttaksplan.class);

        NavigableMap<Periode, Uttaksplanperiode> perioder = new TreeMap<>();

        for(TestPeriode testPeriode : testPerioder) {
            Periode periode = new Periode(testPeriode.getFom(), testPeriode.getTom());
            Uttaksplanperiode uttaksplanperiode = Mockito.mock(Uttaksplanperiode.class);
            if(testPeriode.isInnvilget()) {
                when(uttaksplanperiode.getUtfall()).thenReturn(UtfallType.INNVILGET);
            } else {
                when(uttaksplanperiode.getUtfall()).thenReturn(UtfallType.AVSLÅTT);
            }
            perioder.put(periode, uttaksplanperiode);
        }

        when(uttaksplan.getPerioder()).thenReturn(perioder);

        return uttaksplan;
    }
}
