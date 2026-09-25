package no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.vedtak.VedtakAksjonspunktData;
import no.nav.ung.sak.kontrakt.vedtak.AksjonspunktGodkjenningDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.LokalkontorBeslutterVilkårAksjonspunktDto;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.beslutte.LokalkontorBeslutteVilkårAksjonspunkt;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.beslutte.LokalkontorBeslutteVilkårTjeneste;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@DtoTilServiceAdapter(dto = LokalkontorBeslutterVilkårAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class LokalkontorBeslutterVilkårAksjonspunktOppdaterer implements AksjonspunktOppdaterer<LokalkontorBeslutterVilkårAksjonspunktDto> {

    private LokalkontorBeslutteVilkårAksjonspunkt beslutteVilkårAksjonspunkt;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    LokalkontorBeslutterVilkårAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public LokalkontorBeslutterVilkårAksjonspunktOppdaterer(LokalkontorBeslutteVilkårAksjonspunkt beslutteVilkårAksjonspunkt,
                                                            TotrinnTjeneste totrinnTjeneste,
                                                            BehandlingRepository behandlingRepository,
                                                            HistorikkinnslagRepository historikkinnslagRepository) {
        this.beslutteVilkårAksjonspunkt = beslutteVilkårAksjonspunkt;
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(LokalkontorBeslutterVilkårAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        AksjonspunktDefinisjon fatteVedtakAksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        if (param.getRef().getFagsakYtelseType() != FagsakYtelseType.AKTIVITETSPENGER) {
            throw new IllegalArgumentException("LokalkontorBeslutterVilkårAksjonspunktOppdaterer støtter kun oppdatering for AKTIVITETSPENGER");
        }

        Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtoList = dto.getAksjonspunktGodkjenningDtos() != null ?
            dto.getAksjonspunktGodkjenningDtos() : Collections.emptyList();

        Set<VedtakAksjonspunktData> aksjonspunkter = aksjonspunktGodkjenningDtoList.stream()
            .map(a -> {
                // map til VedtakAksjonsonspunktData fra DTO
                AksjonspunktDefinisjon aksDef = AksjonspunktDefinisjon.fraKode(a.getAksjonspunktKode());
                return new VedtakAksjonspunktData(aksDef, a.isGodkjent(), a.getBegrunnelse(), fraDto(a.getArsaker()));
            })
            .collect(Collectors.toSet());

        validerAksjonspunktType(aksjonspunkter);

        Behandling behandling = param.getBehandling();
        beslutteVilkårAksjonspunkt.oppdater(behandling, fatteVedtakAksjonspunktDefinisjon, aksjonspunkter);

        boolean sendesTilbake = dto.getAksjonspunktGodkjenningDtos().stream().anyMatch(a -> !a.isGodkjent());
        if (!sendesTilbake) {
            validerAlleTotrinnsaksjonspunkterVurdert(param.getBehandlingId());
            lagHistorikkInnslagBesluttetVilkår(behandling);
        } else {
            lagHistorikkInnslagVurderPåNytt(behandling, totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling));
        }
        return OppdateringResultat.nyttResultat();
    }

    private void lagHistorikkInnslagBesluttetVilkår(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_BESLUTTER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.LOKALKONTOR_BESLUTTER_VILKÅR)
            .addLinje("Vilkår som behandles ved lokalkontor er besluttet")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }


    private void lagHistorikkInnslagVurderPåNytt(Behandling behandling, Collection<Totrinnsvurdering> medTotrinnskontroll) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_BESLUTTER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel("Sak retur")
            .medLinjer(lagTekstForHverTotrinnkontroll(medTotrinnskontroll))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private static List<HistorikkinnslagLinjeBuilder> lagTekstForHverTotrinnkontroll(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .sorted(Comparator.comparing(ttv -> ttv.getEndretTidspunkt() != null ? ttv.getEndretTidspunkt() : ttv.getOpprettetTidspunkt()))
            .map(LokalkontorBeslutterVilkårAksjonspunktOppdaterer::tilHistorikkinnslagTekst)
            .map(LokalkontorBeslutterVilkårAksjonspunktOppdaterer::leggTilLinjeskift)
            .flatMap(Collection::stream)
            .toList();
    }

    private static List<HistorikkinnslagLinjeBuilder> tilHistorikkinnslagTekst(Totrinnsvurdering ttv) {
        var aksjonspunktNavn = ttv.getAksjonspunktDefinisjon().getNavn();
        if (Boolean.TRUE.equals(ttv.isGodkjent())) {
            return List.of(new HistorikkinnslagLinjeBuilder().bold(aksjonspunktNavn).bold("er godkjent"));
        }
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        linjer.add(new HistorikkinnslagLinjeBuilder().bold(aksjonspunktNavn).bold("må vurderes på nytt"));
        if (ttv.getBegrunnelse() != null) {
            linjer.add(new HistorikkinnslagLinjeBuilder().tekst("Kommentar:").tekst(ttv.getBegrunnelse()));
        }
        return linjer;
    }

    private static List<HistorikkinnslagLinjeBuilder> leggTilLinjeskift(List<HistorikkinnslagLinjeBuilder> eksistrendeLinjer) {
        var linjer = new ArrayList<>(eksistrendeLinjer);
        linjer.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        return linjer;
    }


    private void validerAlleTotrinnsaksjonspunkterVurdert(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Set<AksjonspunktDefinisjon> totrinnAksjonspunkter = behandling.getBehandledeAksjonspunkter()
            .stream()
            .filter(Aksjonspunkt::isToTrinnsBehandling)
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .collect(Collectors.toSet());
        Collection<Totrinnsvurdering> totrinnsvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
        Set<AksjonspunktDefinisjon> godkjenteAksjonspunkter = totrinnsvurderinger.stream()
            .filter(tt -> tt.isAktiv() && tt.isGodkjent())
            .map(Totrinnsvurdering::getAksjonspunktDefinisjon)
            .collect(Collectors.toSet());

        Set<AksjonspunktDefinisjon> manglerGodkjenning = new HashSet<>();
        manglerGodkjenning.addAll(totrinnAksjonspunkter);
        manglerGodkjenning.removeAll(godkjenteAksjonspunkter);

        if (!manglerGodkjenning.isEmpty()) {
            throw new IllegalArgumentException("Mangler godkjenning for følgende aksjonspunkter (forventer at frontend sender alle samtidig): " + manglerGodkjenning);
        }


    }

    private static void validerAksjonspunktType(Set<VedtakAksjonspunktData> aksjonspunkter) {
        List<AksjonspunktDefinisjon> ikkeStøttedeAksjonspunkt = aksjonspunkter.stream()
            .map(VedtakAksjonspunktData::getAksjonspunktDefinisjon)
            .filter(ap -> ap.getBehandlingDel() != BehandlingDel.LOKAL)
            .toList();
        if (!ikkeStøttedeAksjonspunkt.isEmpty()) {
            throw new IllegalArgumentException("Kun lokalkontor-aksjonspunkt støttes i LokalkontorBeslutterVilkårAksjonspunktOppdaterer, men fikk med: " + ikkeStøttedeAksjonspunkt);
        }
    }

    private Collection<String> fraDto(Collection<VurderÅrsak> arsaker) {
        if (arsaker == null) {
            return Collections.emptySet();
        }
        return arsaker.stream().map(Kodeverdi::getKode).collect(Collectors.toSet());
    }
}
