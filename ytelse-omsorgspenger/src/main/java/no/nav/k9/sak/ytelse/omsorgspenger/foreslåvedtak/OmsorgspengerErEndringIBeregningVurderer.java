package no.nav.k9.sak.ytelse.omsorgspenger.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ErEndringIBeregningVurderer;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class OmsorgspengerErEndringIBeregningVurderer implements ErEndringIBeregningVurderer {

    public OmsorgspengerErEndringIBeregningVurderer() {
    }

    @Override
    public Map<LocalDate, Boolean> vurderUgunst(BehandlingReferanse orginalBeregning, BehandlingReferanse revurdering, NavigableSet<LocalDate> skjæringstidspunkter) {
        // mapper om output
        return skjæringstidspunkter.stream().collect(Collectors.toMap(v -> v, v -> false));
    }
}
