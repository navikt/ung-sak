package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.omsorgspenger.AvklarUtvidetRettDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarUtvidetRettDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarUtvidetRett implements AksjonspunktOppdaterer<AvklarUtvidetRettDto> {

    private HistorikkTjenesteAdapter historikkAdapter;

    AvklarUtvidetRett() {
        // for CDI proxy
    }

    @Inject
    AvklarUtvidetRett(HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarUtvidetRettDto dto, AksjonspunktOppdaterParameter param) {
        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var vilkårBuilder = param.getVilkårResultatBuilder();

        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        var periode = dto.getPeriode();
        oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, periode == null ? null : periode.getFom(), periode == null ? null : periode.getTom());

        return OppdateringResultat.utenOveropp();
    }

    private void oppdaterUtfallOgLagre(VilkårResultatBuilder builder, Utfall utfallType, LocalDate fom, LocalDate tom) {
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.UTVIDETRETT);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
            .medUtfallManuell(utfallType)
            .medAvslagsårsak(!utfallType.equals(Utfall.OPPFYLT) ? Avslagsårsak.IKKE_UTVIDETRETT : null));
        builder.leggTil(vilkårBuilder);
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Utfall nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.UTVIDETRETT, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_UTVIDETRETT);
    }
}
