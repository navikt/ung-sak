package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
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
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.AndreLivsoppholdsytelserFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.VurderFaktaOmAndreLivsoppholdsytelserDto;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.andrelivsoppholdsytelser.AndreLivsoppholdsytelserAvklaring;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.andrelivsoppholdsytelser.AndreLivsoppholdsytelserAvklaringDataMapper;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.andrelivsoppholdsytelser.AndreLivsoppholdsytelserAvklaringTjeneste;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmAndreLivsoppholdsytelserDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmAndreLivsoppholdsytelserOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmAndreLivsoppholdsytelserDto> {

    private static final VilkårType VILKÅR_TYPE = VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private VilkårsavklaringEtterlysningTjeneste vilkårsavklaringEtterlysningTjeneste;
    private AndreLivsoppholdsytelserAvklaringTjeneste andreLivsoppholdsytelserAvklaringTjeneste;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    VurderFaktaOmAndreLivsoppholdsytelserOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmAndreLivsoppholdsytelserOppdaterer(BehandlingRepository behandlingRepository,
                                                           HistorikkinnslagRepository historikkinnslagRepository,
                                                           VilkårsavklaringEtterlysningTjeneste vilkårsavklaringEtterlysningTjeneste,
                                                           @BehandlingÅrsakTypeRef(BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE) AndreLivsoppholdsytelserAvklaringTjeneste andreLivsoppholdsytelserAvklaringTjeneste,
                                                           InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vilkårsavklaringEtterlysningTjeneste = vilkårsavklaringEtterlysningTjeneste;
        this.andreLivsoppholdsytelserAvklaringTjeneste = andreLivsoppholdsytelserAvklaringTjeneste;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
    }

    private static List<DatoIntervallEntitet> tilPerioder(Collection<VilkårsvarselInnhold> avklaringer) {
        return avklaringer.stream().map(VilkårsvarselInnhold::hentPeriodeSomDatoIntervallEntitet).toList();
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmAndreLivsoppholdsytelserDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        andreLivsoppholdsytelserAvklaringTjeneste.validerAvklartePerioderOverlapperEksisterendeVilkårsperioder(behandlingId,
            dto.getAvklaringer().stream().map(AndreLivsoppholdsytelserFaktaavklaringPeriodeDto::periode).toList());

        // Opphør lukkes mot slutten av vilkårsperioden, siden avklaringen kan gjelde en vilkårlig periode i den.
        var vilkårsperioder = inngangsvilkårVurderingTjeneste.hentRelevanteVilkårsperioder(behandlingId, VILKÅR_TYPE);
        if (vilkårsperioder.isEmpty()) {
            throw new IllegalStateException("Må ha relevante vilkårsperioder");
        }
        var maxTomDato = vilkårsperioder.getMaxLocalDate();

        Map<VilkårsvarselInnhold, UUID> tidligereForeslåtteAvklaringer = andreLivsoppholdsytelserAvklaringTjeneste.hentForeslåtteAvklaringerSomInnhold(behandlingId);

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        Set<AndreLivsoppholdsytelserAvklaring> nyeAvklaringer = dto.getAvklaringer().stream().filter(a -> a.avklaring() != null)
            .map(a -> AndreLivsoppholdsytelserAvklaringDataMapper.mapTilAvklaring(a, maxTomDato, vurdertAv, vurdertTidspunkt))
            .collect(Collectors.toSet());

        if (nyeAvklaringer.size() > 1) {
            throw new IllegalArgumentException("Støtter kun lagring av én avklaring for vilkåret om andre livsoppholdsytelser samtidig");
        }

        Map<VilkårsvarselInnhold, UUID> nyeForeslåtteAvklaringer = andreLivsoppholdsytelserAvklaringTjeneste.lagreForeslåtteAvklaringer(behandlingId, nyeAvklaringer);

        inngangsvilkårVurderingTjeneste.nullstillOverlappendeVurderingOgGjenopprettTidligereVedBehov(param,
            VILKÅR_TYPE,
            tilPerioder(tidligereForeslåtteAvklaringer.keySet()),
            tilPerioder(nyeForeslåtteAvklaringer.keySet()));

        vilkårsavklaringEtterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_ANDRE_LIVSOPPHOLDSYTELSER, tidligereForeslåtteAvklaringer, nyeForeslåtteAvklaringer);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.NAV_KONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER)
            .addLinje("Avklaring av andre livsoppholdsytelser registrert")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }
}
