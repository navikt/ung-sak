package no.nav.k9.sak.ytelse.pleiepengerbarn;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class PleiepengerBarnInfotrygdFeedPeriodeberegner implements InfotrygdFeedPeriodeberegner {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private UttakTjeneste uttakTjeneste;

    protected PleiepengerBarnInfotrygdFeedPeriodeberegner() {
        // for CDI
    }

    public PleiepengerBarnInfotrygdFeedPeriodeberegner(UttakTjeneste uttakTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
    }

    @Override
    public InfotrygdFeedPeriode finnInnvilgetPeriode(Saksnummer saksnummer) {
        Map<Saksnummer, Uttaksplan> saksnummerUttaksplanMap = uttakTjeneste.hentUttaksplaner(List.of(saksnummer));
        Uttaksplan uttaksplan = saksnummerUttaksplanMap.get(saksnummer);

        if(uttaksplan == null) {
            logger.info("Ingen treff i uttaksplaner. Antar at saken er annullert. Saksnummer: " + saksnummer);
            return InfotrygdFeedPeriode.annullert();
        }

        List<Periode> perioder = uttaksplan.getPerioder().entrySet().stream()
            .filter(e -> Objects.equals(UtfallType.INNVILGET, e.getValue().getUtfall()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        LocalDate fom = perioder.stream().map(Periode::getFom).min(Comparator.naturalOrder()).orElse(null);
        LocalDate tom = perioder.stream().map(Periode::getTom).max(Comparator.naturalOrder()).orElse(null);

        return new InfotrygdFeedPeriode(fom, tom);
    }

    @Override
    public FagsakYtelseType getFagsakYtelseType() {
        return FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
    }

    @Override
    public String getInfotrygdYtelseKode() {
        return "PN";
    }
}
