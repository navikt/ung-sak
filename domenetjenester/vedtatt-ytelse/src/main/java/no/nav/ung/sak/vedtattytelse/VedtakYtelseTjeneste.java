package no.nav.ung.sak.vedtattytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.Ytelse;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelsePeriode;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseResponse;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;

import java.util.Collection;
import java.util.Map;

/**
 * Henter lagrede AVP- og UPY-vedtak for en person.
 * <p>
 * Kun gjeldende (aktive) vedtak med minst én innvilget periode som overlapper forespørselen tas med. Periodene kuttes ikke.
 * Kalleren slår opp alle aktørIder for personen, også historiske, slik at modulen ikke er avhengig av PDL.
 */
@ApplicationScoped
public class VedtakYtelseTjeneste {

    private static final Map<FagsakYtelseType, YtelseType> YTELSER = Map.of(
        FagsakYtelseType.UNGDOMSYTELSE, YtelseType.UNGDOMSPROGRAMYTELSE,
        FagsakYtelseType.AKTIVITETSPENGER, YtelseType.AKTIVITETSPENGER);

    private VedtakYtelseRepository vedtakYtelseRepository;

    VedtakYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VedtakYtelseTjeneste(VedtakYtelseRepository vedtakYtelseRepository) {
        this.vedtakYtelseRepository = vedtakYtelseRepository;
    }

    public YtelseResponse hentVedtak(Collection<AktørId> aktørIder, Periode periode) {
        var forespurtPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
        var vedtak = vedtakYtelseRepository.hentAktiveVedtakSomOverlapper(aktørIder, YTELSER.keySet(), periode.getFom(), periode.getTom())
            .stream()
            .filter(v -> harAnvisningSomOverlapper(v, forespurtPeriode))
            .map(VedtakYtelseTjeneste::tilVedtak)
            .toList();
        return new YtelseResponse(vedtak);
    }

    private static boolean harAnvisningSomOverlapper(VedtakYtelse vedtak, DatoIntervallEntitet forespurtPeriode) {
        return vedtak.getAnvisninger().stream().anyMatch(a -> a.getPeriode().overlapper(forespurtPeriode));
    }

    private static Ytelse tilVedtak(VedtakYtelse vedtak) {
        return new Ytelse(
            YTELSER.get(vedtak.getYtelseType()),
            vedtak.getSaksnummer().getVerdi(),
            vedtak.getVedtakReferanse(),
            vedtak.getVedtattTidspunkt(),
            vedtak.getAnvisninger().stream().map(VedtakYtelseTjeneste::tilPeriode).toList());
    }

    private static YtelsePeriode tilPeriode(YtelseAnvist anvisning) {
        return new YtelsePeriode(
            anvisning.getPeriode().getFomDato(),
            anvisning.getPeriode().getTomDato(),
            anvisning.getDagsats().orElse(null),
            anvisning.getBeløp().orElse(null),
            anvisning.getAntallBarn().orElse(null),
            anvisning.getDagsatsBarnetillegg().orElse(null));
    }
}
