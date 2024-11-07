package no.nav.k9.sak.grunnbeløp;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.kalkulus.felles.v1.Beløp;
import no.nav.folketrygdloven.kalkulus.request.v1.HentGrunnbeløpRequest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * no.nav.grunnbeløp.KalkulusTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt
 * (https://github.com/navikt/ft-kalkulus/)
 */
@Dependent
public class GrunnbeløpTjeneste {

    private final KalkulusRestKlient restTjeneste;

    @Inject
    public GrunnbeløpTjeneste(KalkulusRestKlient restTjeneste) {
        this.restTjeneste = restTjeneste;

    }

    public Grunnbeløp hentGrunnbeløp(LocalDate dato) {
        HentGrunnbeløpRequest request = new HentGrunnbeløpRequest(dato);
        var grunnbeløp = restTjeneste.hentGrunnbeløp(request);
        return new Grunnbeløp(
            Beløp.safeVerdi(grunnbeløp.getVerdi()).longValue(),
            DatoIntervallEntitet.fraOgMedTilOgMed(grunnbeløp.getPeriode().getFom(), grunnbeløp.getPeriode().getTom()));
    }


}
