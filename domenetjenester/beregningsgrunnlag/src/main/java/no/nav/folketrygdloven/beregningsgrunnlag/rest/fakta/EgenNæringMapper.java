package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.EgenNæringDto;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;

public final class EgenNæringMapper {

    private EgenNæringMapper() {
        // Skjuler default
    }

    public static EgenNæringDto map(OppgittEgenNæring egenNæring) {
        EgenNæringDto dto = new EgenNæringDto();
        dto.setOrgnr(egenNæring.getOrgnr());
        dto.setUtenlandskvirksomhetsnavn(egenNæring.getVirksomhet() == null ? null : egenNæring.getVirksomhet().getNavn());
        dto.setVirksomhetType(egenNæring.getVirksomhetType());
        dto.setBegrunnelse(egenNæring.getBegrunnelse());
        dto.setEndringsdato(egenNæring.getEndringDato());
        dto.setErVarigEndret(egenNæring.getVarigEndring());
        dto.setErNyoppstartet(egenNæring.getNyoppstartet());
        dto.setErNyIArbeidslivet(egenNæring.getNyIArbeidslivet());
        dto.setRegnskapsførerNavn(egenNæring.getRegnskapsførerNavn());
        dto.setRegnskapsførerTlf(egenNæring.getRegnskapsførerTlf());
        dto.setOppgittInntekt(egenNæring.getBruttoInntekt());
        return dto;
    }

}
