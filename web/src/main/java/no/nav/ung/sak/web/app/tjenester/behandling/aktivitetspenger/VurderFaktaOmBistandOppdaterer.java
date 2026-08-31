package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VurderFaktaOmBistandDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår.BistandAvklaringDataMapper;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår.BistandAvklaringInnhold;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår.BistandAvklaringTjeneste;

import java.time.LocalDateTime;
import java.util.*;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBistandDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBistandOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBistandDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private BistandAvklaringTjeneste bistandAvklaringTjeneste;

    VurderFaktaOmBistandOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBistandOppdaterer(BehandlingRepository behandlingRepository,
                                          HistorikkinnslagRepository historikkinnslagRepository,
                                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                          @BehandlingÅrsakTypeRef(BehandlingÅrsakType.ENDRET_BISTANDSBEHOV) BistandAvklaringTjeneste bistandAvklaringTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.bistandAvklaringTjeneste = bistandAvklaringTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBistandDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste
            .finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType())
            .utled(behandlingId, VilkårType.BISTANDSVILKÅR);
        var maxTomDato = perioderTilVurdering.stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Må ha perioder til vurdering"));

        List<VilkårPeriodeAvklaring> tidligereForeslåtteAvklaringer = bistandAvklaringTjeneste.hentForeslåtteAvklaringer(behandlingId);

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        List<BistandAvklaringInnhold> nyeAvklaringer = dto.getAvklaringer().stream().filter(a -> a.vurdering() != null)
            .map(a -> BistandAvklaringDataMapper.mapTilBistandAvklaringInnhold(a, maxTomDato)).toList();

        if (nyeAvklaringer.size() > 1) {
            throw new IllegalArgumentException("Støtter kun lagring av én avklaring for bistandsvilkåret samtidig");
        }

        Set<VilkårPeriodeAvklaring> nyeForeslåtteAvklaringer = bistandAvklaringTjeneste.lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(nyeAvklaringer, vurdertAv, vurdertTidspunkt, behandlingId);

        bistandAvklaringTjeneste.gjenopprettTidligereVilkårsvurderingVedBehovOgSettAvklartPeriodeTilIkkeVurdert(param, tidligereForeslåtteAvklaringer, nyeAvklaringer);

        bistandAvklaringTjeneste.oppdaterEtterlysninger(behandling, tidligereForeslåtteAvklaringer, nyeForeslåtteAvklaringer);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.BISTANDSVILKÅR)
            .addLinje("Bistandsavklaring registrert")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }
}
