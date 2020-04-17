package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OmsorgspengerInfotrygdFeedPeriodeberegner implements InfotrygdFeedPeriodeberegner {
    private ÅrskvantumTjeneste årskvantumTjeneste;

    protected OmsorgspengerInfotrygdFeedPeriodeberegner() {
        // for CDI
    }

    public OmsorgspengerInfotrygdFeedPeriodeberegner(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public InfotrygdFeedPeriode finnInnvilgetPeriode(Saksnummer saksnummer) {
        Periode periode = årskvantumTjeneste.hentPeriodeForFagsak(saksnummer);
        if(periode == null) {
            return InfotrygdFeedPeriode.annullert();
        }

        return new InfotrygdFeedPeriode(periode.getFom(), periode.getTom());
    }

    @Override
    public FagsakYtelseType getFagsakYtelseType() {
        return FagsakYtelseType.OMSORGSPENGER;
    }

    @Override
    public String getInfotrygdYtelseKode() {
        return "OM";
    }
}
