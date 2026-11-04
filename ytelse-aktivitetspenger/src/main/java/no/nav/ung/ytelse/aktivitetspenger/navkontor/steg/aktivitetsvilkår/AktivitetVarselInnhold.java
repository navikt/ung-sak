package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.AktivitetsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AktivitetsvilkåretIkkeOppfyltÅrsak;
import no.nav.ung.sak.etterlysning.VilkårsvarselInnhold;
import no.nav.ung.sak.typer.Periode;

// OBS: Likheten på denne recorden avgjør om en avklaring må varsles på nytt — kun felt som påvirker varselet
// til bruker hører hjemme her. skalSendeVarsel er med, siden en endring av det må oppdatere etterlysningene.
public record AktivitetVarselInnhold(
    Periode periode,
    AktivitetsvilkåretIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    boolean skalSendeVarsel,
    String fritekstTilVarsel,
    AktivitetsavklaringKildeType kilde,
    String kildeFritekst,
    Avklaringtype avklaringtype
) implements VilkårsvarselInnhold {
}
