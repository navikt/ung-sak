package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.etterlysning.VilkårsvarselInnhold;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@BehandlingÅrsakTypeRef(BehandlingÅrsakType.ENDRET_BISTANDSBEHOV)
public class BistandAvklaringTjeneste implements VilkårsavklaringTjeneste {

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    public BistandAvklaringTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BistandAvklaringTjeneste(VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                    InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste) {
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
    }

    public Map<VilkårsvarselInnhold, UUID> hentForeslåtteAvklaringerSomInnhold(long behandlingId) {
        return tilInnholdMap(vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of()));
    }

    public Map<VilkårsvarselInnhold, UUID> lagreForeslåtteAvklaringer(long behandlingId, Set<BistandAvklaring> nyeAvklaringer) {
        var nyeEntiteter = nyeAvklaringer.stream()
            .map(BistandAvklaringDataMapper::mapTilVilkårPeriodeAvklaring)
            .collect(Collectors.toSet());
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, VilkårType.BISTANDSVILKÅR, nyeEntiteter);
        return tilInnholdMap(lagret);
    }

    private static Map<VilkårsvarselInnhold, UUID> tilInnholdMap(Collection<VilkårPeriodeAvklaring> avklaringer) {
        return avklaringer.stream()
            .collect(Collectors.toMap(BistandAvklaringDataMapper::mapTilBistandAvklaringInnhold, VilkårPeriodeAvklaring::getReferanse));
    }

    @Override
    public void ferdigstillForeslåtteAvklaringer(long behandlingId) {
        vilkårsavklaringGrunnlagRepository.ferdigstillForeslåtteAvklaringer(behandlingId, VilkårType.BISTANDSVILKÅR);
    }

    @Override
    public void settVilkårsperioderTilIkkeVurdertForForeslåtteAvklaringer(long behandlingId) {
        var perioderTidligereVurdertEtterAvklaring = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of())
            .stream()
            .map(VilkårPeriodeAvklaring::getPeriode)
            .toList();
        inngangsvilkårVurderingTjeneste.settVilkårResultatIkkeVurdertForPeriode(behandlingId, VilkårType.BISTANDSVILKÅR, perioderTidligereVurdertEtterAvklaring);
    }

    @Override
    public Optional<Vilkårsavklaring> hentSenesteAvklaringForBehandling(long behandlingId) {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of())
            .stream()
            .max(Comparator.comparing(VilkårPeriodeAvklaring::getVurdertTidspunkt)
                .thenComparing(avklaring -> avklaring.getPeriode().getFomDato()))
            .map(avklaring -> new Vilkårsavklaring(avklaring.getAvklaringtype(), avklaring.getPeriode(), null, null));
    }
}
