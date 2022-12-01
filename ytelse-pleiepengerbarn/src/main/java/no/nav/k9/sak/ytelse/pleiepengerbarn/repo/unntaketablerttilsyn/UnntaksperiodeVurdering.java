package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import java.time.LocalDate;

import no.nav.k9.kodeverk.sykdom.Resultat;

public record UnntaksperiodeVurdering(LocalDate fom, LocalDate tom, String begrunnelse, Resultat resultat) {
}
