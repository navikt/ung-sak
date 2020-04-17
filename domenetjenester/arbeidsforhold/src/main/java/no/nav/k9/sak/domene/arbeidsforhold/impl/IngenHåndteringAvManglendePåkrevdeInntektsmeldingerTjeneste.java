package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.typer.Arbeidsgiver;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class IngenHåndteringAvManglendePåkrevdeInntektsmeldingerTjeneste implements ManglendePåkrevdeInntektsmeldingerTjeneste {

    @Inject
    public IngenHåndteringAvManglendePåkrevdeInntektsmeldingerTjeneste() {
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse behandlingReferanse) {
        return Map.of();
    }
}
