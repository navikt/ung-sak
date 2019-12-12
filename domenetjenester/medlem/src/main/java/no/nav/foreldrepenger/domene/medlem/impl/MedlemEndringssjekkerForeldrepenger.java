package no.nav.foreldrepenger.domene.medlem.impl;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;

@FagsakYtelseTypeRef
@ApplicationScoped
public class MedlemEndringssjekkerForeldrepenger extends MedlemEndringssjekker {

    @Override
    public RegisterdataDiffsjekker opprettNyDiffer() {
        return new RegisterdataDiffsjekker();
    }
}
