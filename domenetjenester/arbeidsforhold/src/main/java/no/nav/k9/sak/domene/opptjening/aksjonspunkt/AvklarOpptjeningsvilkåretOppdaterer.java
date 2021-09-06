package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.opptjening.AvklarOpptjeningsvilkårDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOpptjeningsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOpptjeningsvilkåretOppdaterer implements AksjonspunktOppdaterer<AvklarOpptjeningsvilkårDto> {

    private HistorikkTjenesteAdapter historikkAdapter;

    AvklarOpptjeningsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOpptjeningsvilkåretOppdaterer(HistorikkTjenesteAdapter historikkAdapter) {

        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOpptjeningsvilkårDto dto, AksjonspunktOppdaterParameter param) {
        var builder = param.getVilkårResultatBuilder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);

        for (var vilkårPeriodeVurdering : dto.getVilkårPeriodeVurderinger()) {
            Utfall nyttUtfall = vilkårPeriodeVurdering.isErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

            oppdaterUtfallOgLagre(nyttUtfall, vilkårPeriodeVurdering, vilkårBuilder);
        }
        builder.leggTil(vilkårBuilder);
        return OppdateringResultat.utenOveropp();
    }

    private void oppdaterUtfallOgLagre(Utfall utfallType, VilkårPeriodeVurderingDto vilkårPeriode, VilkårBuilder vilkårBuilder) {
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vilkårPeriode.getPeriode().getFom(), vilkårPeriode.getPeriode().getTom())
            .medUtfall(utfallType)
            .medMerknad(utfallType.equals(Utfall.OPPFYLT) ? VilkårUtfallMerknad.fraKode(vilkårPeriode.getInnvilgelseMerknadKode()): null)
            .medAvslagsårsak(!utfallType.equals(Utfall.OPPFYLT) ? Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING : null));
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Utfall nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.OPPTJENINGSVILKARET, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }
}
