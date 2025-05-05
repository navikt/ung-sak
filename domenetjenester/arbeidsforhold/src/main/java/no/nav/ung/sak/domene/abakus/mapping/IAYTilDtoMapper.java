package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;

public class IAYTilDtoMapper {

    public static OppgittOpptjeningDto mapTilDto(OppgittOpptjeningBuilder builder) {
        return new MapOppgittOpptjening().mapTilDto(builder.build());
    }
}
