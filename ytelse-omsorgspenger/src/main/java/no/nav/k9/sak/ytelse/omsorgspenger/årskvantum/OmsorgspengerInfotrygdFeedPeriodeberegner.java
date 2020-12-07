package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerInfotrygdFeedPeriodeberegner implements InfotrygdFeedPeriodeberegner {
    private ÅrskvantumTjeneste årskvantumTjeneste;

    OmsorgspengerInfotrygdFeedPeriodeberegner() {
        // for CDI
    }

    @Inject
    public OmsorgspengerInfotrygdFeedPeriodeberegner(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public InfotrygdFeedPeriode finnInnvilgetPeriode(Saksnummer saksnummer) {
        Periode periode = årskvantumTjeneste.hentPeriodeForFagsak(saksnummer);
        if (periode == null) {
            return InfotrygdFeedPeriode.annullert();
        }

        return new InfotrygdFeedPeriode(periode.getFom(), periode.getTom());
    }

    @Override
    public String getInfotrygdYtelseKode(Saksnummer saksnummer) {
        return "OM";
    }
}
