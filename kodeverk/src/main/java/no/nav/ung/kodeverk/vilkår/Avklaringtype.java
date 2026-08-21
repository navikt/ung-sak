package no.nav.ung.kodeverk.vilkår;

/**
 * Type avklaring for et vilkår. Avgjør hvilken oppgavetype som sendes til bruker
 * og for readonly-visning av tidligere forslag, siden tom konverteres til gjeldende vilkårsperiode ende ved lagring forslag.
 * <p>
 * Felles for alle vilkårsavklaringer (jf. {@code VilkårsavklaringOppdaterer}), ikke kun bostedsvilkåret.
 */
public enum Avklaringtype {
    AVSLAG,
    OPPHØR
}
