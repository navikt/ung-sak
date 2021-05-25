package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import java.time.LocalDate;

public record Unntaksperiode(LocalDate fom, LocalDate tom, String tilleggsinformasjon) {
}
