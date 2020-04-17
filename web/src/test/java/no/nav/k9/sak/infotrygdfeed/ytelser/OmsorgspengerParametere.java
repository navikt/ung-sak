package no.nav.k9.sak.infotrygdfeed.ytelser;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.infotrygdfeed.InfotrygdFeedPeriodeberegnerTestParametere;
import no.nav.k9.sak.infotrygdfeed.TestPeriode;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.OmsorgspengerInfotrygdFeedPeriodeberegner;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

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
        //ÅrskvantumResultat årskvantumResultat = mockÅrskvantumResultat(perioder, harTreffITjeneste);
        //when(årskvantumTjeneste.hentÅrskvantumForFagsak(saksnummer))
        //    .thenReturn(årskvantumResultat);
    }

    private ÅrskvantumResultat mockÅrskvantumResultat(List<TestPeriode> perioder, boolean harTreffITjeneste) {
        if (!harTreffITjeneste) {
            return null;
        }

        ÅrskvantumResultat resultat = Mockito.mock(ÅrskvantumResultat.class);
        List<UttaksperiodeOmsorgspenger> uttaksperioder = mockUttaksperioder(perioder);
        //when(resultat.getUttaksperioder()).thenReturn(uttaksperioder);
        return resultat;
    }

    private List<UttaksperiodeOmsorgspenger> mockUttaksperioder(List<TestPeriode> perioder) {
        return perioder.stream().map(periode -> {
            UttaksperiodeOmsorgspenger uttaksperiode = Mockito.mock(UttaksperiodeOmsorgspenger.class);
            when(uttaksperiode.getPeriode()).thenReturn(new Periode(periode.getFom(), periode.getTom()));
            when(uttaksperiode.getFom()).thenReturn(periode.getFom());
            when(uttaksperiode.getTom()).thenReturn(periode.getTom());

            OmsorgspengerUtfall utfall;
            if (periode.isInnvilget()) {
                utfall = OmsorgspengerUtfall.INNVILGET;
            } else {
                utfall = OmsorgspengerUtfall.AVSLÅTT;
            }

            when(uttaksperiode.getUtfall()).thenReturn(utfall);

            return uttaksperiode;
        }).collect(Collectors.toList());
    }
}
