package no.nav.ung.sak.domene.vedtak.samordning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelse;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelseRepository;
import no.nav.ung.sak.behandlingslager.samordning.YtelseAnvist;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningPeriode;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningVedtak;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningVedtakRequest;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningVedtakResponse;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningYtelse;

import java.util.List;
import java.util.Map;

/**
 * Henter lagrede AVP- og UPY-vedtak for samordning med andre ytelser.
 * <p>
 * Kun gjeldende (aktive) vedtak med minst én innvilget periode som overlapper forespørselen tas med. Periodene kuttes ikke.
 */
@ApplicationScoped
public class SamordningYtelseTjeneste {

    private static final Map<FagsakYtelseType, SamordningYtelse> YTELSER = Map.of(
        FagsakYtelseType.UNGDOMSYTELSE, SamordningYtelse.UNGDOMSPROGRAMYTELSE,
        FagsakYtelseType.AKTIVITETSPENGER, SamordningYtelse.AKTIVITETSPENGER);

    private VedtakYtelseRepository vedtakYtelseRepository;
    private AktørTjeneste aktørTjeneste;

    SamordningYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SamordningYtelseTjeneste(VedtakYtelseRepository vedtakYtelseRepository, AktørTjeneste aktørTjeneste) {
        this.vedtakYtelseRepository = vedtakYtelseRepository;
        this.aktørTjeneste = aktørTjeneste;
    }

    public SamordningVedtakResponse hentVedtak(SamordningVedtakRequest request) {
        var aktørIder = aktørTjeneste.hentAlleAktørIderForPersonIdent(request.ident());
        if (aktørIder.isEmpty()) {
            return new SamordningVedtakResponse(List.of());
        }

        var forespurtPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(request.fom(), request.tom());
        var vedtak = vedtakYtelseRepository.hentAktiveVedtakSomOverlapper(aktørIder, YTELSER.keySet(), request.fom(), request.tom())
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
