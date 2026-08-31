package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.typer.Periode;

/**
 * Bistandsavklaring (faktagrunnlag) for en periode.
 */
public record BistandAvklaringDto(
    Periode foreslåttPeriode,
    /** Årsak til at bistandsvilkåret ikke er oppfylt i perioden. */
    BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    /** Begrunnelse for relevante fakta som er lagt til grunn i avklaringen og varsel */
    String begrunnelse,
    /** Om det skal sendes varsel til bruker for denne perioden. */
    boolean skalSendeVarsel,
    /** Fritekst som brukes i varselet til bruker. */
    String fritekstTilVarsel,
    /** Begrunnelse for hvorfor det ikke skal sendes varsel til bruker. */
    String begrunnelseIkkeVarsel,
    /** Opphør eller avslag som ble foreslått i aksjonspunktet.
     * Utledes i aksjonspunktet basert på perioden, før perioden lukkes med vilkårsperiodens ende. */
    Avklaringtype avklaringtype,
    /** Kun avklaringer opprettet i denne behandlingen kan redigeres. */
    boolean kanRedigeres
) {
}
