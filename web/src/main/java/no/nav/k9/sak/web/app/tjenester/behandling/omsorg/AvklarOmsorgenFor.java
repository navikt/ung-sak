package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.AvklarOmsorgenForDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOmsorgenForDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOmsorgenFor implements AksjonspunktOppdaterer<AvklarOmsorgenForDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private VilkårResultatRepository vilkårResultatRepository;

    AvklarOmsorgenFor() {
        // for CDI proxy
    }

    @Inject
    AvklarOmsorgenFor(VilkårResultatRepository vilkårResultatRepository,
                      HistorikkTjenesteAdapter historikkAdapter) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOmsorgenForDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();

        // skal ha fra før
        var periode = dto.getPeriode();

        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var vilkårBuilder = param.getVilkårResultatBuilder();

        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        if (periode == null) {
            // overskriver hele
            var vilkårene = vilkårResultatRepository.hent(behandlingId);
            var timeline = vilkårene.getVilkårTimeline(VilkårType.OMSORGEN_FOR);
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, timeline.getMinLocalDate(), timeline.getMaxLocalDate());
        } else {
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, periode.getFom(), periode.getTom());
        }

        return OppdateringResultat.utenOveropp();
    }

    private void oppdaterUtfallOgLagre(VilkårResultatBuilder builder, Utfall utfallType, LocalDate fom, LocalDate tom) {

        var vilkårBuilder = builder.hentBuilderFor(VilkårType.OMSORGEN_FOR);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
            .medUtfallManuell(utfallType)
            .medAvslagsårsak(!utfallType.equals(Utfall.OPPFYLT) ? Avslagsårsak.IKKE_DOKUMENTERT_OMSORGEN_FOR : null));
        builder.leggTil(vilkårBuilder);
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Utfall nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.OMSORG_FOR, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_OMSORGEN_FOR);
    }
}
