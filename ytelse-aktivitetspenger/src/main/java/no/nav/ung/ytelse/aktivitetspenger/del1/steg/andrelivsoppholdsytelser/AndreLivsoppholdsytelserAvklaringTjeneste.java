package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.etterlysning.VilkårsvarselInnhold;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@BehandlingÅrsakTypeRef(BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE)
public class AndreLivsoppholdsytelserAvklaringTjeneste implements VilkårsavklaringTjeneste {

    private static final VilkårType VILKÅR_TYPE = VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    public AndreLivsoppholdsytelserAvklaringTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AndreLivsoppholdsytelserAvklaringTjeneste(VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                                     InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste) {
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
    }

    public Map<VilkårsvarselInnhold, UUID> hentForeslåtteAvklaringerSomInnhold(long behandlingId) {
        return tilInnholdMap(hentForeslåtteAvklaringer(behandlingId));
    }

    public Map<VilkårsvarselInnhold, UUID> lagreForeslåtteAvklaringer(long behandlingId, Set<AndreLivsoppholdsytelserAvklaring> nyeAvklaringer) {
        var referanserPerVarselinnhold = hentForeslåtteAvklaringerSomInnhold(behandlingId);
        var nyeEntiteter = nyeAvklaringer.stream()
            .map(avklaring -> AndreLivsoppholdsytelserAvklaringDataMapper.mapTilVilkårPeriodeAvklaring(avklaring, referanseFor(avklaring, referanserPerVarselinnhold)))
            .collect(Collectors.toSet());
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, VILKÅR_TYPE, nyeEntiteter);
        return tilInnholdMap(lagret);
    }

    /**
     * Etterlysningen brukeren allerede har fått peker på referansen til avklaringen. Endres noe som ikke vises for
     * bruker (typisk begrunnelsen), lagres avklaringen på nytt i et nytt grunnlag — da må referansen følge med,
     * ellers ville etterlysningen som beholdes pekt på en avklaring i et deaktivert grunnlag.
     */
    private static UUID referanseFor(AndreLivsoppholdsytelserAvklaring avklaring, Map<VilkårsvarselInnhold, UUID> referanserPerVarselinnholdForEksisterendeAvklaringer) {
        return referanserPerVarselinnholdForEksisterendeAvklaringer.getOrDefault(avklaring.innhold(), UUID.randomUUID());
    }

    private static Map<VilkårsvarselInnhold, UUID> tilInnholdMap(Collection<VilkårPeriodeAvklaring> avklaringer) {
        return avklaringer.stream()
            .collect(Collectors.toMap(AndreLivsoppholdsytelserAvklaringDataMapper::mapTilAvklaringInnhold, VilkårPeriodeAvklaring::getReferanse));
    }

    private Set<VilkårPeriodeAvklaring> hentForeslåtteAvklaringer(long behandlingId) {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VILKÅR_TYPE)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of());
    }

    @Override
    public void ferdigstillForeslåtteAvklaringer(long behandlingId) {
        vilkårsavklaringGrunnlagRepository.ferdigstillForeslåtteAvklaringer(behandlingId, VILKÅR_TYPE);
    }

    @Override
    public void settVilkårsperioderTilIkkeVurdertForForeslåtteAvklaringer(long behandlingId) {
        var perioderTidligereVurdertEtterAvklaring = hentForeslåtteAvklaringer(behandlingId).stream()
            .map(VilkårPeriodeAvklaring::getPeriode)
            .toList();
        inngangsvilkårVurderingTjeneste.settVilkårResultatIkkeVurdertForPeriode(behandlingId, VILKÅR_TYPE, perioderTidligereVurdertEtterAvklaring);
    }

    @Override
    public Optional<Vilkårsavklaring> hentSenesteAvklaringForBehandling(long behandlingId) {
        return hentForeslåtteAvklaringer(behandlingId).stream()
            .max(Comparator.comparing(VilkårPeriodeAvklaring::getVurdertTidspunkt)
                .thenComparing(avklaring -> avklaring.getPeriode().getFomDato()))
            .map(avklaring -> new Vilkårsavklaring(
                avklaring.getAvklaringtype(),
                avklaring.getPeriode(),
                AndreLivsoppholdsytelserAvklaringKildeType.fraKode(avklaring.getKildeKode()),
                avklaring.getKildeFritekst()));
    }
}
