package no.nav.ung.sak.samordning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningPeriode;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningVedtak;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningVedtakResponse;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningYtelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;

import java.util.Collection;
import java.util.Map;

/**
 * Henter lagrede AVP- og UPY-vedtak for samordning med andre ytelser.
 * <p>
 * Kun gjeldende (aktive) vedtak med minst én innvilget periode som overlapper forespørselen tas med. Periodene kuttes ikke.
 * Kalleren slår opp alle aktørIder for personen, også historiske, slik at modulen ikke er avhengig av PDL.
 */
@ApplicationScoped
public class SamordningYtelseTjeneste {

    private static final Map<FagsakYtelseType, SamordningYtelse> YTELSER = Map.of(
        FagsakYtelseType.UNGDOMSYTELSE, SamordningYtelse.UNGDOMSPROGRAMYTELSE,
        FagsakYtelseType.AKTIVITETSPENGER, SamordningYtelse.AKTIVITETSPENGER);

    private VedtakYtelseRepository vedtakYtelseRepository;

    SamordningYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SamordningYtelseTjeneste(VedtakYtelseRepository vedtakYtelseRepository) {
        this.vedtakYtelseRepository = vedtakYtelseRepository;
    }

    public SamordningVedtakResponse hentVedtak(Collection<AktørId> aktørIder, Periode periode) {
        var forespurtPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
        var vedtak = vedtakYtelseRepository.hentAktiveVedtakSomOverlapper(aktørIder, YTELSER.keySet(), periode.getFom(), periode.getTom())
            .stream()
            .filter(v -> harAnvisningSomOverlapper(v, forespurtPeriode))
            .map(SamordningYtelseTjeneste::tilVedtak)
            .toList();
        return new SamordningVedtakResponse(vedtak);
    }

    private static boolean harAnvisningSomOverlapper(VedtakYtelse vedtak, DatoIntervallEntitet forespurtPeriode) {
        return vedtak.getAnvisninger().stream().anyMatch(a -> a.getPeriode().overlapper(forespurtPeriode));
    }

    private static SamordningVedtak tilVedtak(VedtakYtelse vedtak) {
        return new SamordningVedtak(
            YTELSER.get(vedtak.getYtelseType()),
            vedtak.getSaksnummer().getVerdi(),
            vedtak.getVedtakReferanse(),
            vedtak.getVedtattTidspunkt(),
            vedtak.getAnvisninger().stream().map(SamordningYtelseTjeneste::tilPeriode).toList());
    }

    private static SamordningPeriode tilPeriode(YtelseAnvist anvisning) {
        return new SamordningPeriode(
            anvisning.getPeriode().getFomDato(),
            anvisning.getPeriode().getTomDato(),
            anvisning.getDagsats().orElse(null),
            anvisning.getBeløp().orElse(null),
            anvisning.getAntallBarn().orElse(null),
            anvisning.getDagsatsBarnetillegg().orElse(null));
    }
}
