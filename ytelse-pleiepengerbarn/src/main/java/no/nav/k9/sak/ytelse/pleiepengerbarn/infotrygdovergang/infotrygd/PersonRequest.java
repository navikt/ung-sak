package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.time.LocalDate;
import java.util.List;


public record PersonRequest(LocalDate fom, LocalDate tom, List<String> fnr) {
}
