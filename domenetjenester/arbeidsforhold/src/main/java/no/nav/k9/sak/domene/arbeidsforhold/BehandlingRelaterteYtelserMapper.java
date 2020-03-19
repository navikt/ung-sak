package no.nav.k9.sak.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.kontrakt.arbeidsforhold.RelaterteYtelserDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.TilgrensendeYtelserDto;
import no.nav.vedtak.konfig.Tid;

public class BehandlingRelaterteYtelserMapper {

    private static final Map<FagsakYtelseType, FagsakYtelseType> YTELSE_TYPE_MAP = Map.of(
        FagsakYtelseType.ENGANGSTØNAD, FagsakYtelseType.ENGANGSTØNAD,
        FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.FORELDREPENGER,
        FagsakYtelseType.SVANGERSKAPSPENGER, FagsakYtelseType.SVANGERSKAPSPENGER
    );

    public static final List<FagsakYtelseType> RELATERT_YTELSE_TYPER_FOR_SØKER = List.of(
        FagsakYtelseType.FORELDREPENGER,
        FagsakYtelseType.ENGANGSTØNAD,
        FagsakYtelseType.SYKEPENGER,
        FagsakYtelseType.ENSLIG_FORSØRGER,
        FagsakYtelseType.DAGPENGER,
        FagsakYtelseType.ARBEIDSAVKLARINGSPENGER,
        FagsakYtelseType.SVANGERSKAPSPENGER);

    public static final List<FagsakYtelseType> RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER = List.of(
        FagsakYtelseType.FORELDREPENGER,
        FagsakYtelseType.ENGANGSTØNAD);

    private BehandlingRelaterteYtelserMapper() {
    }

    public static List<TilgrensendeYtelserDto> mapFraBehandlingRelaterteYtelser(Collection<Ytelse> ytelser) {
        return ytelser.stream()
            .map(ytelse -> lagTilgrensendeYtelse(ytelse))
            .collect(Collectors.toList());
    }

    public static FagsakYtelseType mapFraFagsakYtelseTypeTilRelatertYtelseType(FagsakYtelseType type) {
        return YTELSE_TYPE_MAP.getOrDefault(type, FagsakYtelseType.UDEFINERT);
    }

    private static TilgrensendeYtelserDto lagTilgrensendeYtelse(Ytelse ytelse) {
        TilgrensendeYtelserDto tilgrensendeYtelserDto = new TilgrensendeYtelserDto();
        tilgrensendeYtelserDto.setRelatertYtelseType(ytelse.getYtelseType().getKode());
        tilgrensendeYtelserDto.setPeriodeFraDato(ytelse.getPeriode().getFomDato());
        tilgrensendeYtelserDto.setPeriodeTilDato(endreTomDatoHvisLøpende(ytelse.getPeriode().getTomDato()));
        tilgrensendeYtelserDto.setStatus(ytelse.getStatus().getKode());
        tilgrensendeYtelserDto.setSaksNummer(ytelse.getSaksnummer());
        return tilgrensendeYtelserDto;
    }

    public static TilgrensendeYtelserDto mapFraFagsak(Fagsak fagsak, LocalDate periodeDato) {
        TilgrensendeYtelserDto tilgrensendeYtelserDto = new TilgrensendeYtelserDto();
        FagsakYtelseType relatertYtelseType = YTELSE_TYPE_MAP.getOrDefault(fagsak.getYtelseType(), FagsakYtelseType.UDEFINERT);
        tilgrensendeYtelserDto.setRelatertYtelseType(relatertYtelseType.getKode());
        tilgrensendeYtelserDto.setStatus(fagsak.getStatus().getKode());
        tilgrensendeYtelserDto.setPeriodeFraDato(periodeDato);
        tilgrensendeYtelserDto.setPeriodeTilDato(endreTomDatoHvisLøpende(periodeDato));
        tilgrensendeYtelserDto.setSaksNummer(fagsak.getSaksnummer());
        return tilgrensendeYtelserDto;
    }

    private static LocalDate endreTomDatoHvisLøpende(LocalDate tomDato) {
        if (Tid.TIDENES_ENDE.equals(tomDato)) {
            return null;
        }
        return tomDato;
    }

    public static List<RelaterteYtelserDto> samleYtelserBasertPåYtelseType(List<TilgrensendeYtelserDto> tilgrensendeYtelser, List<FagsakYtelseType> ytelsesTyper) {
        List<RelaterteYtelserDto> relaterteYtelserDtos = new LinkedList<>();
        for (FagsakYtelseType relatertYtelseType : ytelsesTyper) {
            relaterteYtelserDtos.add(new RelaterteYtelserDto(relatertYtelseType.getKode(), sortTilgrensendeYtelser(tilgrensendeYtelser, relatertYtelseType.getKode())));
        }
        return relaterteYtelserDtos;
    }

    private static List<TilgrensendeYtelserDto> sortTilgrensendeYtelser(List<TilgrensendeYtelserDto> relatertYtelser, String relatertYtelseType) {
        return relatertYtelser.stream().filter(tilgrensendeYtelserDto -> (relatertYtelseType.equals(tilgrensendeYtelserDto.getRelatertYtelseType())))
            .sorted()
            .collect(Collectors.toList());
    }
}
