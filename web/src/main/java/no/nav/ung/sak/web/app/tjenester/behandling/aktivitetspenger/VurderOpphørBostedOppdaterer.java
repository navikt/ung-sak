package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.OpphørPeriodeVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderOpphørBostedDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderOpphørBostedDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderOpphørBostedOppdaterer implements AksjonspunktOppdaterer<VurderOpphørBostedDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    VurderOpphørBostedOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderOpphørBostedOppdaterer(BehandlingRepository behandlingRepository,
                                        HistorikkinnslagRepository historikkinnslagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderOpphørBostedDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();

        for (OpphørPeriodeVurderingDto vurdertPeriode : dto.getVurdertePerioder()) {
            if (!vurdertPeriode.erOpphør() && (vurdertPeriode.opphørDato() != null || vurdertPeriode.opphørÅrsak() != null)) {
                throw new IllegalArgumentException("erOpphør=false, men opphørDato eller opphørÅrsak er satt for periode " + vurdertPeriode.periode());
            }
        }

        LocalDateTimeline<Boolean> tidslinjeTilVurdering = new LocalDateTimeline<>(
            dto.getVurdertePerioder().stream()
                .map(p -> new LocalDateSegment<>(p.periode().getFom(), p.periode().getTom(), Boolean.TRUE))
                .toList());

//        vurdertAktivitetspengerGrunnlag.deaktiverOppfylteOpphørsresultater(behandlingId, VilkårType.BOSTEDSVILKÅR, tidslinjeTilVurdering);

        for (OpphørPeriodeVurderingDto vurdertPeriode : dto.getVurdertePerioder()) {
//            vurdertAktivitetspengerGrunnlag.lagre(new BostedsvurderingResultat(
//                behandlingId,
//                vurdertPeriode.periode().getFom(),
//                vurdertPeriode.opphørDato(),
//                vurdertPeriode.opphørÅrsak(),
//                OpphørKilde.MANUELL,
//                VilkårType.BOSTEDSVILKÅR,
//                vurdertPeriode.begrunnelse(),
//                vurdertPeriode.fritekstVurderingBrev()
//            ));
        }

//        opphørTjeneste.utledOgLagreVilkår(behandlingId, VilkårType.BOSTEDSVILKÅR, tidslinjeTilVurdering);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Manuell vurdering av opphør for bostedsvilkåret lagret")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }
}
