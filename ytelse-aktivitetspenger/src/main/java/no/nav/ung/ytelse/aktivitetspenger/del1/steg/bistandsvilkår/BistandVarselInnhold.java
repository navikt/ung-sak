package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.etterlysning.VilkårsvarselInnhold;
import no.nav.ung.sak.typer.Periode;

// OBS: Likheten på denne recorden avgjør om en avklaring må varsles på nytt — kun felt som påvirker varselet
// til bruker hører hjemme her. skalSendeVarsel er med, siden en endring av det må oppdatere etterlysningene.
public record BistandVarselInnhold(
    Periode periode,
    BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    boolean skalSendeVarsel,
    String fritekstTilVarsel,
    BistandsavklaringKildeType kilde,
    String kildeFritekst,
    Avklaringtype avklaringtype
) implements VilkårsvarselInnhold {
}
