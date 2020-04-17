package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
        ÅrskvantumResultat årskvantumResultat = årskvantumTjeneste.hentÅrskvantumForFagsak(saksnummer);
        if(årskvantumResultat == null) {
            return InfotrygdFeedPeriode.annullert();
        }

        List<UttaksperiodeOmsorgspenger> uttaksperioder = årskvantumResultat.getUttaksperioder().stream()
            .filter(it -> it.getUtfall() == OmsorgspengerUtfall.INNVILGET)
            .collect(Collectors.toList());

        LocalDate fom = uttaksperioder.stream().map(UttaksperiodeOmsorgspenger::getFom).min(Comparator.nullsFirst(Comparator.naturalOrder())).orElse(null);
        LocalDate tom = uttaksperioder.stream().map(UttaksperiodeOmsorgspenger::getTom).max(Comparator.nullsLast(Comparator.naturalOrder())).orElse(null);
        return new InfotrygdFeedPeriode(fom, tom);
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
