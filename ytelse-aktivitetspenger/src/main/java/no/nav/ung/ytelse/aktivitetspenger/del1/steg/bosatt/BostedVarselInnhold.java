package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

// OBS: Likheten på denne recorden avgjør om en avklaring må varsles på nytt — kun felt som påvirker varselet
// til bruker hører hjemme her. skalSendeVarsel er med, siden en endring av det må oppdatere etterlysningene.
// Kilde er med fordi den vises for bruker i varselet.
public record BostedVarselInnhold(
    Periode periode,
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    boolean skalSendeVarsel,
    String fritekstTilVarsel,
    BostedsavklaringKildeType kilde,
    String kildeFritekst,
    Avklaringtype avklaringtype
) {

    public DatoIntervallEntitet hentPeriodeSomDatoIntervallEntitet() {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
