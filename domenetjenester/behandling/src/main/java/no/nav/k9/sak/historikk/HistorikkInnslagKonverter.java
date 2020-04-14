package no.nav.k9.sak.historikk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkOpplysningType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;
import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.kontrakt.historikk.HistorikkInnslagDokumentLinkDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkInnslagTemaDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagDelDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagEndretFeltDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagHendelseDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagOpplysningDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagSoeknadsperiodeDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagTotrinnsVurderingDto;
import no.nav.k9.sak.typer.JournalpostId;

@Dependent
public class HistorikkInnslagKonverter {

    @Inject
    public HistorikkInnslagKonverter() {
    }

    public HistorikkinnslagDto mapFra(Historikkinnslag historikkinnslag, List<ArkivJournalPost> journalPosterForSak) {
        HistorikkinnslagDto dto = new HistorikkinnslagDto();
        dto.setBehandlingId(historikkinnslag.getBehandlingId());
        List<HistorikkinnslagDelDto> historikkinnslagDeler = mapFra(historikkinnslag.getHistorikkinnslagDeler());
        dto.setHistorikkinnslagDeler(historikkinnslagDeler);
        List<HistorikkInnslagDokumentLinkDto> dokumentLinks = mapLenker(historikkinnslag.getDokumentLinker(), journalPosterForSak);
        dto.setDokumentLinks(dokumentLinks);
        if (historikkinnslag.getOpprettetAv() != null) {
            dto.setOpprettetAv(medStorBokstav(historikkinnslag.getOpprettetAv()));
        }
        dto.setOpprettetTidspunkt(historikkinnslag.getOpprettetTidspunkt());
        dto.setType(historikkinnslag.getType());
        dto.setAktoer(historikkinnslag.getAktør());
        return dto;
    }

    private List<HistorikkInnslagDokumentLinkDto> mapLenker(List<HistorikkinnslagDokumentLink> lenker, List<ArkivJournalPost> journalPosterForSak) {
        return lenker.stream().map(lenke -> map(lenke, journalPosterForSak)).collect(Collectors.toList());
    }

    private HistorikkInnslagDokumentLinkDto map(HistorikkinnslagDokumentLink lenke, List<ArkivJournalPost> journalPosterForSak) {
        Optional<ArkivJournalPost> aktivJournalPost = aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak);
        HistorikkInnslagDokumentLinkDto dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        dto.setUtgått(!aktivJournalPost.isPresent());
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        return dto;
    }

    private Optional<ArkivJournalPost> aktivJournalPost(JournalpostId journalpostId, List<ArkivJournalPost> journalPosterForSak) {
        return journalPosterForSak.stream().filter(ajp -> Objects.equals(ajp.getJournalpostId(), journalpostId)).findFirst();
    }

    private String medStorBokstav(String opprettetAv) {
        return opprettetAv.substring(0, 1).toUpperCase() + opprettetAv.substring(1);
    }
    
    static List<HistorikkinnslagDelDto> mapFra(List<HistorikkinnslagDel> historikkinnslagDelList) {
        List<HistorikkinnslagDelDto> historikkinnslagDelDtoList = new ArrayList<>();
        for (var historikkinnslagDel : historikkinnslagDelList) {
            historikkinnslagDelDtoList.add(mapFra(historikkinnslagDel));
        }
        return historikkinnslagDelDtoList;
    }

    private static HistorikkinnslagDelDto mapFra(HistorikkinnslagDel del) {
        var dto = new HistorikkinnslagDelDto();
        del.getBegrunnelseFelt().ifPresent(begrunnelse -> dto.setBegrunnelse(finnÅrsakKodeListe(begrunnelse).orElse(null)));
        if (dto.getBegrunnelse() == null) {
            del.getBegrunnelse().ifPresent(dto::setBegrunnelseFritekst);
        }
        del.getAarsakFelt().ifPresent(aarsak -> dto.setAarsak(finnÅrsakKodeListe(aarsak).orElse(null)));
        del.getTema().ifPresent(felt -> dto.setTema(mapFra(felt)));
        del.getGjeldendeFraFelt().ifPresent(felt -> {
            if (felt.getNavn() != null && felt.getNavnVerdi() != null && felt.getTilVerdi() != null) {
                dto.setGjeldendeFra(felt.getTilVerdi(), felt.getNavn(), felt.getNavnVerdi());
            } else if (felt.getTilVerdi() != null) {
                dto.setGjeldendeFra(felt.getTilVerdi());
            }
        });
        del.getResultat().ifPresent(dto::setResultat);
        del.getHendelse().ifPresent(hendelse -> {
            dto.setHendelse(mapFraHendelse(hendelse));
        });
        del.getSkjermlenke().ifPresent(skjermlenke -> {
            dto.setSkjermlenke(SkjermlenkeType.fraKode(skjermlenke));
        });
        if (!del.getTotrinnsvurderinger().isEmpty()) {
            dto.setAksjonspunkter(mapFraTotrinn(del.getTotrinnsvurderinger()));
        }
        if (!del.getOpplysninger().isEmpty()) {
            dto.setOpplysninger(mapFraOpplysning(del.getOpplysninger()));
        }
        if (!del.getEndredeFelt().isEmpty()) {
            dto.setEndredeFelter(mapFraEndretFelt(del.getEndredeFelt()));
        }
        del.getAvklartSoeknadsperiode().ifPresent(soeknadsperiode -> {
            dto.setSoeknadsperiode(mapFraSøknadsperiode(soeknadsperiode));
        });
        return dto;
    }

    private static Optional<Kodeverdi> finnÅrsakKodeListe(HistorikkinnslagFelt aarsak) {

        String aarsakVerdi = aarsak.getTilVerdi();
        if (Objects.equals("-", aarsakVerdi)) {
            return Optional.empty();
        }
        if (aarsak.getKlTilVerdi() == null) {
            return Optional.empty();
        }

        var kodeverdiMap = HistorikkInnslagTekstBuilder.KODEVERK_KODEVERDI_MAP.get(aarsak.getKlTilVerdi());
        if (kodeverdiMap == null) {
            throw new IllegalStateException("Har ikke støtte for HistorikkinnslagFelt#klTilVerdi=" + aarsak.getKlTilVerdi());
        }
        return Optional.ofNullable(kodeverdiMap.get(aarsakVerdi));
    }
    
    static HistorikkInnslagTemaDto mapFra(HistorikkinnslagFelt felt) {
        HistorikkInnslagTemaDto dto = new HistorikkInnslagTemaDto();
        HistorikkEndretFeltType endretFeltNavn = HistorikkEndretFeltType.fraKode(felt.getNavn());
        dto.setEndretFeltNavn(endretFeltNavn);
        dto.setNavnVerdi(felt.getNavnVerdi());
        dto.setKlNavn(felt.getKlNavn());
        return dto;
    }
    

    public static HistorikkinnslagHendelseDto mapFraHendelse(HistorikkinnslagFelt hendelse) {
        return new HistorikkinnslagHendelseDto(HistorikkinnslagType.fraKode(hendelse.getNavn()), hendelse.getTilVerdi());
    }
    
    static List<HistorikkinnslagTotrinnsVurderingDto> mapFraTotrinn(List<HistorikkinnslagTotrinnsvurdering> aksjonspunkter) {
        return aksjonspunkter.stream()
            .map(HistorikkInnslagKonverter::mapFraTotrinn)
            .collect(Collectors.toList());
    }

    private static HistorikkinnslagTotrinnsVurderingDto mapFraTotrinn(HistorikkinnslagTotrinnsvurdering totrinnsvurdering) {
        HistorikkinnslagTotrinnsVurderingDto dto = new HistorikkinnslagTotrinnsVurderingDto();
        dto.setAksjonspunktKode(totrinnsvurdering.getAksjonspunktDefinisjon().getKode());
        dto.setBegrunnelse(totrinnsvurdering.getBegrunnelse());
        dto.setGodkjent(totrinnsvurdering.erGodkjent());
        return dto;
    }
    
    static List<HistorikkinnslagOpplysningDto> mapFraOpplysning(List<HistorikkinnslagFelt> opplysninger) {
        return opplysninger.stream().map(o -> mapFraOpplysning(o)).collect(Collectors.toList());
    }

    private static HistorikkinnslagOpplysningDto mapFraOpplysning(HistorikkinnslagFelt opplysning) {
        HistorikkinnslagOpplysningDto dto = new HistorikkinnslagOpplysningDto();
        HistorikkOpplysningType opplysningType = HistorikkOpplysningType.fraKode(opplysning.getNavn());
        dto.setOpplysningType(opplysningType);
        dto.setTilVerdi(opplysning.getTilVerdi());
        return dto;
    }

    
    static List<HistorikkinnslagEndretFeltDto> mapFraEndretFelt(List<HistorikkinnslagFelt> endretFeltList) {
        List<HistorikkinnslagEndretFeltDto> dto = new ArrayList<>();
        for (var felt : endretFeltList) {
            dto.add(mapFraEndretFelt(felt));
        }
        return dto;
    }

    private static HistorikkinnslagEndretFeltDto mapFraEndretFelt(HistorikkinnslagFelt endretFelt) {
        HistorikkinnslagEndretFeltDto dto = new HistorikkinnslagEndretFeltDto();
        HistorikkEndretFeltType endretFeltNavn = HistorikkEndretFeltType.fraKode(endretFelt.getNavn());
        dto.setEndretFeltNavn(endretFeltNavn);
        dto.setNavnVerdi(endretFelt.getNavnVerdi());
        dto.setKlNavn(endretFelt.getKlNavn());
        dto.setFraVerdi(tilObject(endretFelt.getFraVerdi()));
        dto.setTilVerdi(tilObject(endretFelt.getTilVerdi()));
        dto.setKlFraVerdi(endretFelt.getKlFraVerdi());
        dto.setKlTilVerdi(endretFelt.getKlTilVerdi());
        return dto;
    }

    private static Object tilObject(String verdi) {
        if ("true".equals(verdi)) {
            return Boolean.TRUE;
        }
        if ("false".equals(verdi)) {
            return Boolean.FALSE;
        }
        return verdi;
    }
    

    static HistorikkinnslagSoeknadsperiodeDto mapFraSøknadsperiode(HistorikkinnslagFelt soeknadsperiode) {
        HistorikkinnslagSoeknadsperiodeDto dto = new HistorikkinnslagSoeknadsperiodeDto();
        HistorikkAvklartSoeknadsperiodeType soeknadsperiodeType = HistorikkAvklartSoeknadsperiodeType.fraKode(soeknadsperiode.getNavn());
        dto.setSoeknadsperiodeType(soeknadsperiodeType);
        dto.setNavnVerdi(soeknadsperiode.getNavnVerdi());
        dto.setTilVerdi(soeknadsperiode.getTilVerdi());
        return dto;
    }


}
