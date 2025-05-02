package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.ytelse.RapportertInntekt;

import java.util.Set;

public record BrukersAvklarteInntekter(Set<RapportertInntekt> inntekter, BrukersAvklarteInntekterKilde kilde) {
}
