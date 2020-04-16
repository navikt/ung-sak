package no.nav.k9.sak.infotrygdfeed;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;

import java.util.List;

public interface InfotrygdFeedPeriodeberegnerTestParametere {
    InfotrygdFeedPeriodeberegner newInfotrygdFeedPeriodeBeregner();
    FagsakYtelseType fagsakYtelseType();
    String infotrygdKode();
    void konfigurerMock(Saksnummer saksnummer, List<TestPeriode> perioder, boolean harTreffITjeneste);
}
