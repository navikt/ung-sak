package no.nav.ung.sak.behandlingslager.bosatt;

/**
 * Type avklaring for bostedsvilkåret. Avgjør hvilken oppgavetype som sendes til bruker
 * og for readonly-visning av tidligere forslag, siden tom konverteres til gjeldende vilkårsperiode ende ved lagring forslag.
 */
public enum Avklaringtype {
    AVSLAG,
    OPPHØR
}
