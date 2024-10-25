package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class OppdaterForespørslerISakMapper {

    public static OppdaterForespørslerISakRequest mapTilRequest(Map<DatoIntervallEntitet, List<Arbeidsgiver>> forespørselMap, BehandlingReferanse ref) {
        AktørIdDto aktørIdDto = new AktørIdDto(ref.getAktørId().getId());
        SaksnummerDto saksnummerDto = new SaksnummerDto(ref.getSaksnummer().getVerdi());
        YtelseType ytelseType = finnYtelseType(ref.getFagsakYtelseType());

        Map<LocalDate, List<OrganisasjonsnummerDto>> forespørselRequestMap = new HashMap<>();
        forespørselMap.forEach((periode, arbeidsgivere) -> {
            forespørselRequestMap.put(periode.getFomDato(), arbeidsgivere.stream().map(a -> new OrganisasjonsnummerDto(a.getOrgnr())).collect(Collectors.toList()));
        });

        return new OppdaterForespørslerISakRequest(aktørIdDto, forespørselRequestMap, ytelseType, saksnummerDto);
    }

    private static YtelseType finnYtelseType(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case OMSORGSPENGER -> YtelseType.OMSORGSPENGER;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case PLEIEPENGER_SYKT_BARN -> YtelseType.PLEIEPENGER_SYKT_BARN;
            case OPPLÆRINGSPENGER -> YtelseType.OPPLÆRINGSPENGER;
            default -> throw new IllegalStateException("Ikke støttet ytelsetype: " + fagsakYtelseType);
        };
    }
}
