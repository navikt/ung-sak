package no.nav.ung.sak.web.app.tjenester.behandling;

import no.nav.ung.sak.kontrakt.behandling.ÅrsakOgPerioderDto;

public interface GyldigePerioderForRevurderingPrÅrsakUtleder {


    ÅrsakOgPerioderDto utledPerioder(long fagsakId);

}
