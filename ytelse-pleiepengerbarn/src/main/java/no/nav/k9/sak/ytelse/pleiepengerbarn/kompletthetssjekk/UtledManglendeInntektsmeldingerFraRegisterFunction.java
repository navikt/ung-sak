package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;

public class UtledManglendeInntektsmeldingerFraRegisterFunction implements BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> {

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    public UtledManglendeInntektsmeldingerFraRegisterFunction(ArbeidsforholdTjeneste arbeidsforholdTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
    }

    @Override
    public Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> apply(BehandlingReferanse referanse, LocalDate localDate) {

        return arbeidsforholdTjeneste.finnArbeidsforholdForIdentPåDag(referanse.getAktørId(), localDate, referanse.getFagsakYtelseType());
    }
}
