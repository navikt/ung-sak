package no.nav.k9.sak.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.kontrakt.arbeidsforhold.RelaterteYtelserDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.TilgrensendeYtelserDto;
import no.nav.vedtak.konfig.Tid;

public class BehandlingRelaterteYtelserMapper {

    public static final List<FagsakYtelseType> RELATERT_YTELSE_TYPER_FOR_SØKER = List.of(
        FagsakYtelseType.FORELDREPENGER,
        FagsakYtelseType.ENGANGSTØNAD,
        FagsakYtelseType.SYKEPENGER,
        FagsakYtelseType.ENSLIG_FORSØRGER,
        FagsakYtelseType.DAGPENGER,
        FagsakYtelseType.ARBEIDSAVKLARINGSPENGER,
        FagsakYtelseType.SVANGERSKAPSPENGER,
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
        FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE,
        FagsakYtelseType.OPPLÆRINGSPENGER,
        FagsakYtelseType.OMSORGSPENGER,
        FagsakYtelseType.FRISINN);

    private BehandlingRelaterteYtelserMapper() {
    }

    public static List<TilgrensendeYtelserDto> mapFraBehandlingRelaterteYtelser(Collection<Ytelse> ytelser) {
        return ytelser.stream()
            .map(ytelse -> lagTilgrensendeYtelse(ytelse))
            .collect(Collectors.toList());
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
        tilgrensendeYtelserDto.setRelatertYtelseType(Objects.requireNonNull(fagsak.getYtelseType(), "ytelseType").getKode());
        tilgrensendeYtelserDto.setStatus(Objects.requireNonNull(fagsak.getStatus(), "status").getKode());
        tilgrensendeYtelserDto.setPeriodeFraDato(periodeDato);
        tilgrensendeYtelserDto.setPeriodeTilDato(endreTomDatoHvisLøpende(periodeDato));
        tilgrensendeYtelserDto.setSaksNummer(fagsak.getSaksnummer());
        return tilgrensendeYtelserDto;
    }

    private static LocalDate endreTomDatoHvisLøpende(LocalDate tomDato) {
        return Tid.TIDENES_ENDE.equals(tomDato) ? null : tomDato;
    }

    public static List<RelaterteYtelserDto> samleYtelserBasertPåYtelseType(List<TilgrensendeYtelserDto> tilgrensendeYtelser, List<FagsakYtelseType> ytelsesTyper) {
        List<RelaterteYtelserDto> dtos = new LinkedList<>();
        for (FagsakYtelseType relatertYtelseType : ytelsesTyper) {
            dtos.add(new RelaterteYtelserDto(relatertYtelseType.getKode(), sortTilgrensendeYtelser(tilgrensendeYtelser, relatertYtelseType.getKode())));
        }
        return dtos;
    }

    private static List<TilgrensendeYtelserDto> sortTilgrensendeYtelser(List<TilgrensendeYtelserDto> relatertYtelser, String relatertYtelseType) {
        return relatertYtelser.stream().filter(tilgrensendeYtelserDto -> (relatertYtelseType.equals(tilgrensendeYtelserDto.getRelatertYtelseType())))
            .sorted()
            .collect(Collectors.toList());
    }
}
