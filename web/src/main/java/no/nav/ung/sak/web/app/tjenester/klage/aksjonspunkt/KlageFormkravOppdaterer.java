package no.nav.ung.sak.web.app.tjenester.klage.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.klage.KlageAvvistÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.klage.KlageUtredningTjeneste;
import no.nav.ung.sak.klage.KlageVurderingTjeneste;
import no.nav.ung.sak.kontrakt.klage.KlageFormkravAksjonspunktDto;
import no.nav.ung.sak.kontrakt.klage.PåklagdBehandlingDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;


@ApplicationScoped
@DtoTilServiceAdapter(dto = KlageFormkravAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class KlageFormkravOppdaterer implements AksjonspunktOppdaterer<KlageFormkravAksjonspunktDto> {

    private KlageUtredningTjeneste klageUtredningTjeneste;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private KlageRepository klageRepository;
    private AksjonspunktRepository aksjonspunktRepository;
    private BehandlingRepository behandlingRepository;

    KlageFormkravOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlageFormkravOppdaterer(KlageUtredningTjeneste klageUtredningTjeneste, KlageVurderingTjeneste klageVurderingTjeneste,
                                   HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   KlageRepository klageRepository, AksjonspunktRepository aksjonspunktRepository) {
        this.klageRepository = klageRepository;
        this.behandlingRepository = behandlingRepository;
        this.klageUtredningTjeneste = klageUtredningTjeneste;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.aksjonspunktRepository = aksjonspunktRepository;
    }

    @Override
    public OppdateringResultat oppdater(KlageFormkravAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var aksjonspunktKode = dto.getKode();
        var klageBehandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var klageUtredning = klageRepository.hentKlageUtredning(klageBehandling.getId());

        var klageFormkrav = klageUtredning.getFormkrav();
        var apDefFormkrav = AksjonspunktDefinisjon.fraKode(aksjonspunktKode);
        var skjermlenkeType = getSkjermlenkeType(aksjonspunktKode);

        opprettHistorikkinnslag(klageBehandling, apDefFormkrav, dto, klageFormkrav, klageUtredning, skjermlenkeType);
        Optional<KlageAvvistÅrsak> optionalAvvistÅrsak = vurderOgLagreFormkrav(dto, klageBehandling);
        if (optionalAvvistÅrsak.isPresent()) {
            lagreKlageVurderingResultatMedAvvistKlage(klageBehandling, dto);
//            return OppdateringResultat.medFremoverHoppTotrinn(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_VEDTAK);
        }
        klageBehandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(apDefFormkrav))
            .findFirst()
            .ifPresent(ap -> aksjonspunktRepository.fjernToTrinnsBehandlingKreves(ap));

        return OppdateringResultat.builder().build();
    }

    private Optional<KlageAvvistÅrsak> vurderOgLagreFormkrav(KlageFormkravAksjonspunktDto dto, Behandling behandling) {
        KlageVurdertAv klageVurdertAv = getKlageVurdertAv(behandling, dto.getKode());
        var påklagdBehandlingInfo = Optional.ofNullable(dto.getPåklagdBehandlingInfo());
        boolean gjelderVedtakPåYtelse = påklagdBehandlingInfo.isPresent();

        klageUtredningTjeneste.oppdaterKlageMedPåklagetEksternBehandlingUuid(
            behandling.getId(),
            påklagdBehandlingInfo.map(PåklagdBehandlingDto::getPåklagBehandlingUuid).orElse(null),
            påklagdBehandlingInfo.map(p -> BehandlingType.fraKode(p.getPåklagBehandlingType())).orElse(null));

        KlageFormkravAdapter adapter = new KlageFormkravAdapter(
            dto.erKlagerPart(),
            dto.erFristOverholdt(),
            dto.erKonkret(),
            dto.erSignert(),
            gjelderVedtakPåYtelse,
            dto.getBegrunnelse(),
            behandling.getId(),
            klageVurdertAv);

        klageVurderingTjeneste.lagreFormkrav(behandling, adapter);
        return utledAvvistÅrsak(dto, gjelderVedtakPåYtelse);    // TODO: Bytt ut med metode laget domeneobjekt
    }

    private Optional<KlageAvvistÅrsak> utledAvvistÅrsak(KlageFormkravAksjonspunktDto dto, boolean gjelderVedtakPåYtelse) {
        if (!gjelderVedtakPåYtelse) {
            return Optional.of(KlageAvvistÅrsak.IKKE_PAKLAGD_VEDTAK);
        }
        if (!dto.erKlagerPart()) {
            return Optional.of(KlageAvvistÅrsak.KLAGER_IKKE_PART);
        }
        if (!dto.erFristOverholdt()) {
            return Optional.of(KlageAvvistÅrsak.KLAGET_FOR_SENT);
        }
        if (!dto.erKonkret()) {
            return Optional.of(KlageAvvistÅrsak.IKKE_KONKRET);
        }
        if (!dto.erSignert()) {
            return Optional.of(KlageAvvistÅrsak.IKKE_SIGNERT);
        }
        return Optional.empty();
    }

    private void lagreKlageVurderingResultatMedAvvistKlage(Behandling klageBehandling, KlageFormkravAksjonspunktDto dto) {
        boolean erFørsteinstansAksjonspunkt = VURDERING_AV_FORMKRAV_KLAGE_NFP.getKode().equals(dto.getKode());
        KlageVurdertAv klageVurdertAv = erFørsteinstansAksjonspunkt ? KlageVurdertAv.NAY : KlageVurdertAv.NK;

        final KlageVurderingAdapter vurderingDto = new KlageVurderingAdapter(
            KlageVurderingType.AVVIS_KLAGE, null, null,
            null, null, null, klageVurdertAv);

        klageVurderingTjeneste.lagreVurdering(klageBehandling, vurderingDto);
    }

    private void opprettHistorikkinnslag(Behandling klageBehandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                         KlageFormkravAksjonspunktDto formkravDto, Optional<KlageFormkravAdapter> klageFormkrav,
                                         KlageUtredningEntitet klageUtredning, SkjermlenkeType skjermlenkeType) {
//        HistorikkinnslagType historikkinnslagType = erFørsteinstansAksjonspunkt(klageBehandling, aksjonspunktDefinisjon) ? HistorikkinnslagType.KLAGE_BEH_NFP
//            : HistorikkinnslagType.KLAGE_BEH_NK;
//        HistorikkInnslagTekstBuilder historiebygger = historikkApplikasjonTjeneste.tekstBuilder();
//        historiebygger.medSkjermlenke(skjermlenkeType).medBegrunnelse(formkravDto.getBegrunnelse());
//        if (klageFormkrav.isEmpty()) {
//            settOppHistorikkFelter(historiebygger, formkravDto);
//        } else {
//            finnOgSettOppEndredeHistorikkFelter(klageFormkrav.get(), historiebygger, formkravDto, klageUtredning);
//        }
//        historikkApplikasjonTjeneste.opprettHistorikkInnslag(klageBehandling.getId(), historikkinnslagType);
    }

    private String hentPåKlagdEksternBehandlingTekst(KlageFormkravAksjonspunktDto formkravDto) {
        return Optional.ofNullable(formkravDto.getPåklagdBehandlingInfo())
            .map(p -> BehandlingType.fraKode(p.getPåklagBehandlingType()).getNavn() + " " +
                formatDato(p.getPåklagBehandlingVedtakDato()))
            .orElse("Ikke påklagd et vedtak");
    }

//    private void settOppHistorikkFelter(HistorikkInnslagTekstBuilder historiebygger, KlageFormkravAksjonspunktDto dto) {
//        var påKlagdBehandling = hentPåKlagdEksternBehandlingTekst(dto);
//        historiebygger
//            .medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID, null, påKlagdBehandling)
//            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGER_PART, null, dto.erKlagerPart())
//            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGEFRIST_OVERHOLDT, null, dto.erFristOverholdt())
//            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGEN_SIGNERT, null, dto.erSignert())
//            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGE_KONKRET, null, dto.erKonkret());
//
//    }
//
//    private void finnOgSettOppEndredeHistorikkFelter(KlageFormkravAdapter klageFormkrav, HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
//                                                     KlageFormkravAksjonspunktDto formkravDto, KlageUtredning klageUtredning) {
//        if (erVedtakOppdatert(klageUtredning, formkravDto)) {
//            var påKlagdBehandlingTekst = hentPåKlagdEksternBehandlingTekst(formkravDto);
//
//            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
//                null,
//                påKlagdBehandlingTekst);
//        }
//        if (klageFormkrav.isErKlagerPart() != formkravDto.erKlagerPart()) {
//            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGER_PART, klageFormkrav.isErKlagerPart(), formkravDto.erKlagerPart());
//        }
//        if (klageFormkrav.isErFristOverholdt() != formkravDto.erFristOverholdt()) {
//            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGEFRIST_OVERHOLDT, klageFormkrav.isErFristOverholdt(),
//                formkravDto.erFristOverholdt());
//        }
//        if (klageFormkrav.isErSignert() != formkravDto.erSignert()) {
//            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGEN_SIGNERT, klageFormkrav.isErSignert(), formkravDto.erSignert());
//        }
//        if (klageFormkrav.isErKonkret() != formkravDto.erKonkret()) {
//            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGE_KONKRET, klageFormkrav.isErKonkret(), formkravDto.erKonkret());
//        }
//    }


    private KlageVurdertAv getKlageVurdertAv(Behandling klageBehandling, String apKode) {
        return apKode.equals(VURDERING_AV_FORMKRAV_KLAGE_NFP.getKode()) ? KlageVurdertAv.NAY : KlageVurdertAv.NK;
    }

    private SkjermlenkeType getSkjermlenkeType(String apKode) {
        return  SkjermlenkeType.FORMKRAV_KLAGE_NFP;
//        return apKode.equals(VURDERING_AV_FORMKRAV_KLAGE_NFP.getKode()) ? SkjermlenkeType.FORMKRAV_KLAGE_NFP : SkjermlenkeType.FORMKRAV_KLAGE_KA;
    }

//    private boolean erVedtakOppdatert(KlageUtredning klageUtredning, KlageFormkravAksjonspunktDto formkravDto) {
//        Optional<UUID> gammelRef = klageUtredning.getPåKlagetBehandlingRef();
//        Optional<UUID> oppdatertRef = Optional.ofNullable(formkravDto.hentpåKlagdEksternBehandlingUuId());
//
//        return !gammelRef.equals(oppdatertRef);
//    }

    private boolean erFørsteinstansAksjonspunkt(Behandling klageBehandling, AksjonspunktDefinisjon apDef) {
        return Objects.equals(KlageVurdertAv.NAY, getKlageVurdertAv(klageBehandling, apDef.getKode()));
    }

    private String formatDato(LocalDate dato) {
        return dato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
