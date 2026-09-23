package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.etterlysning.VilkårsvarselInnhold;
import no.nav.ung.sak.typer.Periode;

// OBS: Likheten på denne recorden avgjør om en avklaring må varsles på nytt — kun felt som påvirker varselet
// til bruker hører hjemme her. skalSendeVarsel er med, siden en endring av det må oppdatere etterlysningene.
public record AndreLivsoppholdsytelserVarselInnhold(
    Periode periode,
    AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    boolean skalSendeVarsel,
    String fritekstTilVarsel,
    AndreLivsoppholdsytelserAvklaringKildeType kilde,
    String kildeFritekst,
    Avklaringtype avklaringtype
) implements VilkårsvarselInnhold {
}
