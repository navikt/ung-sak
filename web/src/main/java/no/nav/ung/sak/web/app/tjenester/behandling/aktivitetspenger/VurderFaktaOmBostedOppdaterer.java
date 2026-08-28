package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt.BostedAvklaringInnhold;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt.BostedsAvklaringDataMapper;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderFaktaOmBostedDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt.BostedAvklaringTjeneste;

import java.time.LocalDateTime;
import java.util.*;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBostedDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBostedOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBostedDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private BostedAvklaringTjeneste bostedAvklaringTjeneste;


    VurderFaktaOmBostedOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBostedOppdaterer(BehandlingRepository behandlingRepository,
                                         HistorikkinnslagRepository historikkinnslagRepository,
                                         @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                         @BehandlingÅrsakTypeRef(BehandlingÅrsakType.ENDRET_BOSTED) BostedAvklaringTjeneste bostedAvklaringTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.bostedAvklaringTjeneste = bostedAvklaringTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBostedDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType()).utled(behandlingId, VilkårType.BOSTEDSVILKÅR);
        var maxTomDato = perioderTilVurdering.stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Må ha perioder til vurdering"));

        List<BostedsPeriodeAvklaring> tidligereForeslåtteAvklaringer = bostedAvklaringTjeneste.hentForeslåtteAvklaringer(behandlingId);

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        List<BostedAvklaringInnhold> nyeAvklaringer = dto.getAvklaringer().stream().filter(a -> a.vurdering() != null)
            .map(a -> BostedsAvklaringDataMapper.mapTilBostedAvklaringInnhold(a, maxTomDato)).toList();

        Set<BostedsPeriodeAvklaring> nyeForeslåtteAvklaringer = bostedAvklaringTjeneste.lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(nyeAvklaringer, vurdertAv, vurdertTidspunkt, behandlingId);

        bostedAvklaringTjeneste.gjenopprettTidligereVilkårsvurderingVedBehovOgSettAvklartPeriodeTilIkkeVurdert(param, tidligereForeslåtteAvklaringer, nyeAvklaringer);

        bostedAvklaringTjeneste.oppdaterEtterlysninger(behandling, tidligereForeslåtteAvklaringer, nyeForeslåtteAvklaringer);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Bostedsavklaring registrert")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }
}
