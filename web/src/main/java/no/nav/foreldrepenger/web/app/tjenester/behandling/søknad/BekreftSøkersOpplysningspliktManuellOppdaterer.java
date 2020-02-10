package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.kontrakt.søknad.BekreftSokersOpplysningspliktManuDto;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSokersOpplysningspliktManuDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftSøkersOpplysningspliktManuellOppdaterer implements AksjonspunktOppdaterer<BekreftSokersOpplysningspliktManuDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    protected BekreftSøkersOpplysningspliktManuellOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftSøkersOpplysningspliktManuellOppdaterer(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    @Override
    public OppdateringResultat oppdater(BekreftSokersOpplysningspliktManuDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        final boolean erVilkårOk = dto.getErVilkarOk() &&
            dto.getInntektsmeldingerSomIkkeKommer().stream().filter(imelding -> !imelding.isBrukerHarSagtAtIkkeKommer()).collect(Collectors.toList()).isEmpty();
        leggTilEndretFeltIHistorikkInnslag(dto.getBegrunnelse(), erVilkårOk);

        Avslagsårsak avslagsårsak = erVilkårOk ? null : Avslagsårsak.MANGLENDE_DOKUMENTASJON;
        List<Aksjonspunkt> åpneAksjonspunkter = behandling.getÅpneAksjonspunkter();
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();
        VilkårResultatBuilder vilkårBuilder = param.getVilkårResultatBuilder();
        if (erVilkårOk) {
            // Reverser vedtak uten totrinnskontroll

            behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL)
                .ifPresent(ap -> resultatBuilder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));

            final var builder = vilkårBuilder.hentBuilderFor(VilkårType.SØKERSOPPLYSNINGSPLIKT);
            builder.leggTil(builder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
                .medUtfallManuell(Utfall.OPPFYLT));
            vilkårBuilder.leggTil(builder);

            return resultatBuilder.build();
        } else {
            // Hoppe rett til foreslå vedtak uten totrinnskontroll
            åpneAksjonspunkter.stream()
                .filter(a -> !a.getAksjonspunktDefinisjon().getKode().equals(dto.getKode())) // Ikke seg selv
                .forEach(a -> resultatBuilder.medEkstraAksjonspunktResultat(a.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
            final var builder = vilkårBuilder.hentBuilderFor(VilkårType.SØKERSOPPLYSNINGSPLIKT);
            builder.leggTil(builder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
                .medUtfallManuell(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(avslagsårsak));
            vilkårBuilder.leggTil(builder);

            return resultatBuilder
                .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
                .medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL, AksjonspunktStatus.OPPRETTET)
                .build();
        }
    }

    private void leggTilEndretFeltIHistorikkInnslag(String begrunnelse, Boolean vilkårOppfylt) {
        HistorikkEndretFeltVerdiType tilVerdi = Boolean.TRUE.equals(vilkårOppfylt) ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT;

        if (begrunnelse != null) {
            historikkTjenesteAdapter.tekstBuilder().medBegrunnelse(begrunnelse);
        }
        historikkTjenesteAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.SOKERSOPPLYSNINGSPLIKT, null, tilVerdi)
            .medSkjermlenke(SkjermlenkeType.OPPLYSNINGSPLIKT);
    }
}
