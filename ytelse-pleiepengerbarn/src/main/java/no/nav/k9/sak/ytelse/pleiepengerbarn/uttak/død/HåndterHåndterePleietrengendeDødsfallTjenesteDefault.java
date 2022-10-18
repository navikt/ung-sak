package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
public class HåndterHåndterePleietrengendeDødsfallTjenesteDefault extends HåndterePleietrengendeDødsfallTjeneste {

    public Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse) {
        return Optional.empty();
    }

    protected void forlengMedisinskeVilkår(VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode, LocalDate fødselsdato) {

    }

    protected Set<VilkårType> vilkårTyperSomForlengesUtoverAldersvilkårOgMedisinskVilkår() {
        return Set.of();
    }
}
