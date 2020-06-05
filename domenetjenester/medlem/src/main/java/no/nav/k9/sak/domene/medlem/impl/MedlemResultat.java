
package no.nav.k9.sak.domene.medlem.impl;

/**Mappes til riktig aksjonspunktDef
 * enten i en inngangsvilkårkontekts
 * eller i en revuderingskontekts
 */
public enum MedlemResultat {
    AVKLAR_OM_ER_BOSATT,
    AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE,
    AVKLAR_OPPHOLDSRETT,
    AVKLAR_LOVLIG_OPPHOLD,
    VENT_PÅ_FØDSEL,
}
