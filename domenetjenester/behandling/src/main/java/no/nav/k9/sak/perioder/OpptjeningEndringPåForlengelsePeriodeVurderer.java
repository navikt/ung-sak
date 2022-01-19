package no.nav.k9.sak.perioder;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårTypeKoder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;

@FagsakYtelseTypeRef
@VilkårTypeRef(VilkårTypeKoder.FP_VK_21)
@VilkårTypeRef(VilkårTypeKoder.FP_VK_23)
@ApplicationScoped
public class OpptjeningEndringPåForlengelsePeriodeVurderer implements EndringPåForlengelsePeriodeVurderer {

    private static final Set<BehandlingÅrsakType> RELEVANTE_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING);

    private ProsessTriggereRepository prosessTriggereRepository;

    OpptjeningEndringPåForlengelsePeriodeVurderer() {
    }

    @Inject
    public OpptjeningEndringPåForlengelsePeriodeVurderer(ProsessTriggereRepository prosessTriggereRepository) {
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    @Override
    public boolean harPeriodeEndring(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        var prosessTriggereOpt = prosessTriggereRepository.hentGrunnlag(input.getBehandlingReferanse().getBehandlingId());

        if (prosessTriggereOpt.isPresent()) {
            var aktuelleTriggere = prosessTriggereOpt.get()
                .getTriggere()
                .stream()
                .filter(it -> it.getPeriode().overlapper(periode))
                .filter(it -> RELEVANTE_ÅRSAKER.contains(it.getÅrsak()))
                .toList();

            return !aktuelleTriggere.isEmpty();
        }
        return false;
    }
}
