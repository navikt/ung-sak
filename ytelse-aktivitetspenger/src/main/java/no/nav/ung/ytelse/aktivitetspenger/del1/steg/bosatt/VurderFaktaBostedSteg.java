package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BosattSøknadGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FAKTA_OM_BOSTED;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FAKTA_OM_BOSTED)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderFaktaBostedSteg implements BehandlingSteg {


    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private BosattSøknadGrunnlagRepository bosattSøknadGrunnlagRepository;
    private Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere;
    private BehandlingRepository behandlingRepository;

    VurderFaktaBostedSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaBostedSteg(BehandlingRepository behandlingRepository,
                                 BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                 BosattSøknadGrunnlagRepository bosattSøknadGrunnlagRepository,
                                 @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                 @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere) {

        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.bosattSøknadGrunnlagRepository = bosattSøknadGrunnlagRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.prosessTriggerPeriodeUtledere = prosessTriggerPeriodeUtledere;
        this.behandlingRepository = behandlingRepository;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType())
            .utled(behandlingId, VilkårType.BOSTEDSVILKÅR);
        initierFaktaFraSøknadsdata(behandlingId, perioderTilVurdering);
        LocalDateTimeline<Boolean> tidslinjeForManuellFaktavurdering = finnTidslinjeForManuellFaktavurdering(behandling, behandlingId);
        // Saksbehandler må vurdere bosted for perioder uten grunnlag — prioritert over vent
        if (!tidslinjeForManuellFaktavurdering.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_FAKTA_OM_BOSTED));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForManuellFaktavurdering(Behandling behandling, long behandlingId) {
        return ProsessTriggerPeriodeUtleder.finnTjeneste(prosessTriggerPeriodeUtledere, behandling.getFagsakYtelseType())
            .utledTidslinje(behandlingId)
            .filterValue(it -> it.contains(BehandlingÅrsakType.ENDRET_BOSTED))
            .mapValue(_ -> true);
    }

    // Dette kan vurderes å flyttes til persistering av søknad. Ulempen er at vi må då må ta stilling til vilkårsperioder tidlig
    private void initierFaktaFraSøknadsdata(long behandlingId, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        Map<LocalDate, Boolean> søknadErBosattPerFom = bosattSøknadGrunnlagRepository.hentSøknadBostedPerFom(behandlingId);
        // Auto-sett fakta fra søknad for perioder uten grunnlag
        Map<LocalDate, Boolean> nyeSøknadAvklaringer = new LinkedHashMap<>();
        perioderTilVurdering.stream().forEach(segment -> {
            LocalDate fom = segment.getFomDato();
            Boolean søknadVerdi = finnSøknadverdi(søknadErBosattPerFom, fom);
            if (søknadVerdi != null) {
                nyeSøknadAvklaringer.put(fom, søknadVerdi);
            }
        });
        if (!nyeSøknadAvklaringer.isEmpty()) {
            bostedsGrunnlagRepository.lagreAvklaringerFraSøknad(behandlingId, nyeSøknadAvklaringer);
        }
    }

    private static Boolean finnSøknadverdi(Map<LocalDate, Boolean> søknadErBosattPerFom, LocalDate fom) {
        // Kan vi forvente eksakte datoer her?
        return søknadErBosattPerFom.get(fom);
    }

}
