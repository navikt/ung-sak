package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
public class HåndterHåndterePleietrengendeDødsfallTjenesteDefault implements HåndterePleietrengendeDødsfallTjeneste {

    @Override
    public Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse) {
        return Optional.empty();
    }

    @Override
    public void utvidPerioderVedDødsfall(BehandlingReferanse referanse) {
        //skal ikke utvide
    }
}
