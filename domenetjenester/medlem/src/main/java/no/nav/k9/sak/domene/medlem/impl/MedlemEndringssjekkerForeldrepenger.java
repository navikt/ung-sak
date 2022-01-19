package no.nav.k9.sak.domene.medlem.impl;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.RegisterdataDiffsjekker;

@FagsakYtelseTypeRef
@ApplicationScoped
public class MedlemEndringssjekkerForeldrepenger extends MedlemEndringssjekker {

    @Override
    public RegisterdataDiffsjekker opprettNyDiffer() {
        return new RegisterdataDiffsjekker();
    }
}
