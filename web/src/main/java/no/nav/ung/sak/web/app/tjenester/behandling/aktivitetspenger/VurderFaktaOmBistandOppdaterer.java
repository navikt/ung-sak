package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
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
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.VilkårsavklaringEtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.VilkårsvarselInnhold;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandVurderingIkkeOppfyltDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VurderFaktaOmBistandDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår.BistandAvklaring;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår.BistandAvklaringDataMapper;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår.BistandAvklaringTjeneste;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBistandDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBistandOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBistandDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private VilkårsavklaringEtterlysningTjeneste vilkårsavklaringEtterlysningTjeneste;

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private BistandAvklaringTjeneste bistandAvklaringTjeneste;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    VurderFaktaOmBistandOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBistandOppdaterer(BehandlingRepository behandlingRepository,
                                          HistorikkinnslagRepository historikkinnslagRepository,
                                          VilkårsavklaringEtterlysningTjeneste vilkårsavklaringEtterlysningTjeneste,
                                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                          @BehandlingÅrsakTypeRef(BehandlingÅrsakType.ENDRET_BISTANDSBEHOV) BistandAvklaringTjeneste bistandAvklaringTjeneste,
                                          InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vilkårsavklaringEtterlysningTjeneste = vilkårsavklaringEtterlysningTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.bistandAvklaringTjeneste = bistandAvklaringTjeneste;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
    }

    private static List<DatoIntervallEntitet> tilPerioder(Collection<VilkårsvarselInnhold> avklaringer) {
        return avklaringer.stream().map(VilkårsvarselInnhold::hentPeriodeSomDatoIntervallEntitet).toList();
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBistandDto dto, AksjonspunktOppdaterParameter param) {
        dto.getAvklaringer().stream()
            .map(BistandFaktaavklaringPeriodeDto::vurdering)
            .forEach(VurderFaktaOmBistandOppdaterer::validerVurdering);

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste
            .finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType())
            .utled(behandlingId, VilkårType.BISTANDSVILKÅR);
        var maxTomDato = perioderTilVurdering.stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Må ha perioder til vurdering"));

        Map<VilkårsvarselInnhold, UUID> tidligereForeslåtteAvklaringer = bistandAvklaringTjeneste.hentForeslåtteAvklaringerSomInnhold(behandlingId);

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        Set<BistandAvklaring> nyeAvklaringer = dto.getAvklaringer().stream().filter(a -> a.vurdering() != null)
            .map(a -> BistandAvklaringDataMapper.mapTilBistandAvklaring(a, maxTomDato, vurdertAv, vurdertTidspunkt))
            .collect(Collectors.toSet());

        if (nyeAvklaringer.size() > 1) {
            throw new IllegalArgumentException("Støtter kun lagring av én avklaring for bistandsvilkåret samtidig");
        }

        Map<VilkårsvarselInnhold, UUID> nyeForeslåtteAvklaringer = bistandAvklaringTjeneste.lagreForeslåtteAvklaringer(behandlingId, nyeAvklaringer);

        inngangsvilkårVurderingTjeneste.gjenopprettTidligereVilkårsvurderingVedBehovOgSettAvklartPeriodeTilIkkeVurdert(param,
            VilkårType.BISTANDSVILKÅR,
            tilPerioder(tidligereForeslåtteAvklaringer.keySet()),
            tilPerioder(nyeForeslåtteAvklaringer.keySet()));

        vilkårsavklaringEtterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BISTAND, tidligereForeslåtteAvklaringer, nyeForeslåtteAvklaringer);

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

    private static void validerVurdering(BistandVurderingIkkeOppfyltDto vurdering) {
        if (vurdering == null) {
            return;
        }
        if (vurdering.ikkeOppfyltÅrsak() == BistandsvilkårIkkeOppfyltÅrsak.AVKORTET) {
            throw new IllegalArgumentException("Ikke-støttet årsak for bistandsavklaring: " + vurdering.ikkeOppfyltÅrsak());
        }
    }
}
