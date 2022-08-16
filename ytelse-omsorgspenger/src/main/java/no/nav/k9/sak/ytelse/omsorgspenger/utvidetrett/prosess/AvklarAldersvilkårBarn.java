package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.omsorgspenger.AvklarAldersvilkårBarnDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAldersvilkårBarnDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAldersvilkårBarn implements AksjonspunktOppdaterer<AvklarAldersvilkårBarnDto> {

    private final VilkårType vilkårType = VilkårType.ALDERSVILKÅR_BARN;
    private final Avslagsårsak defaultAvslagsårsak = Avslagsårsak.BARN_OVER_HØYESTE_ALDER;
    private final SkjermlenkeType skjermlenkeType = SkjermlenkeType.PUNKT_FOR_ALDERSVILKÅR_BARN;

    private HistorikkTjenesteAdapter historikkAdapter;
    private VilkårResultatRepository vilkårResultatRepository;

    AvklarAldersvilkårBarn() {
        // for CDI proxy
    }

    @Inject
    AvklarAldersvilkårBarn(HistorikkTjenesteAdapter historikkAdapter,
                           VilkårResultatRepository vilkårResultatRepository) {
        this.historikkAdapter = historikkAdapter;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAldersvilkårBarnDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var originalVilkårTidslinje = vilkårene.getVilkårTimeline(vilkårType);
        Utfall orginaltUtfall = hentUtfall(originalVilkårTidslinje);
        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;

        lagHistorikkInnslag(param, orginaltUtfall, nyttUtfall, dto.getBegrunnelse());

        var vilkårResultatBuilder = param.getVilkårResultatBuilder();
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(vilkårType);

        oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, originalVilkårTidslinje.getMinLocalDate(), originalVilkårTidslinje.getMaxLocalDate());

        vilkårResultatBuilder.leggTil(vilkårBuilder); // lagres utenfor

        return OppdateringResultat.nyttResultat();
    }

    private Utfall hentUtfall(LocalDateTimeline<VilkårPeriode> originalVilkårTidslinje) {
        List<Utfall> utfallene = originalVilkårTidslinje.stream().map(v -> v.getValue().getGjeldendeUtfall()).distinct().toList();
        if (utfallene.size() != 1){
            throw new IllegalArgumentException("Forventet kun en type utfall, men fikk " + utfallene.size());
        }
        return utfallene.get(0);
    }

    private void oppdaterUtfallOgLagre(VilkårBuilder builder, Utfall utfallType, LocalDate fom, LocalDate tom) {
        Avslagsårsak settAvslagsårsak = utfallType.equals(Utfall.OPPFYLT) ? null : defaultAvslagsårsak;
        builder.leggTil(builder.hentBuilderFor(fom, tom)
            .medUtfallManuell(utfallType)
            .medAvslagsårsak(settAvslagsårsak));
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Utfall orginaltUtfall, Utfall nyVerdi, String begrunnelse) {
        HistorikkEndretFeltType historikkEndretFeltType = HistorikkEndretFeltType.ALDERSVILKÅR_BARN;
        historikkAdapter.tekstBuilder()
            .medEndretFelt(historikkEndretFeltType, orginaltUtfall, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(skjermlenkeType);
    }

}
