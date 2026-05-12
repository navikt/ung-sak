package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.ManuellVurderingBostedsvilkårDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;

/**
 * Oppdaterer for aksjonspunkt 5144 – manuell vurdering av bostedsvilkåret.
 * Brukes ved årsak ANNET, mottatt uttalelse fra bruker, eller auto-fakta fra søknad.
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = ManuellVurderingBostedsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellVurderingBostedsvilkårOppdaterer implements AksjonspunktOppdaterer<ManuellVurderingBostedsvilkårDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    ManuellVurderingBostedsvilkårOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ManuellVurderingBostedsvilkårOppdaterer(BehandlingRepository behandlingRepository,
                                                    HistorikkinnslagRepository historikkinnslagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(ManuellVurderingBostedsvilkårDto dto, AksjonspunktOppdaterParameter param) {
        var resultatBuilder = param.getVilkårResultatBuilder();
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);

        for (VilkårPeriodeVurderingDto vurdertPeriode : dto.getVurdertePerioder()) {
            Utfall utfall = vurdertPeriode.erVilkårOppfylt() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vurdertPeriode.periode().getFom(), vurdertPeriode.periode().getTom())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(vurdertPeriode.avslagsårsak())
                .medBegrunnelse(vurdertPeriode.begrunnelse())
                .medFritekstVurderingBrev(vurdertPeriode.fritekstVurderingBrev()));
        }
        resultatBuilder.leggTil(vilkårBuilder);

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Manuell vurdering av bostedsvilkåret lagret")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }
}
