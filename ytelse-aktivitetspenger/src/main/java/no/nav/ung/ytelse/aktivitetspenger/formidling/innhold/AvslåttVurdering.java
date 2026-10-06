package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.typer.Periode;

public record AvslåttVurdering(VilkårsvurderingResultat vurdering, Periode periode) {
}
