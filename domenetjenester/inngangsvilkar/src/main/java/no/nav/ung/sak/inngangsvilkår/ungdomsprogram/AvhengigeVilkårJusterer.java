package no.nav.ung.sak.inngangsvilkår.ungdomsprogram;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.*;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

/**
 * Tjeneste for å justere avhengige vilkår når et vilkår vurderes.
 * Dersom et vilkår vurderes som IKKE_OPPFYLT, fjernes de aktuelle periodene fra
 * alle avhengige vilkår.
 */
@Dependent
public class AvhengigeVilkårJusterer {

    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public AvhengigeVilkårJusterer(VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    /** Fjerner perioder fra avhengige vilkår der et definerende vilkår vurderes som avslått. Definerende vilkår er et vilkår som bestemmer hvilke perioder andre vilkår skal vurderes i. Det er kun innvilgede perioder for definerende vilkår som skal vurderes for avhengige vilkår.
     * <p>
     * For ungdomsytelsen er definerende vilkår ungdomsprogramvilkåret og avhengige vilkår er aldersvilkåret. Se også VilkårsPerioderTilVurde* For ungdomsytelsen er definerende vilkår ungdomsprogramvilkåret og avhengige vilkår er aldersvilkåret. Se også {@link no.nav.ung.sak.perioder.UngdomsytelseVilkårsperioderTilVurderingTjeneste#definerendeVilkår()}
     *
     * @param behandlingId Behandling ID for den behandlingen som skal justeres.
     * @param perioderTilVurdering Perioder som vurderes for definerende vilkår
     * @param avhengigeVilkår Vilkår som er avhengige av det definerende vilkår
     * @param definerendeVilkår Vilkårtype for definerende vilkår som vurderes.
     */
    public void fjernAvslåttePerioderForAvhengigeVilkår(long behandlingId, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Set<VilkårType> avhengigeVilkår, VilkårType definerendeVilkår) {
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        // Finner avslåtte perioder for definerende vilkår som er vurdert i behandlingen
        var avslåttePerioder = vilkårene.getVilkår(definerendeVilkår)
            .stream().map(Vilkår::getPerioder)
            .flatMap(List::stream)
            .filter(p -> perioderTilVurdering.stream().anyMatch(tilVurdering -> tilVurdering.overlapper(p.getPeriode())) && p.getUtfall().equals(Utfall.IKKE_OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .toList();

        if (avslåttePerioder.isEmpty()) {
            return; // Ingenting å justere
        }

        // Justerer alle avhengige vilkår (fjerner perioder)
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);

        for (var type : avhengigeVilkår) {
            var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(type);
            for (DatoIntervallEntitet datoIntervallEntitet : avslåttePerioder) {
                vilkårBuilder = vilkårBuilder.tilbakestill(datoIntervallEntitet);
            }
            vilkårResultatBuilder.leggTil(vilkårBuilder);
        }

        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());
    }

}
