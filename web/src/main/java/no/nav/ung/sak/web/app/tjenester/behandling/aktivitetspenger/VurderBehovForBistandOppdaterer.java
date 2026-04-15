package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
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
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderBehovForBistandDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderBehovForBistandDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderBehovForBistandOppdaterer implements AksjonspunktOppdaterer<VurderBehovForBistandDto> {

    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private HistorikkinnslagRepository historikkinnslagRepository;

    VurderBehovForBistandOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderBehovForBistandOppdaterer(BehandlingRepository behandlingRepository,
                                           @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                           HistorikkinnslagRepository historikkinnslagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderBehovForBistandDto dto, AksjonspunktOppdaterParameter param) {
        if (dto.getVurdertePerioder() == null || dto.getVurdertePerioder().isEmpty()) {
            //for å støtte frontend slik det er implementer i dag
            return oppdaterGammel(dto, param);
        } else {
            return oppdaterNy(dto, param);
        }
    }

    private OppdateringResultat oppdaterNy(VurderBehovForBistandDto dto, AksjonspunktOppdaterParameter param) {
        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(param.getRef().getFagsakYtelseType(), param.getRef().getBehandlingType());
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.BISTANDSVILKÅR);
        LocalDateTimeline<Boolean> tilVurderingTidslinje = TidslinjeUtil.tilTidslinje(perioderTilVurdering);
        LocalDateTimeline<Boolean> inputOppdateres = new LocalDateTimeline<>(dto.getVurdertePerioder().stream().map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), true)).toList());

        LocalDateTimeline<Boolean> uforventedePerioder = inputOppdateres.disjoint(tilVurderingTidslinje);
        if (!uforventedePerioder.isEmpty()) {
            throw new IllegalArgumentException("Forsøker å vurdere perioder som ikke er til vurdering. Gjelder perioder: " + uforventedePerioder);
        }

        //TODO tillat å ikke vurdere perioder som er satt til IKKE_RELEVANT / som har avslag i tidligere vilkår
        LocalDateTimeline<Boolean> manglendePerioder = tilVurderingTidslinje.disjoint(inputOppdateres);
        if (!manglendePerioder.isEmpty()) {
            throw new IllegalArgumentException("Forventer at alle perioder til vurdering vurderes. Mangler : " + manglendePerioder);
        }

        var resultatBuilder = param.getVilkårResultatBuilder();
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BISTANDSVILKÅR);
        for (VilkårPeriodeVurderingDto vurdertPeriode : dto.getVurdertePerioder()) {
            Utfall utfall = vurdertPeriode.erVilkårOppfylt() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vurdertPeriode.periode().getFom(), vurdertPeriode.periode().getTom())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(vurdertPeriode.avslagsårsak())
                .medBegrunnelse(vurdertPeriode.begrunnelse()));
        }
        resultatBuilder.leggTil(vilkårBuilder);

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.BISTANDSVILKÅR)
            .addLinje("Bistandsvilkår ble vurdert")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }

    public OppdateringResultat oppdaterGammel(VurderBehovForBistandDto dto, AksjonspunktOppdaterParameter param) {
        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(param.getRef().getFagsakYtelseType(), param.getRef().getBehandlingType());

        var resultatBuilder = param.getVilkårResultatBuilder();
        var bistandsvilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BISTANDSVILKÅR);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.BISTANDSVILKÅR);
        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            //FIXME AKT implementer regel for automatisk behandling eller opprett aksjonspunkt her
            var periodeBuilderOppfylt = bistandsvilkårBuilder.hentBuilderFor(periode).medUtfall(Utfall.OPPFYLT).medRegelInput("TODO");
            bistandsvilkårBuilder.leggTil(periodeBuilderOppfylt);
        }

        resultatBuilder.leggTil(bistandsvilkårBuilder);

        return OppdateringResultat.nyttResultat();
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, fagsakYtelseType, behandlingType);
    }
}
