package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import java.util.Map;
import java.util.NavigableSet;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface OmsorgenForGrunnlagMapper {

    static OmsorgenForGrunnlagMapper finnTjeneste(Instance<OmsorgenForGrunnlagMapper> tjenester, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(tjenester, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke " + OmsorgenForGrunnlagMapper.class.getSimpleName() + " for ytelse=" + fagsakYtelseType));
    }

    public Map<DatoIntervallEntitet, OmsorgenForVilkårGrunnlag> map(BehandlingReferanse referanse, NavigableSet<DatoIntervallEntitet> perioder);
}
