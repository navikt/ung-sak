package no.nav.k9.sak.infotrygdfeed;

import no.nav.k9.sak.typer.Saksnummer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class InfotrygdFeedPeriodeberegnerMockHelper {
    private final InfotrygdFeedPeriodeberegnerTestParametere param;

    // Builder-parametere
    private Saksnummer saksnummer = new Saksnummer("x123");
    private List<TestPeriode> perioder = new ArrayList<>();
    private boolean harTreffITjeneste = true;

    protected InfotrygdFeedPeriodeberegnerMockHelper(InfotrygdFeedPeriodeberegnerTestParametere param) {
        this.param = param;
    }

    InfotrygdFeedPeriodeberegnerMockHelper medSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
        return this;
    }

    InfotrygdFeedPeriodeberegnerMockHelper medInnvilgetPeriode(LocalDate fom, LocalDate tom) {
        perioder.add(new TestPeriode(fom, tom, true));
        return this;
    }

    InfotrygdFeedPeriodeberegnerMockHelper medAvslåttPeriode(LocalDate fom, LocalDate tom) {
        perioder.add(new TestPeriode(fom, tom, false));
        return this;
    }

    InfotrygdFeedPeriodeberegnerMockHelper utenTreffITjeneste() {
        harTreffITjeneste = false;
        return this;
    }

    public void mock() {
        param.konfigurerMock(saksnummer, perioder, harTreffITjeneste);
    }
}
