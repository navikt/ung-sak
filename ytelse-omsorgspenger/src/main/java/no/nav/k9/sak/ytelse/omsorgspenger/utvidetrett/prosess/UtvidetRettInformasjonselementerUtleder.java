package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.registerinnhenting.InformasjonselementerUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@BehandlingTypeRef()
class UtvidetRettInformasjonselementerUtleder implements InformasjonselementerUtleder {
    @Override
    public Set<RegisterdataType> utled(BehandlingType behandlingType) {
        return Set.of(); // ingen inntekter/arbeidsforhold her
    }

}
