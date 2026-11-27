package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsvarselInnhold;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.VilkårsavklaringEtterlysningTjeneste;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.aktivitet.AktivitetFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.aktivitet.VurderFaktaOmAktivitetDto;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår.AktivitetAvklaring;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår.AktivitetAvklaringDataMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmAktivitetDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmAktivitetOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmAktivitetDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private VilkårsavklaringEtterlysningTjeneste vilkårsavklaringEtterlysningTjeneste;
    private VilkårsavklaringTjeneste vilkårsavklaringTjeneste;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    VurderFaktaOmAktivitetOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmAktivitetOppdaterer(BehandlingRepository behandlingRepository,
                                            HistorikkinnslagRepository historikkinnslagRepository,
                                            VilkårsavklaringEtterlysningTjeneste vilkårsavklaringEtterlysningTjeneste,
                                            VilkårsavklaringTjeneste vilkårsavklaringTjeneste,
                                            InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vilkårsavklaringEtterlysningTjeneste = vilkårsavklaringEtterlysningTjeneste;
        this.vilkårsavklaringTjeneste = vilkårsavklaringTjeneste;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
    }

    private static List<DatoIntervallEntitet> tilPerioder(Collection<VilkårsvarselInnhold> avklaringer) {
        return avklaringer.stream().map(VilkårsvarselInnhold::hentPeriodeSomDatoIntervallEntitet).toList();
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmAktivitetDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        var vilkårsperioder = inngangsvilkårVurderingTjeneste.getVilkårTimelineUtenAvkortet(behandlingId, VilkårType.AKTIVITETSVILKÅR);
        vilkårsavklaringTjeneste.validerAvklartePerioderOverlapperEksisterendeVilkårsperioder(behandlingId, VilkårType.AKTIVITETSVILKÅR, vilkårsperioder,
            dto.getAvklaringer().stream().map(AktivitetFaktaavklaringPeriodeDto::periode).toList());

        // Opphør lukkes mot slutten av vilkårsperiodene, siden avklaringen kan gjelde en vilkårlig periode i den.
        if (vilkårsperioder.isEmpty()) {
            throw new IllegalStateException("Må ha vilkårsperioder");
        }
        var maxTomDato = vilkårsperioder.getMaxLocalDate();

        Map<VilkårsvarselInnhold, UUID> tidligereForeslåtteAvklaringer = vilkårsavklaringTjeneste.hentForeslåtteAvklaringerSomInnhold(behandlingId, VilkårType.AKTIVITETSVILKÅR, AktivitetAvklaringDataMapper::mapTilAktivitetAvklaringInnhold);

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        Set<AktivitetAvklaring> nyeAvklaringer = dto.getAvklaringer().stream().filter(a -> a.avklaring() != null)
            .map(a -> AktivitetAvklaringDataMapper.mapTilAktivitetAvklaring(a, maxTomDato, vurdertAv, vurdertTidspunkt))
            .collect(Collectors.toSet());

        if (nyeAvklaringer.size() > 1) {
            throw new IllegalArgumentException("Støtter kun lagring av én avklaring for aktivitetsvilkåret samtidig");
        }

        Map<VilkårsvarselInnhold, UUID> nyeForeslåtteAvklaringer = vilkårsavklaringTjeneste.lagreForeslåtteAvklaringer(behandlingId, VilkårType.AKTIVITETSVILKÅR, nyeAvklaringer, AktivitetAvklaringDataMapper::mapTilAktivitetAvklaringInnhold);

        inngangsvilkårVurderingTjeneste.nullstillOverlappendeVurderingOgGjenopprettTidligereVedBehov(param,
            VilkårType.AKTIVITETSVILKÅR,
            tilPerioder(tidligereForeslåtteAvklaringer.keySet()),
            tilPerioder(nyeForeslåtteAvklaringer.keySet()));

        vilkårsavklaringEtterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_AKTIVITET, tidligereForeslåtteAvklaringer, nyeForeslåtteAvklaringer);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.NAV_KONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.AKTIVITETSVILKÅR)
            .addLinje("Aktivitetsavklaring registrert")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }
}
