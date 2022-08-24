package no.nav.k9.sak.ytelse.opplaeringspenger.vilkår;

import static no.nav.k9.kodeverk.vilkår.VilkårType.NØDVENDIG_OPPLÆRING;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.MaksSøktePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleiepengerEndringIUttakPeriodeUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleiepengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.RevurderingPerioderTjeneste;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
public class OLPVilkårsPerioderTilVurderingTjeneste extends PleiepengerVilkårsPerioderTilVurderingTjeneste {

    // MERK: DERSOM PPN og PSB begynner å divergere, må det vurderes å erstatte arv med komposisjon

    OLPVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public OLPVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER) VilkårUtleder vilkårUtleder,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                                  ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste,
                                                  EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                  RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                  SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                  @FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER) PleiepengerEndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder,
                                                  UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository) {

        super(vilkårUtleder
            , Map.of(VilkårType.MEDLEMSKAPSVILKÅRET, new MaksSøktePeriode(søknadsperiodeTjeneste))
            , vilkårResultatRepository
            , behandlingRepository
            , medisinskGrunnlagTjeneste
            , etablertTilsynTjeneste
            , endringUnntakEtablertTilsynTjeneste
            , revurderingPerioderTjeneste
            , søknadsperiodeTjeneste
            , utsattBehandlingAvPeriodeRepository
            , endringIUttakPeriodeUtleder
        );
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(NØDVENDIG_OPPLÆRING);
    }
}
