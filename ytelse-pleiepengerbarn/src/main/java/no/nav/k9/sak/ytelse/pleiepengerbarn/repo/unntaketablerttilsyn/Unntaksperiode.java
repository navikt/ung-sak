package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.k9.sak.kontrakt.sykdom.Resultat;

import java.time.LocalDate;

public record Unntaksperiode(LocalDate fom, LocalDate tom, String begrunnelse, Resultat resultat) {
}
